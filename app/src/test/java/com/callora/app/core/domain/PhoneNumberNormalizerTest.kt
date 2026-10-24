package com.callora.app.core.domain

import com.callora.app.core.model.DestinationValidation
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PhoneNumberNormalizerTest {

    private val normalizer = PhoneNumberNormalizer()

    @Test
    fun normalizes_local_bangladesh_number_to_e164() {
        val result = normalizer.validate("01712345678")
        assertThat(result).isInstanceOf(DestinationValidation.Valid::class.java)
        assertThat((result as DestinationValidation.Valid).e164).isEqualTo("+8801712345678")
    }

    @Test
    fun accepts_international_format() {
        val result = normalizer.validate("+880 1812-345678")
        assertThat(result).isInstanceOf(DestinationValidation.Valid::class.java)
        assertThat((result as DestinationValidation.Valid).e164).isEqualTo("+8801812345678")
    }

    @Test
    fun rejects_empty_input() {
        val result = normalizer.validate("   ")
        assertThat(result).isEqualTo(DestinationValidation.Invalid(DestinationValidation.InvalidReason.EMPTY))
    }

    @Test
    fun rejects_non_numeric_garbage() {
        val result = normalizer.validate("not-a-number")
        assertThat(result).isInstanceOf(DestinationValidation.Invalid::class.java)
    }

    @Test
    fun rejects_foreign_region() {
        // A valid US number must not pass the Bangladesh-only policy.
        val result = normalizer.validate("+12025550173")
        assertThat(result).isEqualTo(
            DestinationValidation.Invalid(DestinationValidation.InvalidReason.WRONG_REGION),
        )
    }

    @Test
    fun validation_never_implies_reachability() {
        // Documented guarantee: syntax validation only.
        val result = normalizer.validate("01987654321")
        assertThat(result).isInstanceOf(DestinationValidation.Valid::class.java)
    }
}
