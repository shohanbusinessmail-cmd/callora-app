package com.callora.app.core.model

/** The mechanism used to attempt a carrier-side change. */
enum class BackendType {
    /** Official Android telephony API (restricted; normally carrier-privileged). */
    OFFICIAL_ANDROID_API,

    /** Standard GSM MMI string handed to the system dialer with explicit consent. */
    USER_CONFIRMED_MMI,

    /** Step-by-step manual configuration guide backed by evidence. */
    MANUAL_GUIDE,

    /** No safe mechanism is available on this device/SIM. */
    UNSUPPORTED
}

/** Evidence strength for a single operator capability claim. */
enum class CapabilityStatus {
    /** Backed by an official operator source. */
    SUPPORTED_OFFICIAL,

    /** Backed by credible secondary sources or the relevant 3GPP standard. */
    SUPPORTED_DOCUMENTED,

    /** Some behaviour is available but with material restrictions. */
    PARTIALLY_SUPPORTED,

    /** Plausible per the GSM standard but not confirmed for this operator. */
    NOT_VERIFIED,

    /** Evidence indicates the capability is unavailable. */
    UNSUPPORTED
}

/** Confidence classification attached to an [EvidenceRecord]. */
enum class EvidenceConfidence { HIGH, MEDIUM, LOW }

/** A single piece of research evidence for an operator capability. */
data class EvidenceRecord(
    val sourceTitle: String,
    val url: String,
    val dateChecked: String,
    val isOfficial: Boolean,
    val confidence: EvidenceConfidence,
    val notes: String,
)

/** Capability description for one forwarding condition on one operator. */
data class ConditionCapability(
    val condition: ForwardingCondition,
    val status: CapabilityStatus,
    val notes: String,
)

/**
 * Structured, evidence-backed capability record for one mobile operator.
 * Carrier policy lives here, separated from UI rendering and business logic.
 */
data class OperatorCapability(
    val operator: OperatorId,
    val displayName: String,
    val mccMnc: List<String>,
    val conditions: Map<ForwardingCondition, ConditionCapability>,
    val domesticDestinations: CapabilityStatus,
    val internationalDestinations: CapabilityStatus,
    val provisioningRequired: CapabilityStatus,
    val chargingSummary: String,
    val querySupported: CapabilityStatus,
    val remainsActiveWhilePoweredOff: CapabilityStatus,
    val limitations: List<String>,
    val evidence: List<EvidenceRecord>,
    val lastReviewed: String,
) {
    fun capabilityFor(condition: ForwardingCondition): ConditionCapability =
        conditions[condition]
            ?: ConditionCapability(condition, CapabilityStatus.NOT_VERIFIED, "")
}
