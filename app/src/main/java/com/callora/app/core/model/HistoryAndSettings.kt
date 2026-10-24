package com.callora.app.core.model

/** Categories of auditable activity events. No call content is ever stored. */
enum class HistoryEventType {
    PROFILE_CHANGED,
    DESTINATION_CHANGED,
    ACTIVATION_REQUESTED,
    ACTIVATION_ACCEPTED,
    STATUS_VERIFIED,
    STATUS_QUERY_FAILED,
    NETWORK_LOST,
    NETWORK_RESTORED,
    SIM_CHANGED,
    RETRY_SCHEDULED,
    RETRY_EXHAUSTED,
    MANUAL_ACTION_REQUIRED,
    DEACTIVATION_REQUESTED,
    DEACTIVATION_VERIFIED,
    PERMISSION_CHANGED,
    RECONCILIATION,
}

enum class HistoryOutcome { INFO, SUCCESS, WARNING, ERROR }

data class HistoryEvent(
    val id: Long = 0L,
    val timestampEpochMs: Long,
    val type: HistoryEventType,
    val outcome: HistoryOutcome,
    val subscriptionId: Int?,
    val operationId: String?,
    val message: String,
)

/** Deterministic output of the reconciliation engine (pure logic, unit tested). */
sealed interface ReconciliationDecision {
    data object NoAction : ReconciliationDecision
    data object QueryOnly : ReconciliationDecision
    data class Activate(
        val subscriptionId: Int,
        val condition: ForwardingCondition,
        val destinationE164: String,
    ) : ReconciliationDecision
    data class Deactivate(
        val subscriptionId: Int,
        val condition: ForwardingCondition,
    ) : ReconciliationDecision
    data class RequireManual(
        val subscriptionId: Int,
        val condition: ForwardingCondition,
        val reason: FailureReason,
    ) : ReconciliationDecision
    data class SuspendAutomation(
        val subscriptionId: Int,
        val reason: FailureReason,
    ) : ReconciliationDecision
}

enum class AppLanguage(val tag: String, val nativeLabel: String) {
    ENGLISH("en", "English"),
    BENGALI("bn", "বাংলা");

    companion object {
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.tag == tag } ?: ENGLISH
    }
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }
