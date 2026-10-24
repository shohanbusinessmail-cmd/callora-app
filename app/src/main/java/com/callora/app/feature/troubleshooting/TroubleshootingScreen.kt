package com.callora.app.feature.troubleshooting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.callora.app.R
import com.callora.app.core.designsystem.component.CalloraCard
import com.callora.app.core.designsystem.component.SectionTitle

private data class TroubleEntry(val titleRes: Int, val bodyRes: Int)

private val TROUBLE_ENTRIES = listOf(
    TroubleEntry(R.string.trouble_sim_unavailable_title, R.string.trouble_sim_unavailable_body),
    TroubleEntry(R.string.trouble_permission_title, R.string.trouble_permission_body),
    TroubleEntry(R.string.trouble_network_title, R.string.trouble_network_body),
    TroubleEntry(R.string.trouble_unsupported_service_title, R.string.trouble_unsupported_service_body),
    TroubleEntry(R.string.trouble_carrier_rejection_title, R.string.trouble_carrier_rejection_body),
    TroubleEntry(R.string.trouble_mmi_title, R.string.trouble_mmi_body),
    TroubleEntry(R.string.trouble_unknown_state_title, R.string.trouble_unknown_state_body),
    TroubleEntry(R.string.trouble_retry_title, R.string.trouble_retry_body),
    TroubleEntry(R.string.trouble_conflict_title, R.string.trouble_conflict_body),
    TroubleEntry(R.string.trouble_api_title, R.string.trouble_api_body),
    TroubleEntry(R.string.trouble_destination_title, R.string.trouble_destination_body),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TroubleshootingScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.troubleshooting_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(TROUBLE_ENTRIES) { entry ->
                CalloraCard {
                    SectionTitle(stringResource(entry.titleRes))
                    Text(
                        stringResource(entry.bodyRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
