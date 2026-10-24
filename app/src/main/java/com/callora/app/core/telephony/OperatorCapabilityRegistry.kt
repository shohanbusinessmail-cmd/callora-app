package com.callora.app.core.telephony

import com.callora.app.core.model.CapabilityStatus
import com.callora.app.core.model.ConditionCapability
import com.callora.app.core.model.EvidenceConfidence
import com.callora.app.core.model.EvidenceRecord
import com.callora.app.core.model.ForwardingCondition
import com.callora.app.core.model.OperatorCapability
import com.callora.app.core.model.OperatorId

/**
 * Evidence-backed registry of Bangladesh operator call-forwarding capabilities.
 *
 * Nothing here is asserted from assumption: every capability points at one or
 * more [EvidenceRecord] entries. Anything without adequate evidence is marked
 * [CapabilityStatus.NOT_VERIFIED]. Experimental, on-device carrier verification
 * has not been performed by Callora and is recorded as such in docs.
 */
object OperatorCapabilityRegistry {

    private const val REVIEW_DATE = "2026-10-24"

    private val gsmStandardEvidence = EvidenceRecord(
        sourceTitle = "Wikipedia — Call forwarding (GSM MMI service codes 21/67/61/62)",
        url = "https://en.wikipedia.org/wiki/Call_forwarding",
        dateChecked = REVIEW_DATE,
        isOfficial = false,
        confidence = EvidenceConfidence.HIGH,
        notes = "Documents the 3GPP TS 22.004 MMI syntax shared by GSM operators.",
    )

    private val gpOfficialEvidence = EvidenceRecord(
        sourceTitle = "Grameenphone Roaming — Useful Information (GSM Feature Activation/Deactivation)",
        url = "https://roaming.grameenphone.com/useful-information",
        dateChecked = REVIEW_DATE,
        isOfficial = true,
        confidence = EvidenceConfidence.HIGH,
        notes = "Official GP domain listing **62*/##62# (not reachable) and **21*/##21# (all calls).",
    )

    private val gpSecondaryEvidence = EvidenceRecord(
        sourceTitle = "Grameenphone service case study — standard GSM features on prepaid and postpaid",
        url = "https://www.lawyersnjurists.com/article/case-study-grameenphone-service-satisfaction-analysis-customer-point-view",
        dateChecked = REVIEW_DATE,
        isOfficial = false,
        confidence = EvidenceConfidence.MEDIUM,
        notes = "States call forwarding is available on prepaid and postpaid; activation free, forwarded calls charged.",
    )

    private val robiEvidence = EvidenceRecord(
        sourceTitle = "BD SIM Link — Robi divert settings table (all four conditions)",
        url = "https://bdsimlink.wordpress.com/2019/09/24/call-settings/",
        dateChecked = REVIEW_DATE,
        isOfficial = false,
        confidence = EvidenceConfidence.MEDIUM,
        notes = "Secondary source listing Robi **21/**61/**62/**67 activation, ## erasure and *# query codes.",
    )

    private val airtelEvidence = EvidenceRecord(
        sourceTitle = "Telecom Bangladesh — Airtel call management / call divert",
        url = "http://telecom-infobd.blogspot.com/2011/01/airtel-call-management-services-call_2292.html",
        dateChecked = REVIEW_DATE,
        isOfficial = false,
        confidence = EvidenceConfidence.LOW,
        notes = "Dated (2011) secondary evidence predating the Robi–Airtel merger; treat with caution.",
    )

    private val banglalinkEvidence = EvidenceRecord(
        sourceTitle = "Banglalink call divert short codes (secondary)",
        url = "https://3g-offer.blogspot.com/2015/08/banglalink-call-divert-forwarding-process-short-codes.html",
        dateChecked = REVIEW_DATE,
        isOfficial = false,
        confidence = EvidenceConfidence.LOW,
        notes = "Dated (2015) secondary evidence consistent with the GSM standard syntax.",
    )

    private val genericBdEvidence = EvidenceRecord(
        sourceTitle = "Call Divert USSD Code Bangladesh (cross-operator GSM code summary)",
        url = "https://www.mimsms.com/call-divert-codes-bangladesh",
        dateChecked = REVIEW_DATE,
        isOfficial = false,
        confidence = EvidenceConfidence.MEDIUM,
        notes = "Cross-operator summary; not an operator-confirmed source.",
    )

    private val mccMncEvidence = EvidenceRecord(
        sourceTitle = "Wikipedia — Mobile network codes in ITU region 4xx (Bangladesh, MCC 470)",
        url = "https://en.wikipedia.org/wiki/Mobile_Network_Codes_in_ITU_region_4xx_(Asia)",
        dateChecked = REVIEW_DATE,
        isOfficial = false,
        confidence = EvidenceConfidence.HIGH,
        notes = "47001 GP, 47002 Robi, 47003 Banglalink, 47004 Teletalk, 47007 Airtel Bangladesh.",
    )

