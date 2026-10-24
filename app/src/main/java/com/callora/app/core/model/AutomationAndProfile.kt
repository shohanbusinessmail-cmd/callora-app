package com.callora.app.core.model

/** User-selectable automation policy (Part 9.2 of the product spec). */
enum class AutomationPolicy {
    /** Keep the unreachable-forwarding rule aligned with the desired policy. */
    ALWAYS_PROTECT,

    /** Protection with an explicit end time; deactivation happens while online. */
    TEMPORARY,

    /** Report status only; never modify carrier state without confirmation. */
    MANUAL,

    /** User-defined combination of conditions, schedules and recovery rules. */
    CUSTOM
}

enum class ProfileType { PERSONAL, OFFICE, CUSTOM, TEMPORARY }

/**
 * A forwarding profile stores *desired* configuration only. It is never proof
 * of carrier-side state — observed state lives in [ObservedForwardingStatus].
 */
data class ForwardingProfile(
    val id: Long = 0L,
    val name: String,
    val type: ProfileType,
    val enabledConditions: Set<ForwardingCondition>,
    val destinationId: Long?,
    val automationPolicy: AutomationPolicy,
    val scheduleId: Long? = null,
    val isDefault: Boolean = false,
    val createdAtEpochMs: Long = 0L,
    val updatedAtEpochMs: Long = 0L,
)

/**
 * Weekly schedule. Android background scheduling is inexact — execution may be
 * delayed by Doze/standby, so this model never promises second-level precision.
 */
data class ScheduleModel(
    val id: Long = 0L,
    val name: String,
    val enabled: Boolean,
    /** ISO-8601 weekdays: 1=Monday … 7=Sunday. */
    val activeWeekdays: Set<Int>,
    /** Minutes from midnight; null means "all day" when [activeWeekdays] match. */
    val startMinuteOfDay: Int?,
    val endMinuteOfDay: Int?,
    val zoneId: String,
)
