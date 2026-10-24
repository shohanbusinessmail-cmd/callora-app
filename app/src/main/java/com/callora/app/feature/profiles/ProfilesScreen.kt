package com.callora.app.feature.profiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import com.callora.app.core.designsystem.component.LabelHelpers
import com.callora.app.core.designsystem.component.PrimaryCalloraButton
import com.callora.app.core.designsystem.component.SecondaryCalloraButton
import com.callora.app.core.model.ForwardingProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesScreen(
    viewModel: ProfilesViewModel,
    onEdit: (Long) -> Unit,
    onOpenScheduling: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<ForwardingProfile?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.profiles_title)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { onEdit(-1L) }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.profile_create))
            }
        },
    ) { padding ->
        if (uiState.profiles.isEmpty()) {
            Column(Modifier.padding(padding)) {
                EmptyStateView(
                    icon = Icons.Filled.SwitchAccount,
                    title = stringResource(R.string.profiles_title),
                    body = stringResource(R.string.profiles_empty),
                    actionLabel = stringResource(R.string.profile_create),
                    onAction = { onEdit(-1L) },
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.profiles, key = { it.id }) { profile ->
                    CalloraCard {
                        Text(profile.name, style = MaterialTheme.typography.titleLarge)
                        Text(
                            profileTypeLabel(profile.type.name),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            policyLabel(profile.automationPolicy.name),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            profile.enabledConditions.joinToString { LabelHelpers.conditionLabel(it) },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (uiState.defaultProfileId == profile.id) {
                            Text(
                                stringResource(R.string.profile_default),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SecondaryCalloraButton(
                                text = stringResource(R.string.action_edit),
                                onClick = { onEdit(profile.id) },
                                modifier = Modifier.weight(1f),
                            )
                            SecondaryCalloraButton(
                                text = stringResource(R.string.profile_default),
                                onClick = { viewModel.setDefault(profile) },
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { pendingDelete = profile }) {
                                Text(stringResource(R.string.action_delete))
                            }
                        }
                    }
                }
                item {
                    PrimaryCalloraButton(
                        text = stringResource(R.string.scheduling_title),
                        onClick = onOpenScheduling,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    pendingDelete?.let { profile ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(profile.name) },
            text = { Text(stringResource(R.string.profile_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(profile)
                    pendingDelete = null
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun profileTypeLabel(name: String): String = when (name) {
    "PERSONAL" -> stringResource(R.string.profile_type_PERSONAL)
    "OFFICE" -> stringResource(R.string.profile_type_OFFICE)
    "CUSTOM" -> stringResource(R.string.profile_type_CUSTOM)
    "TEMPORARY" -> stringResource(R.string.profile_type_TEMPORARY)
    else -> name
}

@Composable
private fun policyLabel(name: String): String = when (name) {
    "ALWAYS_PROTECT" -> stringResource(R.string.policy_ALWAYS_PROTECT)
    "TEMPORARY" -> stringResource(R.string.policy_TEMPORARY)
    "MANUAL" -> stringResource(R.string.policy_MANUAL)
    "CUSTOM" -> stringResource(R.string.policy_CUSTOM)
    else -> name
}
