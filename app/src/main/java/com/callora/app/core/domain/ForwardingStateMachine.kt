package com.callora.app.core.domain

import com.callora.app.core.model.FailureReason
import com.callora.app.core.model.ForwardingState
import com.callora.app.core.model.VerificationProvenance

/**
 * Pure reducer for [ForwardingState]. All carrier-facing code funnels observed
 * events through here so state transitions stay consistent and testable.
 */
object ForwardingStateMachine {

    sealed interface Event {
        data object ActivationRequested : Event
        data object CommandSubmitted : Event
        data class Confirmed(val provenance: VerificationProvenance) : Event
        data object ConfirmedInactive : Event
        data class Failed(val reason: FailureReason) : Event
        data object ManualActionRequired : Event
        data object Unsupported : Event
        data object DeactivationRequested : Event
        data object Reset : Event
    }

    fun reduce(current: ForwardingState, event: Event): ForwardingState = when (event) {
        Event.ActivationRequested -> ForwardingState.PENDING_ACTIVATION
        Event.CommandSubmitted -> ForwardingState.ACTIVE_UNVERIFIED
        is Event.Confirmed -> when (event.provenance) {
            VerificationProvenance.USER_REPORTED -> ForwardingState.ACTIVE_UNVERIFIED
            VerificationProvenance.COMMAND_SUBMITTED_UNCONFIRMED -> ForwardingState.ACTIVE_UNVERIFIED
            VerificationProvenance.LOCAL_DESIRED_POLICY -> current
            else -> ForwardingState.ACTIVE_VERIFIED
        }
        Event.ConfirmedInactive -> ForwardingState.INACTIVE
        is Event.Failed -> ForwardingState.ERROR
        Event.ManualActionRequired -> ForwardingState.MANUAL_ACTION_REQUIRED
        Event.Unsupported -> ForwardingState.UNSUPPORTED
        Event.DeactivationRequested -> ForwardingState.PENDING_DEACTIVATION
        Event.Reset -> ForwardingState.UNKNOWN
    }
}
