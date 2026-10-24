package com.callora.app.feature.profiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callora.app.R
import com.callora.app.core.designsystem.component.LabelHelpers
import com.callora.app.core.designsystem.component.PrimaryCalloraButton
import com.callora.app.core.designsystem.component.SectionTitle
import com.callora.app.core.model.AutomationPolicy
import com.callora.app.core.model.ForwardingCondition
import com.callora.app.core.model.ProfileType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileEditScreen(
    viewModel: ProfilesViewModel,
    profileId: Long,
    onSaved: () -> Unit = {},
    onBack: () -> Unit = onSaved,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(ProfileType.PERSONAL) }
    var policy by remember { mutableStateOf(AutomationPolicy.ALWAYS_PROTECT) }
    var conditions by remember { mutableStateOf(setOf(ForwardingCondition.CFNRC)) }
    var destinationId by remember { mutableStateOf<Long?>(null) }
    var scheduleId by remember { mutableStateOf<Long?>(null) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(profileId) {
        viewModel.loadProfile(profileId) { profile ->
            if (profile != null) {
                name = profile.name
                type = profile.type
                policy = profile.automationPolicy
                conditions = profile.enabledConditions
                destinationId = profile.destinationId
                scheduleId = profile.scheduleId
            }
            loaded = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (profileId >= 0) R.string.profile_edit else R.string.profile_create,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.profile_name_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionTitle(stringResource(R.string.profile_edit))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileType.entries.forEach { candidate ->
                    FilterChip(
                        selected = type == candidate,
                        onClick = { type = candidate },
                        label = { Text(profileTypeText(candidate)) },
                    )
                }
            }

            SectionTitle(stringResource(R.string.settings_automation))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AutomationPolicy.entries.forEach { candidate ->
                    FilterChip(
                        selected = policy == candidate,
                        onClick = { policy = candidate },
                        label = { Text(policyText(candidate)) },
                    )
                }
            }

            SectionTitle(stringResource(R.string.sim_conditions_section))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ForwardingCondition.DISPLAY_ORDER.forEach { candidate ->
                    FilterChip(
                        selected = candidate in conditions,
                        onClick = {
                            conditions = if (candidate in conditions) {
                                conditions - candidate
                            } else {
                                conditions + candidate
                            }
                        },
                        label = { Text(LabelHelpers.conditionLabel(candidate)) },
                    )
                }
            }

            DestinationDropdown(
                options = uiState.destinations,
                selectedId = destinationId,
                onSelect = { destinationId = it },
            )
            ScheduleDropdown(
                options = uiState.schedules.map { it.id to it.name },
                selectedId = scheduleId,
                onSelect = { scheduleId = it },
            )

            Spacer(Modifier.height(8.dp))
            PrimaryCalloraButton(
                text = stringResource(R.string.action_save),
                onClick = {
                    viewModel.saveProfile(
                        existingId = profileId,
                        name = name,
                        type = type,
                        conditions = conditions,
                        destinationId = destinationId,
                        policy = policy,
                        scheduleId = scheduleId,
                        onSaved = onSaved,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DestinationDropdown(
    options: List<com.callora.app.core.model.ForwardingDestination>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = options.firstOrNull { it.id == selectedId }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.label ?: stringResource(R.string.sim_destination_none),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.sim_destination_section)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxSize()
                .menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.sim_destination_none)) },
                onClick = { onSelect(null); expanded = false },
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = { onSelect(option.id); expanded = false },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleDropdown(
    options: List<Pair<Long, String>>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = options.firstOrNull { it.first == selectedId }?.second ?: "—",
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.scheduling_title)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxSize()
                .menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("—") }, onClick = { onSelect(null); expanded = false })
            options.forEach { (id, label) ->
                DropdownMenuItem(text = { Text(label) }, onClick = { onSelect(id); expanded = false })
            }
        }
    }
}

@Composable
private fun profileTypeText(type: ProfileType): String = when (type) {
    ProfileType.PERSONAL -> stringResource(R.string.profile_type_PERSONAL)
    ProfileType.OFFICE -> stringResource(R.string.profile_type_OFFICE)
    ProfileType.CUSTOM -> stringResource(R.string.profile_type_CUSTOM)
    ProfileType.TEMPORARY -> stringResource(R.string.profile_type_TEMPORARY)
}

@Composable
private fun policyText(policy: AutomationPolicy): String = when (policy) {
    AutomationPolicy.ALWAYS_PROTECT -> stringResource(R.string.policy_ALWAYS_PROTECT)
    AutomationPolicy.TEMPORARY -> stringResource(R.string.policy_TEMPORARY)
    AutomationPolicy.MANUAL -> stringResource(R.string.policy_MANUAL)
    AutomationPolicy.CUSTOM -> stringResource(R.string.policy_CUSTOM)
}
