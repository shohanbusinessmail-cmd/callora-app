package com.callora.app.core.domain

import com.callora.app.core.model.FailureReason
import com.callora.app.core.model.ForwardingOperation
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * Bounded exponential backoff with jitter. Ambiguous or potentially chargeable
 * failures are never retried automatically — the user must re-confirm.
 */
class RetryPolicy(
    val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    private val baseDelayMs: Long = 30_000L,
    private val maxDelayMs: Long = 6 * 60 * 60 * 1_000L,
    private val random: Random = Random.Default,
) {

    fun isRetryable(reason: FailureReason?): Boolean = reason in RETRYABLE_REASONS

    fun shouldRetry(operation: ForwardingOperation): Boolean {
        if (!operation.retryEligible) return false
        if (operation.attempts >= operation.maxAttempts) return false
        return isRetryable(operation.failureReason)
    }

    /**
     * Delay before attempt [failedAttempt] (1-based count of failures so far).
     * Jitter is bounded to ±25 % so tests can assert the deterministic window.
     */
    fun delayMillis(failedAttempt: Int): Long {
        val exp = baseDelayMs * 2.0.pow(failedAttempt - 1).toLong()
        val capped = min(exp, maxDelayMs)
        val jitter = random.nextLong(-capped / 4, capped / 4 + 1)
        return (capped + jitter).coerceAtLeast(1_000L)
    }

    companion object {
        const val DEFAULT_MAX_ATTEMPTS = 4

        val RETRYABLE_REASONS: Set<FailureReason> = setOf(
            FailureReason.NETWORK_UNAVAILABLE,
            FailureReason.TIMEOUT,
            FailureReason.SIM_UNAVAILABLE,
        )
    }
}
