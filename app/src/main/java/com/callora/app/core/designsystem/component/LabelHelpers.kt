package com.callora.app.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.callora.app.R
import com.callora.app.core.model.BackendType
import com.callora.app.core.model.CapabilityStatus
import com.callora.app.core.model.ForwardingCondition
import com.callora.app.core.model.ForwardingState

/** Central mapping from typed models to localized resource strings. */
object LabelHelpers {

    @Composable
    fun stateLabel(state: ForwardingState): String = when (state) {
        ForwardingState.UNKNOWN -> stringResource(R.string.state_UNKNOWN)
        ForwardingState.INACTIVE -> stringResource(R.string.state_INACTIVE)
        ForwardingState.PENDING_ACTIVATION -> stringResource(R.string.state_PENDING_ACTIVATION)
        ForwardingState.ACTIVE_VERIFIED -> stringResource(R.string.state_ACTIVE_VERIFIED)
        ForwardingState.ACTIVE_UNVERIFIED -> stringResource(R.string.state_ACTIVE_UNVERIFIED)
        ForwardingState.PENDING_DEACTIVATION -> stringResource(R.string.state_PENDING_DEACTIVATION)
        ForwardingState.ERROR -> stringResource(R.string.state_ERROR)
        ForwardingState.UNSUPPORTED -> stringResource(R.string.state_UNSUPPORTED)
        ForwardingState.MANUAL_ACTION_REQUIRED -> stringResource(R.string.state_MANUAL_ACTION_REQUIRED)
    }

    @Composable
    fun conditionLabel(condition: ForwardingCondition): String = when (condition) {
        ForwardingCondition.CFU -> stringResource(R.string.condition_CFU)
        ForwardingCondition.CFB -> stringResource(R.string.condition_CFB)
        ForwardingCondition.CFNRY -> stringResource(R.string.condition_CFNRY)
        ForwardingCondition.CFNRC -> stringResource(R.string.condition_CFNRC)
    }

    @Composable
    fun conditionDescription(condition: ForwardingCondition): String = when (condition) {
        ForwardingCondition.CFU -> stringResource(R.string.condition_CFU_desc)
        ForwardingCondition.CFB -> stringResource(R.string.condition_CFB_desc)
        ForwardingCondition.CFNRY -> stringResource(R.string.condition_CFNRY_desc)
        ForwardingCondition.CFNRC -> stringResource(R.string.condition_CFNRC_desc)
    }

    @Composable
    fun backendLabel(type: BackendType): String = when (type) {
        BackendType.OFFICIAL_ANDROID_API -> stringResource(R.string.backend_OFFICIAL_ANDROID_API)
        BackendType.USER_CONFIRMED_MMI -> stringResource(R.string.backend_USER_CONFIRMED_MMI)
        BackendType.MANUAL_GUIDE -> stringResource(R.string.backend_MANUAL_GUIDE)
        BackendType.UNSUPPORTED -> stringResource(R.string.backend_UNSUPPORTED)
    }

    @Composable
    fun capabilityLabel(status: CapabilityStatus): String = when (status) {
        CapabilityStatus.SUPPORTED_OFFICIAL -> stringResource(R.string.capability_status_SUPPORTED_OFFICIAL)
        CapabilityStatus.SUPPORTED_DOCUMENTED -> stringResource(R.string.capability_status_SUPPORTED_DOCUMENTED)
        CapabilityStatus.PARTIALLY_SUPPORTED -> stringResource(R.string.capability_status_PARTIALLY_SUPPORTED)
        CapabilityStatus.NOT_VERIFIED -> stringResource(R.string.capability_status_NOT_VERIFIED)
        CapabilityStatus.UNSUPPORTED -> stringResource(R.string.capability_status_UNSUPPORTED)
    }
}
