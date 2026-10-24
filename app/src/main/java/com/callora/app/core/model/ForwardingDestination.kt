package com.callora.app.core.model

/**
 * A saved forwarding destination. [e164] is the normalized canonical form used
 * for MMI generation; syntactic validity never proves the number is reachable.
 */
data class ForwardingDestination(
    val id: Long = 0L,
    val e164: String,
    val label: String,
    val rawInput: String,
    val createdAtEpochMs: Long,
) {
    /** Masked rendering suitable for logs and non-sensitive UI surfaces. */
    fun masked(): String {
        val digits = e164.filter { it.isDigit() }
        if (digits.length <= 6) return e164
        return "${digits.take(4)}••••${digits.takeLast(2)}"
    }
}

/** Result of validating user-entered destination text. */
sealed interface DestinationValidation {
    data class Valid(val e164: String, val formatted: String) : DestinationValidation
    data class Invalid(val reason: InvalidReason) : DestinationValidation

    enum class InvalidReason {
        EMPTY,
        TOO_SHORT,
        TOO_LONG,
        NOT_A_NUMBER,
        WRONG_REGION,
    }
}
