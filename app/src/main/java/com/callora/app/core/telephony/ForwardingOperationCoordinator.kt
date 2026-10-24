package com.callora.app.core.telephony

import com.callora.app.core.database.HistoryRepository
import com.callora.app.core.database.ObservedStatusRepository
import com.callora.app.core.database.OperationRepository
import com.callora.app.core.domain.RetryPolicy
import com.callora.app.core.model.BackendOutcome
import com.callora.app.core.model.FailureReason
import com.callora.app.core.model.ForwardingCondition
import com.callora.app.core.model.ForwardingOperation
import com.callora.app.core.model.ForwardingState
import com.callora.app.core.model.HistoryEventType
import com.callora.app.core.model.HistoryOutcome
import com.callora.app.core.model.ObservedForwardingStatus
import com.callora.app.core.model.OperationAction
import com.callora.app.core.model.OperationStatus
import com.callora.app.core.model.VerificationProvenance
import com.callora.app.core.telephony.backend.ManualGuideBackend
import com.callora.app.core.telephony.backend.MmiHandOff
import com.callora.app.core.telephony.backend.MmiIntentBackend
import com.callora.app.core.telephony.backend.OfficialApiBackend
import com.callora.app.core.telephony.backend.UnsupportedBackend
import java.util.UUID

/** Result of asking the coordinator to prepare an operation. */
sealed interface OperationRequestResult {
    data class HandoffReady(val operation: ForwardingOperation, val handoff: MmiHandOff) :
        OperationRequestResult

    data class ManualRequired(
        val operation: ForwardingOperation,
        val steps: List<String>,
        val queryMmi: String,
    ) : OperationRequestResult

    data class Rejected(val reason: FailureReason, val message: String) : OperationRequestResult
}

/**
 * Single entry point for carrier-side changes. Enforces idempotency, conflict
 * prevention, provenance rules and safe-retry policy. The coordinator never
 * invents a carrier response — an unconfirmed handoff stays unconfirmed.
 */
