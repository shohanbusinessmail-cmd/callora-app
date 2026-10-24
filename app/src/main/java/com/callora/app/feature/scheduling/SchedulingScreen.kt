package com.callora.app.feature.scheduling

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callora.app.R
import com.callora.app.core.designsystem.component.CalloraCard
import com.callora.app.core.designsystem.component.EmptyStateView
import com.callora.app.core.designsystem.component.InfoBanner
import com.callora.app.core.designsystem.component.LabeledSwitchRow
import com.callora.app.core.designsystem.component.PrimaryCalloraButton
import com.callora.app.core.designsystem.component.SecondaryCalloraButton
import com.callora.app.core.model.ScheduleModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SchedulingScreen(viewModel: SchedulingViewModel, onBack: () -> Unit = {}) {
    val schedules by viewModel.schedules.collectAsStateWithLifecycle()
    var editorVisible by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ScheduleModel?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scheduling_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editing = null
                editorVisible = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_add))
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            InfoBanner(
                text = stringResource(R.string.scheduling_disclaimer),
                modifier = Modifier.padding(16.dp),
            )
            if (schedules.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Filled.Add,
                    title = stringResource(R.string.scheduling_title),
                    body = stringResource(R.string.scheduling_empty),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(schedules, key = { it.id }) { schedule ->
                        ScheduleCard(
                            schedule = schedule,
                            onEdit = {
                                editing = schedule
                                editorVisible = true
                            },
                            onDelete = { viewModel.delete(schedule) },
                        )
                    }
                }
            }
        }
    }

    if (editorVisible) {
        ScheduleEditorDialog(
            initial = editing,
            onDismiss = { editorVisible = false },
            onSave = { name, weekdays, allDay, start, end ->
                viewModel.save(
                    id = editing?.id ?: 0L,
                    name = name,
                    weekdays = weekdays,
                    allDay = allDay,
                    startMinute = start,
                    endMinute = end,
                )
                editorVisible = false
            },
        )
    }
}

@Composable
private fun ScheduleCard(
    schedule: ScheduleModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    CalloraCard {
        Text(schedule.name, style = MaterialTheme.typography.titleMedium)
        val weekdayLabels = buildList {
            schedule.activeWeekdays.sorted().forEach { add(weekdayShort(it)) }
        }.joinToString()
        Text(
            weekdayLabels,
            style = MaterialTheme.typography.bodyMedium,
        )
        val window = if (schedule.startMinuteOfDay == null || schedule.endMinuteOfDay == null) {
            stringResource(R.string.schedule_all_day)
        } else {
            "${formatMinute(schedule.startMinuteOfDay)} – ${formatMinute(schedule.endMinuteOfDay)}"
        }
        Text(window, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryCalloraButton(text = stringResource(R.string.action_edit), onClick = onEdit, modifier = Modifier.weight(1f))
            TextButton(onClick = onDelete) { Text(stringResource(R.string.action_delete)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ScheduleEditorDialog(
    initial: ScheduleModel?,
    onDismiss: () -> Unit,
    onSave: (String, Set<Int>, Boolean, Int?, Int?) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var weekdays by remember { mutableStateOf(initial?.activeWeekdays ?: setOf(1, 2, 3, 4, 5)) }
    var allDay by remember { mutableStateOf(initial?.startMinuteOfDay == null) }
    var start by remember { mutableIntStateOf(initial?.startMinuteOfDay ?: 9 * 60) }
    var end by remember { mutableIntStateOf(initial?.endMinuteOfDay ?: 18 * 60) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.scheduling_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.schedule_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(stringResource(R.string.schedule_weekdays), style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..7).forEach { day ->
                        FilterChip(
                            selected = day in weekdays,
                            onClick = {
                                weekdays = if (day in weekdays) weekdays - day else weekdays + day
                            },
                            label = { Text(weekdayShort(day)) },
                        )
                    }
                }
                LabeledSwitchRow(
                    label = stringResource(R.string.schedule_all_day),
                    description = null,
                    checked = allDay,
                    onCheckedChange = { allDay = it },
                )
                if (!allDay) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = { showStartPicker = true }, modifier = Modifier.weight(1f)) {
                            Text("${stringResource(R.string.schedule_start)} ${formatMinute(start)}")
                        }
                        Button(onClick = { showEndPicker = true }, modifier = Modifier.weight(1f)) {
                            Text("${stringResource(R.string.schedule_end)} ${formatMinute(end)}")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(name, weekdays, allDay, start, end)
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )

    if (showStartPicker) {
        val state = rememberTimePickerState(initialHour = start / 60, initialMinute = start % 60)
        TimePickerDialog(
            onDismiss = { showStartPicker = false },
            onConfirm = {
                start = state.hour * 60 + state.minute
                showStartPicker = false
            },
            state = state,
        )
    }
    if (showEndPicker) {
        val state = rememberTimePickerState(initialHour = end / 60, initialMinute = end % 60)
        TimePickerDialog(
            onDismiss = { showEndPicker = false },
            onConfirm = {
                end = state.hour * 60 + state.minute
                showEndPicker = false
            },
            state = state,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    state: androidx.compose.material3.TimePickerState,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
        text = { TimePicker(state = state) },
    )
}

@Composable
private fun weekdayShort(day: Int): String = stringResource(
    when (day) {
        1 -> R.string.weekday_1
        2 -> R.string.weekday_2
        3 -> R.string.weekday_3
        4 -> R.string.weekday_4
        5 -> R.string.weekday_5
        6 -> R.string.weekday_6
        else -> R.string.weekday_7
    },
)

private fun formatMinute(minuteOfDay: Int): String {
    val hour = minuteOfDay / 60
    val minute = minuteOfDay % 60
    return "%02d:%02d".format(hour, minute)
}
