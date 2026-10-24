package com.callora.app.core.model

/**
 * The four GSM/3GPP call forwarding conditions, represented independently.
 *
 * @property gsmServiceCode the 3GPP TS 22.004 supplementary-service code used
 *   inside standard MMI strings (21/67/61/62). This is a standards identifier,
 *   not an operator-specific code.
 */
enum class ForwardingCondition(val gsmServiceCode: String) {
    /** Call Forwarding Unconditional — every eligible incoming call. */
    CFU("21"),

    /** Call Forwarding when Busy. */
    CFB("67"),

    /** Call Forwarding when No Reply. */
    CFNRY("61"),

    /** Call Forwarding when Not Reachable — the primary power-off protection. */
    CFNRC("62");

    companion object {
        /** Conditions offered by Callora, ordered for stable UI presentation. */
        val DISPLAY_ORDER: List<ForwardingCondition> =
            listOf(CFNRc, CFU, CFB, CFNRY)
    }
}

/** The lifecycle action requested against a carrier-side forwarding rule. */
enum class OperationAction {
    ACTIVATE,
    DEACTIVATE,
    QUERY
}

/** Persistent lifecycle state of a [ForwardingOperation]. */
enum class OperationStatus {
    PENDING,
    SUBMITTED,
    CONFIRMED,
    REJECTED,
    FAILED,
    AMBIGUOUS,
    EXHAUSTED,
    CANCELLED
}
