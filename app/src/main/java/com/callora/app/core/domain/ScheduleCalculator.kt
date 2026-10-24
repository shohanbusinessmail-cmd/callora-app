package com.callora.app.core.domain

import com.callora.app.core.model.ScheduleModel
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Pure scheduling math. Android background execution is inexact (Doze, app
 * standby, manufacturer restrictions), so callers must treat results as
 * earliest-execution hints, never precise alarms.
 */
object ScheduleCalculator {

    /** ISO-8601 style weekday used by [ScheduleModel]: 1=Monday … 7=Sunday. */
    fun isoWeekday(zoned: ZonedDateTime): Int = zoned.dayOfWeek.value

    fun isActiveAt(schedule: ScheduleModel, instant: Instant, zoneId: ZoneId): Boolean {
        if (!schedule.enabled || schedule.activeWeekdays.isEmpty()) return false
        val zoned = instant.atZone(zoneId)
        if (isoWeekday(zoned) !in schedule.activeWeekdays) return false
        val start = schedule.startMinuteOfDay
        val end = schedule.endMinuteOfDay
        if (start == null || end == null) return true // all-day on selected weekdays
        val now = zoned.hour * 60 + zoned.minute
        return if (start <= end) {
            now in start until end
        } else {
            // Overnight window, e.g. 22:00–06:00.
            now >= start || now < end
        }
    }

    /**
     * Returns the next instant at which the schedule becomes active, or null if
     * it is disabled / has no weekdays. Scans at most 8 days.
     */
    fun nextActivation(schedule: ScheduleModel, from: Instant, zoneId: ZoneId): Instant? {
        if (!schedule.enabled || schedule.activeWeekdays.isEmpty()) return null
        val start = schedule.startMinuteOfDay ?: return null
        var candidate = from.atZone(zoneId)
            .withHour(start / 60)
            .withMinute(start % 60)
            .withSecond(0)
            .withNano(0)
        if (!candidate.toInstant().isAfter(from)) {
            candidate = candidate.plusDays(1)
        }
        repeat(8) {
            if (isoWeekday(candidate) in schedule.activeWeekdays) {
                return candidate.toInstant()
            }
            candidate = candidate.plusDays(1)
        }
        return null
    }

    fun zoneIdOrDefault(schedule: ScheduleModel): ZoneId =
        runCatching { ZoneId.of(schedule.zoneId) }.getOrDefault(ZoneId.systemDefault())
}
