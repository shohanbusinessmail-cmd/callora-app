package com.callora.app.feature.sims

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.callora.app.R
import com.callora.app.core.designsystem.component.InfoBanner
import com.callora.app.core.designsystem.component.LabelHelpers
import com.callora.app.core.model.DestinationValidation
import com.callora.app.core.model.ForwardingOperation

@Composable
fun DestinationEditorDialog(
    initial: String,
    onValidate: (String) -> DestinationValidation,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by remember { mutableStateOf(initial) }
    var errorRes by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.destination_dialog_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        errorRes = null
                    },
                    label = { Text(stringResource(R.string.destination_label_hint)) },
                    isError = errorRes != null,
                    supportingText = if (errorRes != null) {
                        { Text(stringResource(errorRes!!)) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when (val result = onValidate(input)) {
                    is DestinationValidation.Valid -> onSave(input)
                    is DestinationValidation.Invalid -> errorRes = invalidReasonRes(result.reason)
                }
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
fun ConfirmMmiDialog(
    mmi: String,
    operation: ForwardingOperation,
    destinationText: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val conditionLabel = LabelHelpers.conditionLabel(operation.condition)
    val body = if (destinationText != null) {
        stringResource(
            R.string.confirm_body,
            operation.subscriptionId,
            mmi,
            conditionLabel,
            destinationText,
        )
    } else {
        stringResource(
            R.string.confirm_body_no_dest,
            operation.subscriptionId,
            mmi,
            conditionLabel,
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.confirm_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(body, style = MaterialTheme.typography.bodyMedium)
                InfoBanner(text = stringResource(R.string.confirm_charge_warning), warning = true)
                InfoBanner(text = stringResource(R.string.confirm_sim_warning), warning = true)
                Text(
                    stringResource(R.string.mmi_handoff_explanation),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.confirm_launch_dialer))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
fun ManualStepsDialog(
    steps: List<String>,
    queryMmi: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.manual_steps_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                steps.forEachIndexed { index, step ->
                    Text("${index + 1}. $step", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "${stringResource(R.string.query_mmi_label)}: $queryMmi",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        },
    )
}

@Composable
fun ResultQuestionDialog(
    onResult: (Boolean, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var choice by remember { mutableStateOf<Boolean?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.result_title)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.result_question),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = choice == true, onClick = { choice = true })
                    Text(stringResource(R.string.result_accepted))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = choice == false, onClick = { choice = false })
                    Text(stringResource(R.string.result_rejected))
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = choice != null,
                onClick = { onResult(choice == true, null) },
            ) { Text(stringResource(R.string.action_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

fun invalidReasonRes(reason: DestinationValidation.InvalidReason): Int = when (reason) {
    DestinationValidation.InvalidReason.EMPTY -> R.string.destination_invalid_EMPTY
    DestinationValidation.InvalidReason.TOO_SHORT -> R.string.destination_invalid_TOO_SHORT
    DestinationValidation.InvalidReason.TOO_LONG -> R.string.destination_invalid_TOO_LONG
    DestinationValidation.InvalidReason.NOT_A_NUMBER -> R.string.destination_invalid_NOT_A_NUMBER
    DestinationValidation.InvalidReason.WRONG_REGION -> R.string.destination_invalid_WRONG_REGION
}
