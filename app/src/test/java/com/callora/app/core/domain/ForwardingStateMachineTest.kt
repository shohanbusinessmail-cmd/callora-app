package com.callora.app.core.domain

import com.callora.app.core.model.FailureReason
import com.callora.app.core.model.ForwardingState
import com.callora.app.core.model.VerificationProvenance
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ForwardingStateMachineTest {

    @Test
    fun activation_request_moves_to_pending() {
        val next = ForwardingStateMachine.reduce(
            ForwardingState.INACTIVE,
            ForwardingStateMachine.Event.ActivationRequested,
        )
        assertThat(next).isEqualTo(ForwardingState.PENDING_ACTIVATION)
    }

    @Test
    fun submitted_command_is_never_verified() {
        val next = ForwardingStateMachine.reduce(
            ForwardingState.PENDING_ACTIVATION,
            ForwardingStateMachine.Event.CommandSubmitted,
        )
        assertThat(next).isEqualTo(ForwardingState.ACTIVE_UNVERIFIED)
    }

    @Test
    fun authoritative_query_marks_verified_but_user_report_does_not() {
        val verified = ForwardingStateMachine.reduce(
            ForwardingState.ACTIVE_UNVERIFIED,
            ForwardingStateMachine.Event.Confirmed(VerificationProvenance.AUTHORITATIVE_API_QUERY),
        )
        assertThat(verified).isEqualTo(ForwardingState.ACTIVE_VERIFIED)

        val reported = ForwardingStateMachine.reduce(
            ForwardingState.ACTIVE_UNVERIFIED,
            ForwardingStateMachine.Event.Confirmed(VerificationProvenance.USER_REPORTED),
        )
        assertThat(reported).isEqualTo(ForwardingState.ACTIVE_UNVERIFIED)
    }

    @Test
    fun failures_and_unsupported_map_to_distinct_states() {
        assertThat(
            ForwardingStateMachine.reduce(
                ForwardingState.PENDING_ACTIVATION,
                ForwardingStateMachine.Event.Failed(FailureReason.CARRIER_REJECTION),
            ),
        ).isEqualTo(ForwardingState.ERROR)

        assertThat(
            ForwardingStateMachine.reduce(
                ForwardingState.UNKNOWN,
                ForwardingStateMachine.Event.Unsupported,
            ),
        ).isEqualTo(ForwardingState.UNSUPPORTED)

        assertThat(
            ForwardingStateMachine.reduce(
                ForwardingState.ACTIVE_VERIFIED,
                ForwardingStateMachine.Event.ConfirmedInactive,
            ),
        ).isEqualTo(ForwardingState.INACTIVE)
    }
}
