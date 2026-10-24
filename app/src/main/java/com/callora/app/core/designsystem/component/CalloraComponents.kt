package com.callora.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.callora.app.core.designsystem.theme.StatusError
import com.callora.app.core.designsystem.theme.StatusInfo
import com.callora.app.core.designsystem.theme.StatusNeutral
import com.callora.app.core.designsystem.theme.StatusSuccess
import com.callora.app.core.designsystem.theme.StatusWarning
import com.callora.app.core.model.ForwardingState

@Composable
fun CalloraCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), content = { content() })
    }
}

@Composable
fun PrimaryCalloraButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryCalloraButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(vertical = 8.dp),
    )
}

@Composable
fun StatusChip(
    state: ForwardingState,
    label: String,
    modifier: Modifier = Modifier,
) {
    val (color, icon) = stateVisuals(state)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .semantics { contentDescription = label }
            .padding(vertical = 2.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .padding(2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

private data class StateVisuals(val color: androidx.compose.ui.graphics.Color, val icon: ImageVector)

@Composable
private fun stateVisuals(state: ForwardingState): StateVisuals = when (state) {
    ForwardingState.ACTIVE_VERIFIED -> StateVisuals(StatusSuccess, Icons.Filled.CheckCircle)
    ForwardingState.ACTIVE_UNVERIFIED,
    ForwardingState.PENDING_ACTIVATION,
    ForwardingState.PENDING_DEACTIVATION,
    -> StateVisuals(StatusWarning, Icons.Filled.HourglassTop)
    ForwardingState.ERROR -> StateVisuals(StatusError, Icons.Filled.ErrorOutline)
    ForwardingState.UNSUPPORTED -> StateVisuals(StatusNeutral, Icons.Filled.HelpOutline)
    ForwardingState.MANUAL_ACTION_REQUIRED -> StateVisuals(StatusInfo, Icons.Filled.Info)
    ForwardingState.INACTIVE -> StateVisuals(StatusNeutral, Icons.Filled.HelpOutline)
    ForwardingState.UNKNOWN -> StateVisuals(StatusNeutral, Icons.Filled.HelpOutline)
}

@Composable
fun InfoBanner(
    text: String,
    modifier: Modifier = Modifier,
    warning: Boolean = false,
) {
    val container = if (warning) StatusWarning.copy(alpha = 0.14f) else StatusInfo.copy(alpha = 0.12f)
    val content = if (warning) StatusWarning else StatusInfo
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(
                if (warning) Icons.Filled.ErrorOutline else Icons.Filled.Info,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun LabeledSwitchRow(
    label: String,
    description: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun LoadingView(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    body: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            PrimaryCalloraButton(text = actionLabel, onClick = onAction)
        }
    }
}

@Composable
fun ErrorView(
    message: String,
    actionLabel: String?,
    onAction: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = StatusError,
            modifier = Modifier.size(44.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            SecondaryCalloraButton(text = actionLabel, onClick = onAction)
        }
    }
}

/** Small circular badge used for readiness dots; text alternative is mandatory. */
@Composable
fun ReadinessDot(color: androidx.compose.ui.graphics.Color, contentDescription: String) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .semantics { this.contentDescription = contentDescription }
            .padding(2.dp),
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(10.dp)) {
            drawCircle(color = color)
        }
    }
}
