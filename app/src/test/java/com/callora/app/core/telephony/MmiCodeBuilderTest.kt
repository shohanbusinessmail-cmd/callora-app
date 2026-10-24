package com.callora.app.core.telephony

import com.callora.app.core.model.ForwardingCondition
import com.callora.app.core.model.OperationAction
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MmiCodeBuilderTest {

    @Test
    fun registers_unreachable_forwarding_with_standard_gsm_syntax() {
        val mmi = MmiCodeBuilder.buildRegister(ForwardingCondition.CFNRC, "+8801712345678")
        assertThat(mmi).isEqualTo("**62*8801712345678#")
    }

    @Test
    fun registers_unconditional_and_busy_conditions() {
        assertThat(MmiCodeBuilder.buildRegister(ForwardingCondition.CFU, "8801812345678"))
            .isEqualTo("**21*8801812345678#")
        assertThat(MmiCodeBuilder.buildRegister(ForwardingCondition.CFB, "8801812345678"))
            .isEqualTo("**67*8801812345678#")
    }

    @Test
    fun builds_no_reply_timer_variant() {
        assertThat(MmiCodeBuilder.buildRegisterNoReply("8801712345678", 20))
            .isEqualTo("**61*8801712345678**20#")
    }

    @Test
    fun builds_erasure_and_query_strings() {
        assertThat(MmiCodeBuilder.buildErase(ForwardingCondition.CFNRC)).isEqualTo("##62#")
        assertThat(MmiCodeBuilder.buildQuery(ForwardingCondition.CFNRC)).isEqualTo("*#62#")
        assertThat(MmiCodeBuilder.buildEraseAll()).isEqualTo("##002#")
    }

    @Test
    fun buildForAction_maps_lifecycle_actions() {
        assertThat(
            MmiCodeBuilder.buildForAction(
                OperationAction.DEACTIVATE,
                ForwardingCondition.CFU,
                destinationE164 = null,
            ),
        ).isEqualTo("##21#")
        assertThat(
            MmiCodeBuilder.buildForAction(
                OperationAction.QUERY,
                ForwardingCondition.CFB,
                destinationE164 = null,
            ),
        ).isEqualTo("*#67#")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejects_too_short_destination() {
        MmiCodeBuilder.buildRegister(ForwardingCondition.CFNRC, "1234")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejects_timer_outside_gsm_range() {
        MmiCodeBuilder.buildRegisterNoReply("8801712345678", 60)
    }
}