    private val capabilities: Map<OperatorId, OperatorCapability> = mapOf(
        OperatorId.GRAMEENPHONE to OperatorCapability(
            operator = OperatorId.GRAMEENPHONE,
            displayName = "Grameenphone",
            mccMnc = listOf("47001"),
            conditions = mapOf(
                ForwardingCondition.CFNRC to ConditionCapability(
                    ForwardingCondition.CFNRC, CapabilityStatus.SUPPORTED_OFFICIAL,
                    "Official GP roaming documentation lists **62* registration and ##62# erasure.",
                ),
                ForwardingCondition.CFU to ConditionCapability(
                    ForwardingCondition.CFU, CapabilityStatus.SUPPORTED_OFFICIAL,
                    "Official GP roaming documentation lists **21* registration and ##21# erasure.",
                ),
                ForwardingCondition.CFB to ConditionCapability(
                    ForwardingCondition.CFB, CapabilityStatus.SUPPORTED_DOCUMENTED,
                    "Standard GSM **67* sequence documented by secondary BD sources; not on the official GP page reviewed.",
                ),
                ForwardingCondition.CFNRY to ConditionCapability(
                    ForwardingCondition.CFNRY, CapabilityStatus.SUPPORTED_DOCUMENTED,
                    "Standard GSM **61* sequence documented by secondary BD sources; timer support not confirmed.",
                ),
            ),
            domesticDestinations = CapabilityStatus.SUPPORTED_DOCUMENTED,
            internationalDestinations = CapabilityStatus.NOT_VERIFIED,
            provisioningRequired = CapabilityStatus.SUPPORTED_DOCUMENTED,
            chargingSummary = "Secondary evidence: activation is free; forwarded calls are charged like an outgoing call to the destination. Confirm current tariffs with GP (121).",
            querySupported = CapabilityStatus.SUPPORTED_DOCUMENTED,
            remainsActiveWhilePoweredOff = CapabilityStatus.SUPPORTED_DOCUMENTED,
            limitations = listOf(
                "Callora cannot programmatically read the *#62# interrogation response on a non-carrier-privileged device.",
                "Current tariffs and prepaid/postpaid differences must be confirmed with GP customer care.",
                "Roaming behaviour may differ from domestic behaviour.",
            ),
            evidence = listOf(gpOfficialEvidence, gpSecondaryEvidence, gsmStandardEvidence, mccMncEvidence),
            lastReviewed = REVIEW_DATE,
        ),
        OperatorId.ROBI to OperatorCapability(
            operator = OperatorId.ROBI,
            displayName = "Robi",
            mccMnc = listOf("47002"),
            conditions = standardDocumented(
                robiEvidence,
                "Robi secondary sources document all four GSM conditions with standard syntax; no official Robi page was located during research.",
            ),
            domesticDestinations = CapabilityStatus.NOT_VERIFIED,
            internationalDestinations = CapabilityStatus.NOT_VERIFIED,
            provisioningRequired = CapabilityStatus.NOT_VERIFIED,
            chargingSummary = "Charging and provisioning not confirmed by an official Robi source. Verify via Robi customer care (121).",
            querySupported = CapabilityStatus.SUPPORTED_DOCUMENTED,
            remainsActiveWhilePoweredOff = CapabilityStatus.SUPPORTED_DOCUMENTED,
            limitations = listOf(
                "No official Robi call-forwarding documentation located; evidence is secondary and standards-based.",
                "App cannot parse MMI interrogation responses automatically.",
            ),
            evidence = listOf(robiEvidence, genericBdEvidence, gsmStandardEvidence, mccMncEvidence),
            lastReviewed = REVIEW_DATE,
        ),
        OperatorId.AIRTEL_BANGLADESH to OperatorCapability(
            operator = OperatorId.AIRTEL_BANGLADESH,
            displayName = "Airtel Bangladesh",
            mccMnc = listOf("47007"),
            conditions = standardDocumented(
                airtelEvidence,
                "Airtel BD secondary evidence (2011) documents the four GSM conditions; the network now operates under the Robi–Airtel merger.",
            ),
            domesticDestinations = CapabilityStatus.NOT_VERIFIED,
            internationalDestinations = CapabilityStatus.NOT_VERIFIED,
            provisioningRequired = CapabilityStatus.NOT_VERIFIED,
            chargingSummary = "Not confirmed. Airtel Bangladesh is operated by Robi Axiata; confirm via customer care.",
            querySupported = CapabilityStatus.SUPPORTED_DOCUMENTED,
            remainsActiveWhilePoweredOff = CapabilityStatus.SUPPORTED_DOCUMENTED,
            limitations = listOf(
                "Evidence predates the Robi–Airtel merger and is low confidence.",
                "Post-merger network behaviour may follow Robi policy.",
            ),
            evidence = listOf(airtelEvidence, gsmStandardEvidence, mccMncEvidence),
            lastReviewed = REVIEW_DATE,
        ),
        OperatorId.BANGLALINK to OperatorCapability(
            operator = OperatorId.BANGLALINK,
            displayName = "Banglalink",
            mccMnc = listOf("47003"),
            conditions = standardDocumented(
                banglalinkEvidence,
                "Banglalink secondary evidence documents standard GSM divert codes; no official Banglalink page was located.",
            ),
            domesticDestinations = CapabilityStatus.NOT_VERIFIED,
            internationalDestinations = CapabilityStatus.NOT_VERIFIED,
            provisioningRequired = CapabilityStatus.NOT_VERIFIED,
            chargingSummary = "Not confirmed by an official Banglalink source. Verify via Banglalink customer care (121).",
            querySupported = CapabilityStatus.SUPPORTED_DOCUMENTED,
            remainsActiveWhilePoweredOff = CapabilityStatus.SUPPORTED_DOCUMENTED,
            limitations = listOf(
                "Secondary evidence is dated (2015); current provisioning is unconfirmed.",
                "App cannot parse MMI interrogation responses automatically.",
            ),
            evidence = listOf(banglalinkEvidence, genericBdEvidence, gsmStandardEvidence, mccMncEvidence),
            lastReviewed = REVIEW_DATE,
        ),
        OperatorId.TELETALK to OperatorCapability(
            operator = OperatorId.TELETALK,
            displayName = "Teletalk",
            mccMnc = listOf("47004"),
            conditions = ForwardingCondition.entries.associateWith {
                ConditionCapability(
                    it,
                    CapabilityStatus.NOT_VERIFIED,
                    "Only generic cross-operator listings mention Teletalk; no Teletalk-specific forwarding evidence was located.",
                )
            },
            domesticDestinations = CapabilityStatus.NOT_VERIFIED,
            internationalDestinations = CapabilityStatus.NOT_VERIFIED,
            provisioningRequired = CapabilityStatus.NOT_VERIFIED,
            chargingSummary = "Unknown. Confirm directly with Teletalk customer care.",
            querySupported = CapabilityStatus.NOT_VERIFIED,
            remainsActiveWhilePoweredOff = CapabilityStatus.NOT_VERIFIED,
            limitations = listOf(
                "No operator-specific evidence; Callora will only offer standards-based MMI handoff with explicit NOT VERIFIED warnings.",
            ),
            evidence = listOf(genericBdEvidence, gsmStandardEvidence, mccMncEvidence),
            lastReviewed = REVIEW_DATE,
        ),
    )

