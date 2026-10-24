package com.callora.app.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.callora.app.R
import com.callora.app.core.database.SubscriptionConfigEntity
import com.callora.app.core.di.AppContainer
import com.callora.app.core.domain.ReconciliationEngine
import com.callora.app.core.model.ForwardingCondition
import com.callora.app.core.model.HistoryEventType
import com.callora.app.core.model.HistoryOutcome
import com.callora.app.core.model.ReconciliationDecision
import kotlinx.coroutines.flow.first

/**
 * Bounded, idempotent reconciliation. Android blocks background activity starts,
 * so this worker never launches the dialer itself: it refreshes subscription
 * state, applies the deterministic [ReconciliationEngine], records history and
 * raises a notification whenever explicit user confirmation is required.
 */
class ReconciliationWorker(
    appContext: Context,
    params: WorkerParameters,
    private val container: AppContainer,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = runCatching {
        val now = System.currentTimeMillis()
        val capability = container.capabilityProvider.current()
        val visible = container.subscriptionScanner.scan()
        container.subscriptionConfigRepository
            .reconcileWithVisibleSubscriptions(visible, now)

        val configs = container.subscriptionConfigRepository.observeAll().first()
        val observed = container.observedStatusRepository.observeAll().first()
            .associateBy { ReconciliationEngine.key(it.subscriptionId, it.condition) }
        val inFlight = container.operationRepository.inFlightKeys()

        val desiredRules = configs
            .filter { !it.automationSuspended }
            .flatMap { row -> buildDesiredRules(row) }

        val decisions = ReconciliationEngine.reconcile(
            ReconciliationEngine.Input(
                subscriptionPresent = visible.isNotEmpty(),
                permissionGranted = capability.readPhoneStateGranted,
                backendAvailable = capability.mmiHandoffAvailable,
                desiredRules = desiredRules,
                observedByKey = observed,
                inFlightKeys = inFlight,
            ),
        )

        decisions.forEach { handleDecision(it) }
        container.settingsRepository.setLastReconciliation(now)
        Result.success()
    }.getOrElse {
        Result.retry()
    }

    private suspend fun buildDesiredRules(
        row: SubscriptionConfigEntity,
    ): List<ReconciliationEngine.DesiredRule> {
        val conditions = row.enabledConditionsCsv.split(",")
            .filter { it.isNotBlank() }
            .mapNotNull { runCatching { ForwardingCondition.valueOf(it) }.getOrNull() }
        if (conditions.isEmpty()) return emptyList()

        val destination = row.destinationId
            ?.let { container.destinationRepository.get(it)?.e164 }
        if (destination == null) {
            container.historyRepository.record(
                HistoryEventType.RECONCILIATION,
                HistoryOutcome.WARNING,
                applicationContext.getString(R.string.history_missing_destination),
                subscriptionId = row.subscriptionId,
            )
            return emptyList()
        }
        val profile = row.profileId?.let { container.profileRepository.get(it) }
        val policy = profile?.automationPolicy
            ?: com.callora.app.core.model.AutomationPolicy.ALWAYS_PROTECT
        return conditions.map { condition ->
            ReconciliationEngine.DesiredRule(
                subscriptionId = row.subscriptionId,
                condition = condition,
                effectiveEnabled = true,
                destinationE164 = destination,
                policy = policy,
            )
        }
    }

    private suspend fun handleDecision(decision: ReconciliationDecision) {
        when (decision) {
            is ReconciliationDecision.SuspendAutomation -> {
                container.subscriptionConfigRepository.suspendAutomation(
                    decision.subscriptionId,
                    decision.reason.name,
                )
                container.historyRepository.record(
                    HistoryEventType.SIM_CHANGED,
                    HistoryOutcome.WARNING,
                    applicationContext.getString(R.string.history_automation_suspended),
                    subscriptionId = decision.subscriptionId,
                )
            }
            is ReconciliationDecision.Activate,
            is ReconciliationDecision.Deactivate,
            is ReconciliationDecision.RequireManual,
            -> notifyManualActionRequired()
            ReconciliationDecision.NoAction,
            ReconciliationDecision.QueryOnly,
            -> Unit
        }
    }

    private suspend fun notifyManualActionRequired() {
        val settings = container.settingsRepository.settings.first()
        if (!settings.notificationsEnabled) return
        container.notificationCenter.notify(
            title = applicationContext.getString(R.string.notification_manual_title),
            text = applicationContext.getString(R.string.notification_manual_text),
        )
    }

    companion object {
        const val PERIODIC_NAME = "callora_reconciliation_periodic"
        const val BOOT_NAME = "callora_reconciliation_boot"
    }
}
