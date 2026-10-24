package com.callora.app.core.model

/**
 * One carrier-side operation with full lifecycle metadata. Persisted so that
 * process death and reboot cannot make an in-flight, potentially chargeable
 * operation invisible.
 */
data class ForwardingOperation(
    val operationId: String,
    val subscriptionId: Int,
    val condition: ForwardingCondition,
    val action: OperationAction,
    val destinationE164: String?,
    val backend: BackendType,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val status: OperationStatus,
    val attempts: Int,
    val maxAttempts: Int,
    val provenance: VerificationProvenance,
    val failureReason: FailureReason?,
    val retryEligible: Boolean,
    val detail: String?,
) {
    /** Same subscription + condition + action pair must never run concurrently. */
    fun conflictsWith(other: ForwardingOperation): Boolean =
        subscriptionId == other.subscriptionId &&
            condition == other.condition &&
            action == other.action &&
            status in IN_FLIGHT_STATUSES &&
            other.status in IN_FLIGHT_STATUSES

    val isInFlight: Boolean get() = status in IN_FLIGHT_STATUSES

    companion object {
        val IN_FLIGHT_STATUSES: Set<OperationStatus> = setOf(
            OperationStatus.PENDING,
            OperationStatus.SUBMITTED,
        )
    }
}

/** Outcome returned by a [com.callora.app.core.telephony.backend.CallForwardingBackend]. */
sealed interface BackendOutcome {
    data class Submitted(
        val provenance: VerificationProvenance,
        val detail: String?,
    ) : BackendOutcome

    data class Succeeded(
        val provenance: VerificationProvenance,
        val observedState: ForwardingState,
        val detail: String?,
    ) : BackendOutcome

    data class Failed(
        val reason: FailureReason,
        val detail: String?,
        val retryEligible: Boolean,
    ) : BackendOutcome

    data class Unavailable(
        val reason: FailureReason,
        val detail: String,
    ) : BackendOutcome
}