    private fun standardDocumented(
        source: EvidenceRecord,
        notes: String,
    ): Map<ForwardingCondition, ConditionCapability> =
        ForwardingCondition.entries.associateWith {
            ConditionCapability(it, CapabilityStatus.SUPPORTED_DOCUMENTED, notes)
        }

    fun all(): List<OperatorCapability> =
        listOf(
            requireNotNull(capabilities[OperatorId.GRAMEENPHONE]),
            requireNotNull(capabilities[OperatorId.ROBI]),
            requireNotNull(capabilities[OperatorId.AIRTEL_BANGLADESH]),
            requireNotNull(capabilities[OperatorId.BANGLALINK]),
            requireNotNull(capabilities[OperatorId.TELETALK]),
        )

    fun forOperator(operatorId: OperatorId): OperatorCapability? = capabilities[operatorId]

    fun byMccMnc(mccMnc: String?): OperatorCapability? =
        capabilities.values.firstOrNull { it.mccMnc.contains(mccMnc) }

    /** Fallback capability record used when the SIM operator cannot be identified. */
    fun unknown(): OperatorCapability = OperatorCapability(
        operator = OperatorId.UNKNOWN,
        displayName = "Unknown operator",
        mccMnc = emptyList(),
        conditions = ForwardingCondition.entries.associateWith {
            ConditionCapability(it, CapabilityStatus.NOT_VERIFIED, "Operator unknown.")
        },
        domesticDestinations = CapabilityStatus.NOT_VERIFIED,
        internationalDestinations = CapabilityStatus.NOT_VERIFIED,
        provisioningRequired = CapabilityStatus.NOT_VERIFIED,
        chargingSummary = "Unknown operator — charging cannot be estimated.",
        querySupported = CapabilityStatus.NOT_VERIFIED,
        remainsActiveWhilePoweredOff = CapabilityStatus.NOT_VERIFIED,
        limitations = listOf("Operator could not be identified; only generic GSM workflows are available."),
        evidence = listOf(gsmStandardEvidence),
        lastReviewed = REVIEW_DATE,
    )
}
