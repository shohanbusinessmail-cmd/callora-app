package com.callora.app.feature.scheduling

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.callora.app.CalloraApplication
import com.callora.app.CalloraViewModel
import com.callora.app.core.model.ScheduleModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.TimeZone

class SchedulingViewModel(app: CalloraApplication) : CalloraViewModel(app) {

    val schedules: StateFlow<List<ScheduleModel>> =
        container.scheduleRepository.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(
        id: Long,
        name: String,
        weekdays: Set<Int>,
        allDay: Boolean,
        startMinute: Int?,
        endMinute: Int?,
        enabled: Boolean = true,
    ) {
        viewModelScope.launch {
            container.scheduleRepository.save(
                ScheduleModel(
                    id = id,
                    name = name.ifBlank { "Schedule" },
                    enabled = enabled,
                    activeWeekdays = weekdays,
                    startMinuteOfDay = if (allDay) null else startMinute,
                    endMinuteOfDay = if (allDay) null else endMinute,
                    zoneId = TimeZone.getDefault().id,
                ),
            )
        }
    }

    fun delete(schedule: ScheduleModel) {
        viewModelScope.launch { container.scheduleRepository.delete(schedule) }
    }

    companion object {
        fun factory(app: CalloraApplication): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SchedulingViewModel(app) as T
            }
    }
}
