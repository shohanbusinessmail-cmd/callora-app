package com.callora.app.core.domain

import com.callora.app.core.model.AutomationPolicy
import com.callora.app.core.model.ForwardingCondition
import com.callora.app.core.model.ForwardingState
import com.callora.app.core.model.ObservedForwardingStatus
import com.callora.app.core.model.ReconciliationDecision

/**
 * Deterministic, side-effect-free reconciliation between desired policy and
 * observed carrier state. The engine never issues carrier commands itself and
 * never auto-deactivates a rule while the true state is unknown.
 */
object ReconciliationEngine {

    data class DesiredRule(
        val subscriptionId: Int,
        val condition: ForwardingCondition,
        val effectiveEnabled: Boolean,
        val destinationE164: String?,
        val policy: AutomationPolicy,
    )

    data class Input(
        val subscriptionPresent: Boolean,
        val permissionGranted: Boolean,
        val backendAvailable: Boolean,
        val desiredRules: List<DesiredRule>,
        val observedByKey: Map<String, ObservedForwardingStatus>,
        val inFlightKeys: Set<String>,
    ) {
        fun observed(subscriptionId: Int, condition: ForwardingCondition): ObservedForwardingStatus? =
            observedByKey[key(subscriptionId, condition)]
    }

    fun key(subscriptionId: Int, condition: ForwardingCondition): String =
        "$subscriptionId:${condition.name}"

    fun reconcile(input: Input): List<ReconciliationDecision> {
        if (!input.subscriptionPresent) {
            val subIds = input.desiredRules.map { it.subscriptionId }.distinct()
            return subIds.map {
                ReconciliationDecision.SuspendAutomation(it, com.callora.app.core.model.FailureReason.SIM_UNAVAILABLE)
            }
        }

        return input.desiredRules.map { rule ->
            val key = key(rule.subscriptionId, rule.condition)
            when {
                key in input.inFlightKeys -> ReconciliationDecision.NoAction
                !input.permissionGranted -> ReconciliationDecision.RequireManual(
                    rule.subscriptionId, rule.condition,
                    com.callora.app.core.model.FailureReason.PERMISSION_DENIED,
                )
                !input.backendAvailable -> ReconciliationDecision.RequireManual(
                    rule.subscriptionId, rule.condition,
                    com.callora.app.core.model.FailureReason.UNSUPPORTED_API,
                )
                rule.policy == AutomationPolicy.MANUAL -> ReconciliationDecision.QueryOnly
                else -> reconcileRule(rule, input.observed(rule.subscriptionId, rule.condition))
            }
        }
    }

    private fun reconcileRule(
        rule: DesiredRule,
        observed: ObservedForwardingStatus?,
    ): ReconciliationDecision {
        val state = observed?.state ?: ForwardingState.UNKNOWN
        return if (rule.effectiveEnabled) {
            val destination = requireNotNull(rule.destinationE164)
            when (state) {
                ForwardingState.ACTIVE_VERIFIED ->
                    if (observed.destinationE164 != null && observed.destinationE164 != destination) {
                        ReconciliationDecision.Activate(
                            rule.subscriptionId, rule.condition, destination,
                        )
                    } else {
                        ReconciliationDecision.NoAction
                    }
                ForwardingState.ACTIVE_UNVERIFIED,
                ForwardingState.UNKNOWN,
                -> ReconciliationDecision.QueryOnly
                ForwardingState.INACTIVE ->
                    ReconciliationDecision.Activate(rule.subscriptionId, rule.condition, destination)
                ForwardingState.ERROR,
                ForwardingState.MANUAL_ACTION_REQUIRED,
                -> ReconciliationDecision.RequireManual(
                    rule.subscriptionId, rule.condition,
                    com.callora.app.core.model.FailureReason.VERIFICATION_FAILURE,
                )
                ForwardingState.UNSUPPORTED -> ReconciliationDecision.RequireManual(
                    rule.subscriptionId, rule.condition,
                    com.callora.app.core.model.FailureReason.UNSUPPORTED_CARRIER_SERVICE,
                )
                ForwardingState.PENDING_ACTIVATION,
                ForwardingState.PENDING_DEACTIVATION,
                -> ReconciliationDecision.NoAction
            }
        } else {
            // Desired OFF. Never erase while state is unknown/unverified.
            when (state) {
                ForwardingState.ACTIVE_VERIFIED ->
                    ReconciliationDecision.Deactivate(rule.subscriptionId, rule.condition)
                ForwardingState.INACTIVE -> ReconciliationDecision.NoAction
                else -> ReconciliationDecision.QueryOnly
            }
        }
    }
}
