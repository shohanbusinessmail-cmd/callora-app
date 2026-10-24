package com.callora.app.core.domain

import com.callora.app.core.model.AutomationPolicy
import com.callora.app.core.model.FailureReason
import com.callora.app.core.model.ForwardingCondition
import com.callora.app.core.model.ForwardingState
import com.callora.app.core.model.ObservedForwardingStatus
import com.callora.app.core.model.ReconciliationDecision
import com.callora.app.core.model.VerificationProvenance
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReconciliationEngineTest {

    private val condition = ForwardingCondition.CFNRC
    private val subId = 7
    private val destination = "+8801712345678"

    private fun desired(
        enabled: Boolean = true,
        policy: AutomationPolicy = AutomationPolicy.ALWAYS_PROTECT,
    ) = ReconciliationEngine.DesiredRule(subId, condition, enabled, destination, policy)

    private fun observed(state: ForwardingState, dest: String? = destination) =
        ObservedForwardingStatus(
            subscriptionId = subId,
            condition = condition,
            state = state,
            provenance = VerificationProvenance.USER_REPORTED,
            destinationE164 = dest,
            observedAtEpochMs = 1_000L,
        )

    private fun input(
        rules: List<ReconciliationEngine.DesiredRule> = listOf(desired()),
        observed: ObservedForwardingStatus? = null,
        subscriptionPresent: Boolean = true,
        permissionGranted: Boolean = true,
        backendAvailable: Boolean = true,
        inFlight: Set<String> = emptySet(),
    ) = ReconciliationEngine.Input(
        subscriptionPresent = subscriptionPresent,
        permissionGranted = permissionGranted,
        backendAvailable = backendAvailable,
        desiredRules = rules,
        observedByKey = observed?.let {
            mapOf(ReconciliationEngine.key(subId, condition) to it)
        }.orEmpty(),
        inFlightKeys = inFlight,
    )

    @Test
    fun missing_sim_suspends_automation_instead_of_touching_another_sim() {
        val decisions = ReconciliationEngine.reconcile(input(subscriptionPresent = false))
        assertThat(decisions).containsExactly(
            ReconciliationDecision.SuspendAutomation(subId, FailureReason.SIM_UNAVAILABLE),
        )
    }

    @Test
    fun missing_permission_requires_manual_action() {
        val decisions = ReconciliationEngine.reconcile(input(permissionGranted = false))
        assertThat(decisions.first()).isInstanceOf(ReconciliationDecision.RequireManual::class.java)
        assertThat((decisions.first() as ReconciliationDecision.RequireManual).reason)
            .isEqualTo(FailureReason.PERMISSION_DENIED)
    }

    @Test
    fun inactive_observed_state_triggers_activation() {
        val decisions = ReconciliationEngine.reconcile(input(observed = observed(ForwardingState.INACTIVE)))
        assertThat(decisions).containsExactly(
            ReconciliationDecision.Activate(subId, condition, destination),
        )
    }

    @Test
    fun unknown_state_never_auto_activates_or_deactivates_blindly() {
        val decisions = ReconciliationEngine.reconcile(input(observed = null))
        assertThat(decisions).containsExactly(ReconciliationDecision.QueryOnly)
    }

    @Test
    fun verified_active_with_matching_destination_needs_no_action() {
        val decisions = ReconciliationEngine.reconcile(
            input(observed = observed(ForwardingState.ACTIVE_VERIFIED)),
        )
        assertThat(decisions).containsExactly(ReconciliationDecision.NoAction)
    }

    @Test
    fun verified_active_with_wrong_destination_is_re_registered() {
        val decisions = ReconciliationEngine.reconcile(
            input(observed = observed(ForwardingState.ACTIVE_VERIFIED, dest = "+8801900000000")),
        )
        assertThat(decisions.first()).isInstanceOf(ReconciliationDecision.Activate::class.java)
    }

    @Test
    fun desired_off_does_not_erase_unknown_state_and_deactivates_only_when_verified_active() {
        val offRule = desired(enabled = false)

        val unknownDecision = ReconciliationEngine.reconcile(
            input(rules = listOf(offRule), observed = observed(ForwardingState.UNKNOWN)),
        )
        assertThat(unknownDecision).containsExactly(ReconciliationDecision.QueryOnly)

        val activeDecision = ReconciliationEngine.reconcile(
            input(rules = listOf(offRule), observed = observed(ForwardingState.ACTIVE_VERIFIED)),
        )
        assertThat(activeDecision).containsExactly(
            ReconciliationDecision.Deactivate(subId, condition),
        )
    }

    @Test
    fun manual_policy_only_queries() {
        val decisions = ReconciliationEngine.reconcile(
            input(rules = listOf(desired(policy = AutomationPolicy.MANUAL))),
        )
        assertThat(decisions).containsExactly(ReconciliationDecision.QueryOnly)
    }

    @Test
    fun in_flight_operation_blocks_duplicates() {
        val key = ReconciliationEngine.key(subId, condition)
        val decisions = ReconciliationEngine.reconcile(input(inFlight = setOf(key)))
        assertThat(decisions).containsExactly(ReconciliationDecision.NoAction)
    }

    @Test
    fun unsupported_backend_requires_manual_action() {
        val decisions = ReconciliationEngine.reconcile(input(backendAvailable = false))
        assertThat((decisions.first() as ReconciliationDecision.RequireManual).reason)
            .isEqualTo(FailureReason.UNSUPPORTED_API)
    }
}
