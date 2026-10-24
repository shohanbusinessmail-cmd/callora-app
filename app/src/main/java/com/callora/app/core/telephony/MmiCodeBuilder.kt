package com.callora.app.core.telephony

import com.callora.app.core.model.ForwardingCondition
import com.callora.app.core.model.OperationAction

/**
 * Builds standard GSM/3GPP TS 22.004 Man-Machine Interface (MMI) strings.
 *
 * These are *standards-based* supplementary-service sequences shared across GSM
 * networks — not invented operator-specific codes. Whether a given operator has
 * the service provisioned or charges for it is tracked separately in the
 * [com.callora.app.core.telephony.OperatorCapabilityRegistry].
 */
object MmiCodeBuilder {

    /**
     * Registration + activation: "**<service>*<destination>#".
     * For CFNRY the no-reply timer variant is [buildRegisterNoReply].
     */
    fun buildRegister(condition: ForwardingCondition, destinationE164: String): String {
        val dest = requireDialable(destinationE164)
        return "**${condition.gsmServiceCode}*$dest#"
    }

    /** CFNRY registration with an explicit no-reply timer (GSM range 5–30 s). */
    fun buildRegisterNoReply(destinationE164: String, timerSeconds: Int): String {
        require(timerSeconds in 5..30) { "CFNRY timer must be within 5..30 seconds" }
        val dest = requireDialable(destinationE164)
        return "**${ForwardingCondition.CFNRY.gsmServiceCode}*$dest**$timerSeconds#"
    }

    /** Retain-and-activate a previously registered rule: "*<service>#". */
    fun buildActivate(condition: ForwardingCondition): String =
        "*${condition.gsmServiceCode}#"

    /** Deactivate but retain the registration: "#<service>#". */
    fun buildDeactivateRetain(condition: ForwardingCondition): String =
        "#${condition.gsmServiceCode}#"

    /** Erasure deactivation (removes the rule): "##<service>#". */
    fun buildErase(condition: ForwardingCondition): String =
        "##${condition.gsmServiceCode}#"

    /** Interrogation/status query: "*#<service>#". */
    fun buildQuery(condition: ForwardingCondition): String =
        "*#${condition.gsmServiceCode}#"

    /** Erase all call forwarding registrations: "##002#". */
    fun buildEraseAll(): String = "##002#"

    /** Resolves the correct MMI string for a lifecycle action. */
    fun buildForAction(
        action: OperationAction,
        condition: ForwardingCondition,
        destinationE164: String?,
        noReplyTimerSeconds: Int? = null,
    ): String = when (action) {
        OperationAction.ACTIVATE -> when {
            condition == ForwardingCondition.CFNRY && noReplyTimerSeconds != null ->
                buildRegisterNoReply(requireNotNull(destinationE164), noReplyTimerSeconds)
            destinationE164 != null -> buildRegister(condition, destinationE164)
            else -> buildActivate(condition)
        }
        OperationAction.DEACTIVATE -> buildErase(condition)
        OperationAction.QUERY -> buildQuery(condition)
    }

    private fun requireDialable(destinationE164: String): String {
        val cleaned = destinationE164.removePrefix("+").filter { it.isDigit() }
        require(cleaned.length in 7..15) {
            "Destination must contain 7..15 dialable digits, got ${cleaned.length}"
        }
        return cleaned
    }
}
