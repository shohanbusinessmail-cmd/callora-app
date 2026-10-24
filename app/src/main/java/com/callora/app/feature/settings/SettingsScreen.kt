package com.callora.app.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalLayoutApi
import androidx.compose.material3.FilterChip
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
import com.callora.app.core.designsystem.component.LabeledSwitchRow
import com.callora.app.core.designsystem.component.SectionTitle
import com.callora.app.core.designsystem.component.SecondaryCalloraButton
import com.callora.app.core.model.AppLanguage
import com.callora.app.core.model.ThemeMode

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onAbout: () -> Unit,
    onTroubleshooting: () -> Unit,
    onReplayOnboarding: () -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CalloraCard {
                SectionTitle(stringResource(R.string.settings_language))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = settings.language == AppLanguage.ENGLISH,
                        onClick = { viewModel.setLanguage(AppLanguage.ENGLISH) },
                        label = { Text(stringResource(R.string.language_english)) },
                    )
                    FilterChip(
                        selected = settings.language == AppLanguage.BENGALI,
                        onClick = { viewModel.setLanguage(AppLanguage.BENGALI) },
                        label = { Text(stringResource(R.string.language_bengali)) },
                    )
                }

                Spacer(Modifier.height(8.dp))
                SectionTitle(stringResource(R.string.settings_theme))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            label = { Text(themeLabel(mode)) },
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                LabeledSwitchRow(
                    label = stringResource(R.string.settings_notifications),
                    description = null,
                    checked = settings.notificationsEnabled,
                    onCheckedChange = viewModel::setNotifications,
                )
            }

            SecondaryCalloraButton(
                text = stringResource(R.string.settings_troubleshooting),
                onClick = onTroubleshooting,
                modifier = Modifier.fillMaxWidth(),
            )
            SecondaryCalloraButton(
                text = stringResource(R.string.settings_about),
                onClick = onAbout,
                modifier = Modifier.fillMaxWidth(),
            )
            SecondaryCalloraButton(
                text = stringResource(R.string.settings_replay_onboarding),
                onClick = {
                    viewModel.replayOnboarding()
                    onReplayOnboarding()
                },
                modifier = Modifier.fillMaxWidth(),
            )
            SecondaryCalloraButton(
                text = stringResource(R.string.settings_clear_data),
                onClick = { confirmClear = true },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                stringResource(R.string.onboarding_privacy_body),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.settings_clear_data)) },
            text = { Text(stringResource(R.string.settings_clear_data_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllData()
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
private fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> stringResource(R.string.theme_SYSTEM)
    ThemeMode.LIGHT -> stringResource(R.string.theme_LIGHT)
    ThemeMode.DARK -> stringResource(R.string.theme_DARK)
}
