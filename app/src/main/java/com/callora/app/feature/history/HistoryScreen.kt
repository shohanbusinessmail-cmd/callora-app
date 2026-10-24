package com.callora.app.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callora.app.R
import com.callora.app.core.designsystem.component.CalloraCard
import com.callora.app.core.designsystem.component.EmptyStateView
import com.callora.app.core.designsystem.theme.StatusError
import com.callora.app.core.designsystem.theme.StatusInfo
import com.callora.app.core.designsystem.theme.StatusSuccess
import com.callora.app.core.designsystem.theme.StatusWarning
import com.callora.app.core.model.HistoryEvent
import com.callora.app.core.model.HistoryOutcome
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel) {
    val events by viewModel.events.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                actions = {
                    if (events.isNotEmpty()) {
                        IconButton(onClick = { confirmClear = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.history_clear))
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (events.isEmpty()) {
            EmptyStateView(
                icon = Icons.Filled.History,
                title = stringResource(R.string.history_title),
                body = stringResource(R.string.history_empty),
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(events, key = { it.id }) { event ->
                    HistoryRow(event)
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.history_clear)) },
            text = { Text(stringResource(R.string.history_clear)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clear()
                    confirmClear = false
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun HistoryRow(event: HistoryEvent) {
    val color = when (event.outcome) {
        HistoryOutcome.SUCCESS -> StatusSuccess
        HistoryOutcome.WARNING -> StatusWarning
        HistoryOutcome.ERROR -> StatusError
        HistoryOutcome.INFO -> StatusInfo
    }
    CalloraCard {
        Row(horizontalArrangement = Arrangement.SpaceBetween) {
            Text(eventTypeLabel(event.type.name), style = MaterialTheme.typography.titleMedium, color = color)
            Text(
                DateFormat.getDateTimeInstance().format(Date(event.timestampEpochMs)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            event.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun eventTypeLabel(name: String): String {
    val resId = when (name) {
        "PROFILE_CHANGED" -> R.string.history_type_PROFILE_CHANGED
        "DESTINATION_CHANGED" -> R.string.history_type_DESTINATION_CHANGED
        "ACTIVATION_REQUESTED" -> R.string.history_type_ACTIVATION_REQUESTED
        "ACTIVATION_ACCEPTED" -> R.string.history_type_ACTIVATION_ACCEPTED
        "STATUS_VERIFIED" -> R.string.history_type_STATUS_VERIFIED
        "STATUS_QUERY_FAILED" -> R.string.history_type_STATUS_QUERY_FAILED
        "NETWORK_LOST" -> R.string.history_type_NETWORK_LOST
        "NETWORK_RESTORED" -> R.string.history_type_NETWORK_RESTORED
        "SIM_CHANGED" -> R.string.history_type_SIM_CHANGED
        "RETRY_SCHEDULED" -> R.string.history_type_RETRY_SCHEDULED
        "RETRY_EXHAUSTED" -> R.string.history_type_RETRY_EXHAUSTED
        "MANUAL_ACTION_REQUIRED" -> R.string.history_type_MANUAL_ACTION_REQUIRED
        "DEACTIVATION_REQUESTED" -> R.string.history_type_DEACTIVATION_REQUESTED
        "DEACTIVATION_VERIFIED" -> R.string.history_type_DEACTIVATION_VERIFIED
        "PERMISSION_CHANGED" -> R.string.history_type_PERMISSION_CHANGED
        else -> R.string.history_type_RECONCILIATION
    }
    return stringResource(resId)
}
