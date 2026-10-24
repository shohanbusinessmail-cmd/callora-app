package com.callora.app.feature.sims

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callora.app.R
import com.callora.app.core.designsystem.component.CalloraCard
import com.callora.app.core.designsystem.component.EmptyStateView
import com.callora.app.core.designsystem.component.LabelHelpers
import com.callora.app.core.designsystem.component.LoadingView
import com.callora.app.core.designsystem.component.PrimaryCalloraButton
import com.callora.app.core.designsystem.component.SecondaryCalloraButton
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimsScreen(
    viewModel: SimsViewModel,
    onOpenSim: (Int) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.refresh() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            val text = when (message) {
                is SimsMessage.InvalidDestination ->
                    context.getString(invalidReasonRes(message.reason))
                is SimsMessage.Text -> message.text
            }
            scope.launch { snackbarHostState.showSnackbar(text) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sims_title)) },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.action_retry))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            uiState.loading && uiState.subscriptions.isEmpty() ->
                LoadingView(stringResource(R.string.loading_generic), Modifier.padding(padding))
            uiState.subscriptions.isEmpty() -> Column(Modifier.padding(padding)) {
                EmptyStateView(
                    icon = Icons.Filled.SimCard,
                    title = stringResource(R.string.sims_empty_title),
                    body = stringResource(R.string.sims_empty_body),
                )
                PrimaryCalloraButton(
                    text = stringResource(R.string.action_grant_permission),
                    onClick = { permissionLauncher.launch(Manifest.permission.READ_PHONE_STATE) },
                    modifier = Modifier.padding(horizontal = 32.dp),
                )
            }
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.subscriptions, key = { it.subscriptionId }) { subscription ->
                    CalloraCard(modifier = Modifier.animateItem()) {
                        Text(subscription.stableLabel, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.sim_detail_slot, subscription.slotIndex + 1),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "${stringResource(R.string.sim_detail_carrier)}: ${subscription.operator.displayName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        val config = uiState.configFor(subscription.subscriptionId)
                        val enabledCount = config?.enabledConditionsCsv
                            ?.split(",")?.count { it.isNotBlank() } ?: 0
                        Text(
                            LabelHelpers.backendLabel(
                                com.callora.app.core.model.BackendType.USER_CONFIRMED_MMI,
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(8.dp))
                        SecondaryCalloraButton(
                            text = "${stringResource(R.string.sim_conditions_section)} ($enabledCount)",
                            onClick = { onOpenSim(subscription.subscriptionId) },
                        )
                    }
                }
            }
        }
    }
}
