package com.callora.app.core.domain

import com.callora.app.core.model.ScheduleModel
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class ScheduleCalculatorTest {

    private val zone = ZoneId.of("Asia/Dhaka")

    // 2026-10-26 is a Monday. 10:00 in Dhaka = 04:00 UTC.
    private val mondayMorning = Instant.parse("2026-10-26T04:00:00Z")
    private val mondayNight = Instant.parse("2026-10-26T16:00:00Z") // 22:00 Dhaka

    @Test
    fun active_inside_weekday_window() {
        val schedule = ScheduleModel(
            name = "Office",
            enabled = true,
            activeWeekdays = setOf(1, 2, 3, 4, 5),
            startMinuteOfDay = 9 * 60,
            endMinuteOfDay = 17 * 60,
            zoneId = zone.id,
        )
        assertThat(ScheduleCalculator.isActiveAt(schedule, mondayMorning, zone)).isTrue()
    }

    @Test
    fun inactive_outside_window_and_on_disabled_weekday() {
        val schedule = ScheduleModel(
            name = "Office",
            enabled = true,
            activeWeekdays = setOf(1),
            startMinuteOfDay = 9 * 60,
            endMinuteOfDay = 17 * 60,
            zoneId = zone.id,
        )
        // 22:00 Dhaka is outside the window.
        assertThat(ScheduleCalculator.isActiveAt(schedule, mondayNight, zone)).isFalse()
        // Disabled schedule never activates.
        assertThat(ScheduleCalculator.isActiveAt(schedule.copy(enabled = false), mondayMorning, zone)).isFalse()
    }

    @Test
    fun overnight_window_wraps_past_midnight() {
        val schedule = ScheduleModel(
            name = "Night",
            enabled = true,
            activeWeekdays = setOf(1),
            startMinuteOfDay = 22 * 60,
            endMinuteOfDay = 6 * 60,
            zoneId = zone.id,
        )
        assertThat(ScheduleCalculator.isActiveAt(schedule, mondayNight, zone)).isTrue()
    }

    @Test
    fun all_day_schedule_uses_weekday_only() {
        val schedule = ScheduleModel(
            name = "All day",
            enabled = true,
            activeWeekdays = setOf(1),
            startMinuteOfDay = null,
            endMinuteOfDay = null,
            zoneId = zone.id,
        )
        assertThat(ScheduleCalculator.isActiveAt(schedule, mondayNight, zone)).isTrue()
    }

    @Test
    fun next_activation_returns_null_when_disabled() {
        val schedule = ScheduleModel(
            name = "x",
            enabled = false,
            activeWeekdays = setOf(1),
            startMinuteOfDay = 9 * 60,
            endMinuteOfDay = 17 * 60,
            zoneId = zone.id,
        )
        assertThat(ScheduleCalculator.nextActivation(schedule, mondayMorning, zone)).isNull()
    }
}
