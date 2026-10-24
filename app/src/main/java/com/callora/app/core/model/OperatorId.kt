package com.callora.app.core.model

/**
 * Target mobile operators in Bangladesh. MCC for Bangladesh is 470
 * (ITU/3GPP assignment); MNC values follow the ITU region-4xx registry.
 */
enum class OperatorId(
    val mcc: String,
    val mnc: String,
    val displayName: String,
    val numberPrefixes: List<String>,
) {
    GRAMEENPHONE(
        mcc = "470",
        mnc = "01",
        displayName = "Grameenphone",
        numberPrefixes = listOf("017", "013"),
    ),
    ROBI(
        mcc = "470",
        mnc = "02",
        displayName = "Robi",
        numberPrefixes = listOf("018", "016"),
    ),
    BANGLALINK(
        mcc = "470",
        mnc = "03",
        displayName = "Banglalink",
        numberPrefixes = listOf("019", "014"),
    ),
    TELETALK(
        mcc = "470",
        mnc = "04",
        displayName = "Teletalk",
        numberPrefixes = listOf("015"),
    ),
    AIRTEL_BANGLADESH(
        mcc = "470",
        mnc = "07",
        displayName = "Airtel Bangladesh",
        // 016 is shared with Robi after the brand merger; prefix matching is
        // therefore best-effort only and never used as an identity guarantee.
        numberPrefixes = listOf("016"),
    ),
    UNKNOWN(
        mcc = "",
        mnc = "",
        displayName = "Unknown operator",
        numberPrefixes = emptyList(),
    );

    val mccMnc: String get() = "$mcc$mnc"

    companion object {
        /** Resolves an MCC+MNC pair (e.g. "47002") to a known operator. */
        fun fromMccMnc(mccMnc: String?): OperatorId {
            if (mccMnc.isNullOrBlank() || mccMnc.length < 5) return UNKNOWN
            return entries.firstOrNull { it != UNKNOWN && it.mccMnc == mccMnc }
                ?: UNKNOWN
        }

        /** Best-effort resolution from a Bangladesh mobile number prefix. */
        fun fromNumberPrefix(nationalNumber: String?): OperatorId {
            val digits = nationalNumber?.filter { it.isDigit() }.orEmpty()
            if (digits.length < 3) return UNKNOWN
            val prefix = digits.take(3)
            return entries.firstOrNull { it.numberPrefixes.contains(prefix) } ?: UNKNOWN
        }
    }
}
