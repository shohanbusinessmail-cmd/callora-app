package com.callora.app.feature.sims

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callora.app.R
import com.callora.app.core.designsystem.component.CalloraCard
import com.callora.app.core.designsystem.component.InfoBanner
import com.callora.app.core.designsystem.component.LabelHelpers
import com.callora.app.core.designsystem.component.PrimaryCalloraButton
import com.callora.app.core.designsystem.component.SecondaryCalloraButton
import com.callora.app.core.designsystem.component.SectionTitle
import com.callora.app.core.designsystem.component.StatusChip
import com.callora.app.core.model.CapabilityStatus
import com.callora.app.core.model.ForwardingCondition
import com.callora.app.core.model.ForwardingState
import com.callora.app.core.model.OperationAction
import com.callora.app.core.telephony.OperatorCapabilityRegistry
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimDetailScreen(
    viewModel: SimsViewModel,
    subscriptionId: Int,
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dialog by viewModel.dialog.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(subscriptionId) {
        viewModel.messages.collect { message ->
            val text = when (message) {
                is SimsMessage.InvalidDestination -> context.getString(invalidReasonRes(message.reason))
                is SimsMessage.Text -> message.text
            }
            scope.launch { snackbarHostState.showSnackbar(text) }
        }
    }

    val subscription = uiState.subscriptions.firstOrNull { it.subscriptionId == subscriptionId }
    val config = uiState.configFor(subscriptionId)
    val observed = uiState.observedFor(subscriptionId).associateBy { it.condition }
    val capability = subscription?.let {
        OperatorCapabilityRegistry.forOperator(it.operator) ?: OperatorCapabilityRegistry.unknown()
    }
    val destination = config?.destinationId?.let { id -> uiState.destinations.firstOrNull { it.id == id } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(subscription?.stableLabel ?: stringResource(R.string.sims_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (subscription == null) {
            InfoBanner(
                text = stringResource(R.string.sims_empty_body),
                warning = true,
                modifier = Modifier.padding(padding).padding(16.dp),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                CalloraCard {
                    Text(
                        stringResource(R.string.sim_detail_slot, subscription.slotIndex + 1),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "${stringResource(R.string.sim_detail_carrier)}: ${subscription.operator.displayName}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "${stringResource(R.string.sim_detail_backend)}: ${
                            LabelHelpers.backendLabel(com.callora.app.core.model.BackendType.USER_CONFIRMED_MMI)
                        }",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            item {
                CalloraCard {
                    SectionTitle(stringResource(R.string.sim_destination_section))
                    Text(
                        destination?.label ?: stringResource(R.string.sim_destination_none),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(Modifier.height(8.dp))
                    PrimaryCalloraButton(
                        text = stringResource(R.string.sim_add_destination),
                        onClick = { viewModel.openDestinationEditor(subscriptionId) },
                    )
                }
            }

            if (capability != null) {
                item {
                    CalloraCard {
                        SectionTitle(stringResource(R.string.sim_capability_section))
                        capability.conditions.values.forEach { cc ->
                            Row(
                                modifier = Modifier.fillMaxSize().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(LabelHelpers.conditionLabel(cc.condition), style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    LabelHelpers.capabilityLabel(cc.status),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (cc.status == CapabilityStatus.NOT_VERIFIED) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                )
                            }
                        }
                        if (capability.conditions.values.any { it.status == CapabilityStatus.NOT_VERIFIED }) {
                            Spacer(Modifier.height(8.dp))
                            InfoBanner(text = stringResource(R.string.capability_warning), warning = true)
                        }
                    }
                }
            }

            item { SectionTitle(stringResource(R.string.sim_conditions_section)) }

            items(ForwardingCondition.DISPLAY_ORDER) { condition ->
                val enabled = config?.enabledConditionsCsv
                    ?.split(",")
                    ?.contains(condition.name) == true
                val status = observed[condition]
                ConditionCard(
                    condition = condition,
                    desiredEnabled = enabled,
                    observedState = status?.state ?: ForwardingState.UNKNOWN,
                    onToggleDesired = { checked ->
                        viewModel.toggleCondition(subscriptionId, condition, checked)
                    },
                    onActivate = {
                        viewModel.requestOperation(subscriptionId, condition, OperationAction.ACTIVATE)
                    },
                    onDeactivate = {
                        viewModel.requestOperation(subscriptionId, condition, OperationAction.DEACTIVATE)
                    },
                    onQuery = {
                        viewModel.requestOperation(subscriptionId, condition, OperationAction.QUERY)
                    },
                )
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }

    when (val current = dialog) {
        is ForwardingDialog.DestinationEditor -> DestinationEditorDialog(
            initial = current.initial,
            onValidate = viewModel::validateDestination,
            onSave = { input ->
                viewModel.saveDestination(subscriptionId, input) {
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.destination_saved)) }
                }
            },
            onDismiss = viewModel::dismissDialog,
        )
        is ForwardingDialog.Confirm -> ConfirmMmiDialog(
            mmi = current.handoff.mmi,
            operation = current.operation,
            destinationText = current.destinationText,
            onConfirm = viewModel::launchConfirmedHandoff,
            onDismiss = viewModel::dismissDialog,
        )
        is ForwardingDialog.Manual -> ManualStepsDialog(
            steps = current.steps,
            queryMmi = current.queryMmi,
            onDismiss = viewModel::dismissDialog,
        )
        is ForwardingDialog.ResultQuestion -> ResultQuestionDialog(
            onResult = viewModel::recordUserResult,
            onDismiss = viewModel::dismissDialog,
        )
        null -> Unit
    }
}

@Composable
private fun ConditionCard(
    condition: ForwardingCondition,
    desiredEnabled: Boolean,
    observedState: ForwardingState,
    onToggleDesired: (Boolean) -> Unit,
    onActivate: () -> Unit,
    onDeactivate: () -> Unit,
    onQuery: () -> Unit,
) {
    CalloraCard {
        Row(horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(LabelHelpers.conditionLabel(condition), style = MaterialTheme.typography.titleMedium)
                Text(
                    LabelHelpers.conditionDescription(condition),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusChip(observedState, LabelHelpers.stateLabel(observedState))
        }
        Spacer(Modifier.height(8.dp))
        androidx.compose.material3.Switch(
            checked = desiredEnabled,
            onCheckedChange = onToggleDesired,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryCalloraButton(
                text = stringResource(R.string.action_activate),
                onClick = onActivate,
                modifier = Modifier.weight(1f),
            )
            SecondaryCalloraButton(
                text = stringResource(R.string.action_deactivate),
                onClick = onDeactivate,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        SecondaryCalloraButton(
            text = stringResource(R.string.action_query),
            onClick = onQuery,
        )
    }
}
