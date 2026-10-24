package com.callora.app.feature.dashboard

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callora.app.R
import com.callora.app.core.designsystem.component.CalloraCard
import com.callora.app.core.designsystem.component.EmptyStateView
import com.callora.app.core.designsystem.component.InfoBanner
import com.callora.app.core.designsystem.component.LabelHelpers
import com.callora.app.core.designsystem.component.LoadingView
import com.callora.app.core.designsystem.component.PrimaryCalloraButton
import com.callora.app.core.designsystem.component.ReadinessDot
import com.callora.app.core.designsystem.component.SecondaryCalloraButton
import com.callora.app.core.designsystem.component.StatusChip
import com.callora.app.core.designsystem.theme.StatusError
import com.callora.app.core.designsystem.theme.StatusSuccess
import com.callora.app.core.designsystem.theme.StatusWarning
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onOpenSims: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dashboard_title)) },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.action_retry))
                    }
                },
            )
        },
    ) { padding ->
        if (uiState.loading && uiState.capability == null) {
            LoadingView(stringResource(R.string.loading_generic), Modifier.padding(padding))
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { ReadinessCard(uiState, onGrantPermission = {
                permissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
            }) }

            if (uiState.capability?.readPhoneStateGranted == false) {
                item {
                    InfoBanner(text = stringResource(R.string.permission_rationale_phone_state), warning = true)
                }
            }
            if (uiState.capability?.networkConnected == false) {
                item {
                    InfoBanner(text = stringResource(R.string.readiness_network_off), warning = true)
                }
            }

            item {
                PrimaryCalloraButton(
                    text = stringResource(R.string.dashboard_primary_action_set_up),
                    onClick = onOpenSims,
                    leadingIcon = Icons.Filled.Shield,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (uiState.subscriptions.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Filled.Shield,
                        title = stringResource(R.string.dashboard_no_sim_title),
                        body = stringResource(R.string.dashboard_no_sim_body),
                    )
                }
            } else {
                items(uiState.subscriptions) { subscription ->
                    SubscriptionStatusCard(uiState, subscription.subscriptionId, subscription.stableLabel)
                }
            }

            if (uiState.pendingOperations.isNotEmpty()) {
                item {
                    CalloraCard {
                        Text(
                            stringResource(R.string.dashboard_pending_operations),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(8.dp))
                        uiState.pendingOperations.forEach { op ->
                            Text(
                                "${LabelHelpers.conditionLabel(op.condition)} — ${op.status.name}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }

            item {
                CalloraCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.nav_history),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            val last = uiState.settings.lastReconciliationEpochMs
                            Text(
                                if (last > 0) {
                                    stringResource(
                                        R.string.dashboard_last_verified,
                                        DateFormat.getDateTimeInstance().format(Date(last)),
                                    )
                                } else {
                                    stringResource(R.string.dashboard_never_verified)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = onOpenHistory) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun ReadinessCard(uiState: DashboardUiState, onGrantPermission: () -> Unit) {
    val capability = uiState.capability
    CalloraCard {
        Text(stringResource(R.string.dashboard_readiness_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        val networkOk = capability?.networkConnected == true
        val permissionOk = capability?.readPhoneStateGranted == true
        ReadinessRow(
            color = if (networkOk) StatusSuccess else StatusError,
            label = stringResource(
                if (networkOk) R.string.readiness_network_on else R.string.readiness_network_off,
            ),
        )
        ReadinessRow(
            color = if (permissionOk) StatusSuccess else StatusWarning,
            label = stringResource(
                if (permissionOk) R.string.readiness_permission_granted
                else R.string.readiness_permission_missing,
            ),
        )
        ReadinessRow(
            color = if (uiState.subscriptions.isNotEmpty()) StatusSuccess else StatusWarning,
            label = stringResource(R.string.readiness_sim_count, uiState.subscriptions.size),
        )
        if (!permissionOk) {
            Spacer(Modifier.height(12.dp))
            SecondaryCalloraButton(
                text = stringResource(R.string.action_grant_permission),
                onClick = onGrantPermission,
            )
        }
    }
}

@Composable
private fun ReadinessRow(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ReadinessDot(color = color, contentDescription = label)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun SubscriptionStatusCard(
    uiState: DashboardUiState,
    subscriptionId: Int,
    label: String,
) {
    val statuses = uiState.observed.filter { it.subscriptionId == subscriptionId }
    CalloraCard {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (statuses.isEmpty()) {
            Text(
                stringResource(R.string.state_UNKNOWN),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            statuses.forEach { status ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        LabelHelpers.conditionLabel(status.condition),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    StatusChip(status.state, LabelHelpers.stateLabel(status.state))
                }
            }
        }
    }
}
