package com.callora.app.core.domain

import com.callora.app.core.model.BackendType
import com.callora.app.core.model.FailureReason
import com.callora.app.core.model.ForwardingCondition
import com.callora.app.core.model.ForwardingOperation
import com.callora.app.core.model.OperationAction
import com.callora.app.core.model.OperationStatus
import com.callora.app.core.model.VerificationProvenance
import com.google.common.truth.Truth.assertThat
import kotlin.random.Random
import org.junit.Test

class RetryPolicyTest {

    private val policy = RetryPolicy(maxAttempts = 4, baseDelayMs = 30_000, random = Random(0))

    @Test
    fun only_transient_reasons_are_retryable() {
        assertThat(policy.isRetryable(FailureReason.NETWORK_UNAVAILABLE)).isTrue()
        assertThat(policy.isRetryable(FailureReason.TIMEOUT)).isTrue()
        assertThat(policy.isRetryable(FailureReason.CARRIER_REJECTION)).isFalse()
        assertThat(policy.isRetryable(FailureReason.AMBIGUOUS_RESPONSE)).isFalse()
        assertThat(policy.isRetryable(FailureReason.INVALID_DESTINATION)).isFalse()
        assertThat(policy.isRetryable(FailureReason.PERMISSION_DENIED)).isFalse()
    }

    @Test
    fun backoff_grows_and_is_bounded() {
        val first = policy.delayMillis(1)
        val second = policy.delayMillis(2)
        val third = policy.delayMillis(3)
        assertThat(second).isAtLeast(first)
        assertThat(third).isAtLeast(second)
        repeat(10) { attempt ->
            assertThat(policy.delayMillis(attempt + 1)).isAtLeast(1_000L)
        }
    }

    @Test
    fun operation_stops_retrying_after_max_attempts() {
        val op = operation(attempts = 4, reason = FailureReason.NETWORK_UNAVAILABLE, retryEligible = true)
        assertThat(policy.shouldRetry(op)).isFalse()
    }

    @Test
    fun rejected_chargeable_operation_is_never_retried() {
        val op = operation(attempts = 1, reason = FailureReason.CARRIER_REJECTION, retryEligible = false)
        assertThat(policy.shouldRetry(op)).isFalse()
    }

    private fun operation(
        attempts: Int,
        reason: FailureReason,
        retryEligible: Boolean,
    ): ForwardingOperation = ForwardingOperation(
        operationId = "op-1",
        subscriptionId = 1,
        condition = ForwardingCondition.CFNRC,
        action = OperationAction.ACTIVATE,
        destinationE164 = "+8801712345678",
        backend = BackendType.USER_CONFIRMED_MMI,
        createdAtEpochMs = 0,
        updatedAtEpochMs = 0,
        status = OperationStatus.FAILED,
        attempts = attempts,
        maxAttempts = 4,
        provenance = VerificationProvenance.LOCAL_DESIRED_POLICY,
        failureReason = reason,
        retryEligible = retryEligible,
        detail = null,
    )
}
