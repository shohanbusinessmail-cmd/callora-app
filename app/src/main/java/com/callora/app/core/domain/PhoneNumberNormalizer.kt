package com.callora.app.core.domain

import com.callora.app.core.model.DestinationValidation
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber

/**
 * Wraps Google's libphonenumber for Bangladesh-centric validation. Syntactic
 * validation never implies that the number is active, reachable or eligible for
 * forwarding — the UI states this explicitly.
 */
class PhoneNumberNormalizer(
    private val defaultRegion: String = REGION_BANGLADESH,
) {
    private val util: PhoneNumberUtil = PhoneNumberUtil.getInstance()

    fun validate(input: String): DestinationValidation {
        val cleaned = input.trim()
        if (cleaned.isEmpty()) {
            return DestinationValidation.Invalid(DestinationValidation.InvalidReason.EMPTY)
        }
        val parsed: Phonenumber.PhoneNumber = try {
            util.parse(cleaned, defaultRegion)
        } catch (e: NumberParseException) {
            return mapParseError(e)
        }
        if (!util.isPossibleNumber(parsed)) {
            return DestinationValidation.Invalid(DestinationValidation.InvalidReason.TOO_SHORT)
        }
        if (!util.isValidNumber(parsed)) {
            return DestinationValidation.Invalid(DestinationValidation.InvalidReason.NOT_A_NUMBER)
        }
        val e164 = util.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
        if (util.getRegionCodeForNumber(parsed) != defaultRegion) {
            return DestinationValidation.Invalid(DestinationValidation.InvalidReason.WRONG_REGION)
        }
        val formatted = util.format(parsed, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)
        return DestinationValidation.Valid(e164 = e164, formatted = formatted)
    }

    fun toE164OrNull(input: String): String? = (validate(input) as? DestinationValidation.Valid)?.e164

    fun formatInternational(e164: String): String {
        return try {
            val parsed = util.parse(e164, defaultRegion)
            util.format(parsed, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)
        } catch (e: NumberParseException) {
            e164
        }
    }

    private fun mapParseError(e: NumberParseException): DestinationValidation =
        when (e.errorType) {
            NumberParseException.ErrorType.TOO_SHORT_NSN ->
                DestinationValidation.Invalid(DestinationValidation.InvalidReason.TOO_SHORT)
            NumberParseException.ErrorType.TOO_LONG_NSN ->
                DestinationValidation.Invalid(DestinationValidation.InvalidReason.TOO_LONG)
            NumberParseException.ErrorType.NOT_A_NUMBER ->
                DestinationValidation.Invalid(DestinationValidation.InvalidReason.NOT_A_NUMBER)
            else -> DestinationValidation.Invalid(DestinationValidation.InvalidReason.NOT_A_NUMBER)
        }

    companion object {
        const val REGION_BANGLADESH = "BD"
        const val COUNTRY_CALLING_CODE = "+880"
    }
}