class ForwardingOperationCoordinator(
    private val operationRepository: OperationRepository,
    private val observedStatusRepository: ObservedStatusRepository,
    private val historyRepository: HistoryRepository,
    private val capabilityProvider: TelephonyCapabilityProvider,
    private val mmiIntentBackend: MmiIntentBackend,
    private val manualGuideBackend: ManualGuideBackend,
    private val officialApiBackend: OfficialApiBackend,
    private val unsupportedBackend: UnsupportedBackend,
    private val retryPolicy: RetryPolicy = RetryPolicy(),
    private val now: () -> Long = System::currentTimeMillis,
) {

    suspend fun prepareOperation(
        subscriptionId: Int,
        condition: ForwardingCondition,
        action: OperationAction,
        destinationE164: String?,
        operatorDisplayName: String,
        noReplyTimerSeconds: Int? = null,
    ): OperationRequestResult {
        // 1. Conflict prevention: never run two identical in-flight operations.
        if (operationRepository.hasInFlight(subscriptionId, condition, action)) {
            return OperationRequestResult.Rejected(
                FailureReason.INTERNAL_ERROR,
                "An identical operation is already in progress for this SIM and condition.",
            )
        }

        val capability = capabilityProvider.current()
        val operation = newOperation(subscriptionId, condition, action, destinationE164, capability)
        operationRepository.createIfAbsent(operation)

        // 2. Backend selection based on real device capability.
        if (officialApiBackend.isAvailable(capability)) {
            // Privileged build path; current build still reports unavailable honestly.
            val outcome = officialApiBackend.evaluate(capability)
            recordUnavailable(operation, outcome)
            return OperationRequestResult.Rejected(outcome.reason, outcome.detail)
        }

        return if (mmiIntentBackend.isAvailable(capability)) {
            val handoff = mmiIntentBackend.prepare(
                action = action,
                condition = condition,
                subscriptionId = subscriptionId,
                destinationE164 = destinationE164,
                noReplyTimerSeconds = noReplyTimerSeconds,
                capability = capability,
            )
            historyRepository.record(
                type = if (action == OperationAction.ACTIVATE) HistoryEventType.ACTIVATION_REQUESTED
                else HistoryEventType.DEACTIVATION_REQUESTED,
                outcome = HistoryOutcome.INFO,
                message = "Prepared ${action.name} for ${condition.name} via MMI handoff.",
                subscriptionId = subscriptionId,
                operationId = operation.operationId,
            )
            OperationRequestResult.HandoffReady(operation, handoff)
        } else {
            val guide = manualGuideBackend.buildGuide(condition, destinationE164, operatorDisplayName)
            markManual(operation)
            OperationRequestResult.ManualRequired(operation, guide.steps, guide.queryMmi)
        }
    }

    /** Called after the system dialer intent was actually launched. */
    suspend fun recordHandoffLaunched(operationId: String): ForwardingOperation? {
        val op = operationRepository.get(operationId) ?: return null
        val updated = op.copy(
            status = OperationStatus.SUBMITTED,
            attempts = op.attempts + 1,
            updatedAtEpochMs = now(),
            provenance = VerificationProvenance.COMMAND_SUBMITTED_UNCONFIRMED,
            detail = "MMI handed to system dialer; carrier result not observable by Callora.",
        )
        operationRepository.update(updated)
        val newState = when (op.action) {
            OperationAction.ACTIVATE -> ForwardingState.ACTIVE_UNVERIFIED
            OperationAction.DEACTIVATE -> ForwardingState.PENDING_DEACTIVATION
            OperationAction.QUERY -> ForwardingState.UNKNOWN
        }
        if (op.action != OperationAction.QUERY) {
            observedStatusRepository.save(
                ObservedForwardingStatus(
                    subscriptionId = op.subscriptionId,
                    condition = op.condition,
                    state = newState,
                    provenance = VerificationProvenance.COMMAND_SUBMITTED_UNCONFIRMED,
                    destinationE164 = op.destinationE164,
                    observedAtEpochMs = now(),
                    detail = updated.detail,
                ),
            )
        }
        return updated
    }

    /**
     * Records what the user saw in the carrier/dialer response. User-reported
     * evidence is stored as [VerificationProvenance.USER_REPORTED], never as
     * network-verified truth.
     */
    suspend fun recordUserResult(
        operationId: String,
        carrierAccepted: Boolean,
        detail: String?,
    ): ForwardingOperation? {
        val op = operationRepository.get(operationId) ?: return null
        val updated = if (carrierAccepted) {
            op.copy(
                status = OperationStatus.CONFIRMED,
                provenance = VerificationProvenance.USER_REPORTED,
                failureReason = null,
                retryEligible = false,
                detail = detail,
                updatedAtEpochMs = now(),
            )
        } else {
            op.copy(
                status = OperationStatus.REJECTED,
                provenance = VerificationProvenance.USER_REPORTED,
                failureReason = FailureReason.CARRIER_REJECTION,
                retryEligible = false, // never blindly retry a rejected, chargeable operation
                detail = detail,
                updatedAtEpochMs = now(),
            )
        }
        operationRepository.update(updated)

        if (carrierAccepted) {
            val state = when (op.action) {
                OperationAction.ACTIVATE -> ForwardingState.ACTIVE_UNVERIFIED
                OperationAction.DEACTIVATE -> ForwardingState.INACTIVE
                OperationAction.QUERY -> ForwardingState.UNKNOWN
            }
            observedStatusRepository.save(
                ObservedForwardingStatus(
                    subscriptionId = op.subscriptionId,
                    condition = op.condition,
                    state = state,
                    provenance = VerificationProvenance.USER_REPORTED,
                    destinationE164 = op.destinationE164,
                    observedAtEpochMs = now(),
                    detail = detail,
                ),
            )
            historyRepository.record(
                type = HistoryEventType.ACTIVATION_ACCEPTED,
                outcome = HistoryOutcome.SUCCESS,
                message = "User confirmed carrier acceptance for ${op.condition.name} (unverified by network query).",
                subscriptionId = op.subscriptionId,
                operationId = op.operationId,
            )
        } else {
            observedStatusRepository.save(
                ObservedForwardingStatus(
                    subscriptionId = op.subscriptionId,
                    condition = op.condition,
                    state = ForwardingState.ERROR,
                    provenance = VerificationProvenance.USER_REPORTED,
                    destinationE164 = op.destinationE164,
                    observedAtEpochMs = now(),
                    detail = detail,
                ),
            )
            historyRepository.record(
                type = HistoryEventType.STATUS_QUERY_FAILED,
                outcome = HistoryOutcome.ERROR,
                message = "Carrier rejected ${op.condition.name}: ${detail.orEmpty()}",
                subscriptionId = op.subscriptionId,
                operationId = op.operationId,
            )
        }
        return updated
    }

    suspend fun recordFailure(operationId: String, reason: FailureReason, detail: String?) {
        val op = operationRepository.get(operationId) ?: return
        val retryable = retryPolicy.isRetryable(reason)
        val exhausted = !retryable || op.attempts >= op.maxAttempts
        val updated = op.copy(
            status = if (exhausted) OperationStatus.EXHAUSTED else OperationStatus.FAILED,
            failureReason = reason,
            retryEligible = retryable && !exhausted,
            detail = detail,
            updatedAtEpochMs = now(),
        )
        operationRepository.update(updated)
        observedStatusRepository.save(
            ObservedForwardingStatus(
                subscriptionId = op.subscriptionId,
                condition = op.condition,
                state = ForwardingState.ERROR,
                provenance = op.provenance,
                destinationE164 = op.destinationE164,
                observedAtEpochMs = now(),
                detail = detail,
            ),
        )
        historyRepository.record(
            type = if (exhausted) HistoryEventType.RETRY_EXHAUSTED else HistoryEventType.RETRY_SCHEDULED,
            outcome = HistoryOutcome.WARNING,
            message = "Operation ${op.condition.name} failed: $reason",
            subscriptionId = op.subscriptionId,
            operationId = op.operationId,
        )
    }

    private suspend fun markManual(operation: ForwardingOperation) {
        val updated = operation.copy(
            status = OperationStatus.PENDING,
            updatedAtEpochMs = now(),
            detail = "Manual action required outside Callora.",
        )
        operationRepository.update(updated)
        observedStatusRepository.save(
            ObservedForwardingStatus(
                subscriptionId = operation.subscriptionId,
                condition = operation.condition,
                state = ForwardingState.MANUAL_ACTION_REQUIRED,
                provenance = VerificationProvenance.LOCAL_DESIRED_POLICY,
                destinationE164 = operation.destinationE164,
                observedAtEpochMs = now(),
            ),
        )
        historyRepository.record(
            type = HistoryEventType.MANUAL_ACTION_REQUIRED,
            outcome = HistoryOutcome.WARNING,
            message = "Manual configuration required for ${operation.condition.name}.",
            subscriptionId = operation.subscriptionId,
            operationId = operation.operationId,
        )
    }

    private suspend fun recordUnavailable(operation: ForwardingOperation, outcome: BackendOutcome.Unavailable) {
        val updated = operation.copy(
            status = OperationStatus.FAILED,
            failureReason = outcome.reason,
            retryEligible = false,
            detail = outcome.detail,
            updatedAtEpochMs = now(),
        )
        operationRepository.update(updated)
    }

    private fun newOperation(
        subscriptionId: Int,
        condition: ForwardingCondition,
        action: OperationAction,
        destinationE164: String?,
        capability: com.callora.app.core.telephony.DeviceTelephonyCapability,
    ): ForwardingOperation {
        val timestamp = now()
        return ForwardingOperation(
            operationId = UUID.randomUUID().toString(),
            subscriptionId = subscriptionId,
            condition = condition,
            action = action,
            destinationE164 = destinationE164,
            backend = capability.recommendedBackend,
            createdAtEpochMs = timestamp,
            updatedAtEpochMs = timestamp,
            status = OperationStatus.PENDING,
            attempts = 0,
            maxAttempts = retryPolicy.maxAttempts,
            provenance = VerificationProvenance.LOCAL_DESIRED_POLICY,
            failureReason = null,
            retryEligible = true,
            detail = null,
        )
    }

}
