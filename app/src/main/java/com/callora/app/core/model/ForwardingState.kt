package com.callora.app.core.model

/**
 * Typed representation of a forwarding rule's state. A single Boolean cannot
 * express the difference between "verified active", "configured but not
 * confirmed by the network" and "we genuinely do not know".
 */
enum class ForwardingState {
    /** No evidence has ever been collected for this rule. */
    UNKNOWN,

    /** Evidence indicates no forwarding rule is registered. */
    INACTIVE,

    /** An activation request is queued or in flight. */
    PENDING_ACTIVATION,

    /** The network (or an equivalent authoritative source) confirmed the rule. */
    ACTIVE_VERIFIED,

    /** A rule is locally desired/submitted but carrier confirmation is absent. */
    ACTIVE_UNVERIFIED,

    /** A deactivation request is queued or in flight. */
    PENDING_DEACTIVATION,

    /** The last operation failed; see [FailureReason]. */
    ERROR,

    /** No supported backend can manage this rule on the current device/SIM. */
    UNSUPPORTED,

    /** The user must complete an action outside Callora (e.g. system dialer). */
    MANUAL_ACTION_REQUIRED
}

/**
 * Where a piece of forwarding evidence came from. UI labels must reflect this
 * provenance honestly — a user-reported result is never "network verified".
 */
enum class VerificationProvenance {
    /** Authoritative Android API query that returned a definitive answer. */
    AUTHORITATIVE_API_QUERY,

    /** A documented carrier response that conclusively describes the rule. */
    DOCUMENTED_CARRIER_RESPONSE,

    /** A successful operation followed by an independent successful query. */
    OPERATION_THEN_QUERY,

    /** The user personally confirmed the outcome of a manual workflow. */
    USER_REPORTED,

    /** A command was handed to the system dialer with no observable result. */
    COMMAND_SUBMITTED_UNCONFIRMED,

    /** A locally stored desired policy — not evidence of carrier state. */
    LOCAL_DESIRED_POLICY,

    /** No provenance available. */
    NONE
}

/** Classified, user-actionable failure causes. */
enum class FailureReason {
    PERMISSION_DENIED,
    UNSUPPORTED_API,
    UNSUPPORTED_CARRIER_SERVICE,
    NETWORK_UNAVAILABLE,
    SIM_UNAVAILABLE,
    CARRIER_REJECTION,
    TIMEOUT,
    AMBIGUOUS_RESPONSE,
    VERIFICATION_FAILURE,
    INVALID_DESTINATION,
    RETRY_EXHAUSTED,
    INTERNAL_ERROR
}
