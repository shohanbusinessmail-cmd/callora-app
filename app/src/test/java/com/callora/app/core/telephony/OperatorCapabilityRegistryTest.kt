package com.callora.app.core.telephony

import com.callora.app.core.model.CapabilityStatus
import com.callora.app.core.model.ForwardingCondition
import com.callora.app.core.model.OperatorId
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class OperatorCapabilityRegistryTest {

    @Test
    fun all_five_target_operators_are_present() {
        val operators = OperatorCapabilityRegistry.all().map { it.operator }
        assertThat(operators).containsExactly(
            OperatorId.GRAMEENPHONE,
            OperatorId.ROBI,
            OperatorId.AIRTEL_BANGLADESH,
            OperatorId.BANGLALINK,
            OperatorId.TELETALK,
        )
    }

    @Test
    fun grameenphone_cfnrc_has_official_evidence() {
        val gp = OperatorCapabilityRegistry.forOperator(OperatorId.GRAMEENPHONE)
        requireNotNull(gp)
        assertThat(gp.capabilityFor(ForwardingCondition.CFNRC).status)
            .isEqualTo(CapabilityStatus.SUPPORTED_OFFICIAL)
        assertThat(gp.evidence.any { it.isOfficial }).isTrue()
    }

    @Test
    fun teletalk_is_honestly_marked_not_verified() {
        val teletalk = OperatorCapabilityRegistry.forOperator(OperatorId.TELETALK)
        requireNotNull(teletalk)
        ForwardingCondition.entries.forEach { condition ->
            assertThat(teletalk.capabilityFor(condition).status)
                .isEqualTo(CapabilityStatus.NOT_VERIFIED)
        }
    }

    @Test
    fun mcc_mnc_resolution_matches_itu_registry() {
        assertThat(OperatorId.fromMccMnc("47001")).isEqualTo(OperatorId.GRAMEENPHONE)
        assertThat(OperatorId.fromMccMnc("47002")).isEqualTo(OperatorId.ROBI)
        assertThat(OperatorId.fromMccMnc("47003")).isEqualTo(OperatorId.BANGLALINK)
        assertThat(OperatorId.fromMccMnc("47004")).isEqualTo(OperatorId.TELETALK)
        assertThat(OperatorId.fromMccMnc("47007")).isEqualTo(OperatorId.AIRTEL_BANGLADESH)
        assertThat(OperatorId.fromMccMnc(null)).isEqualTo(OperatorId.UNKNOWN)
        assertThat(OperatorId.fromMccMnc("99999")).isEqualTo(OperatorId.UNKNOWN)
    }

    @Test
    fun every_record_carries_evidence_and_review_date() {
        OperatorCapabilityRegistry.all().forEach { capability ->
            assertThat(capability.evidence).isNotEmpty()
            assertThat(capability.lastReviewed).isNotEmpty()
            capability.evidence.forEach { ev: com.callora.app.core.model.EvidenceRecord ->
                // Absolute URL only; one archived 2011 source is served over plain http.
                assertThat(ev.url).startsWith("http")
                assertThat(ev.dateChecked).isNotEmpty()
            }
        }
    }

    @Test
    fun unknown_operator_does_not_claim_support() {
        val unknown = OperatorCapabilityRegistry.unknown()
        ForwardingCondition.entries.forEach { condition ->
            assertThat(unknown.capabilityFor(condition).status)
                .isEqualTo(CapabilityStatus.NOT_VERIFIED)
        }
    }
}
