package com.callora.app.core.model

/** Coarse, permission-safe SIM state used throughout Callora. */
enum class SimState {
    UNKNOWN,
    ABSENT,
    PIN_REQUIRED,
    PUK_REQUIRED,
    NETWORK_LOCKED,
    READY,
    DISABLED,
    NOT_READY
}

/**
 * Privacy-conscious subscription snapshot. Sensitive identifiers such as
 * ICCID/IMSI are deliberately excluded; the Android [subscriptionId] is an
 * app-local integer handle, not a stable hardware identifier.
 */
data class SubscriptionSnapshot(
    val subscriptionId: Int,
    val displayName: String,
    val carrierName: String,
    val operator: OperatorId,
    val mccMnc: String?,
    val slotIndex: Int,
    val simState: SimState,
    val isActive: Boolean,
    /** Whether Android exposed a phone number for this subscription. */
    val phoneNumberAvailable: Boolean,
) {
    val stableLabel: String
        get() = carrierName.ifBlank { displayName.ifBlank { "SIM ${slotIndex + 1}" } }
}

/** Observed carrier-side status for one condition on one subscription. */
data class ObservedForwardingStatus(
    val subscriptionId: Int,
    val condition: ForwardingCondition,
    val state: ForwardingState,
    val provenance: VerificationProvenance,
    val destinationE164: String?,
    val observedAtEpochMs: Long,
    val detail: String? = null,
) {
    companion object {
        fun unknown(subscriptionId: Int, condition: ForwardingCondition): ObservedForwardingStatus =
            ObservedForwardingStatus(
                subscriptionId = subscriptionId,
                condition = condition,
                state = ForwardingState.UNKNOWN,
                provenance = VerificationProvenance.NONE,
                destinationE164 = null,
                observedAtEpochMs = 0L,
            )
    }
}
