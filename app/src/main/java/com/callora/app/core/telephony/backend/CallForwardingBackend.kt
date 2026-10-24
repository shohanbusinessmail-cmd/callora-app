package com.callora.app.core.telephony.backend

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import com.callora.app.core.model.BackendOutcome
import com.callora.app.core.model.BackendType
import com.callora.app.core.model.FailureReason
import com.callora.app.core.model.ForwardingCondition
import com.callora.app.core.model.OperationAction
import com.callora.app.core.telephony.DeviceTelephonyCapability
import com.callora.app.core.telephony.MmiCodeBuilder

/** Everything the UI needs to render the explicit MMI confirmation screen. */
data class MmiHandOff(
    val mmi: String,
    val action: OperationAction,
    val condition: ForwardingCondition,
    val targetSubscriptionId: Int,
    val phoneAccountResolved: Boolean,
    val useActionCall: Boolean,
)

/** Manual, evidence-backed steps shown when no programmatic path exists. */
data class ManualGuide(
    val condition: ForwardingCondition,
    val steps: List<String>,
    val queryMmi: String,
)

/**
 * Abstraction over the mechanism used to change carrier-side forwarding.
 * Implementations never fake success: [BackendOutcome.Submitted] explicitly
 * means "handed off, result unconfirmed".
 */
interface CallForwardingBackend {
    val type: BackendType
    fun isAvailable(capability: DeviceTelephonyCapability): Boolean
}

/**
 * Backend A — official Android telephony API.
 *
 * The call-forwarding methods on TelephonyManager (setCallForwarding /
 * CallForwardingInfo) are guarded by carrier privileges / signature permissions
 * and are not part of the third-party SDK surface. This backend therefore
 * performs a real privilege check and reports UNAVAILABLE rather than pretending
 * an activation succeeded. A carrier-signed build could extend this class with
 * the privileged call.
 */
class OfficialApiBackend : CallForwardingBackend {
    override val type: BackendType = BackendType.OFFICIAL_ANDROID_API

    override fun isAvailable(capability: DeviceTelephonyCapability): Boolean =
        capability.officialApiAvailable

    fun evaluate(capability: DeviceTelephonyCapability): BackendOutcome.Unavailable =
        if (capability.officialApiAvailable) {
            // Privileged builds would invoke TelephonyManager.setCallForwarding here.
            BackendOutcome.Unavailable(
                FailureReason.UNSUPPORTED_API,
                "Carrier privileges detected, but no privileged API integration is packaged in this build.",
            )
        } else {
            BackendOutcome.Unavailable(
                FailureReason.UNSUPPORTED_API,
                "Android restricts programmatic call-forwarding control to carrier-privileged applications.",
            )
        }
}

/**
 * Backend B — explicit, user-confirmed standard GSM MMI handoff.
 *
 * Callora builds the standards-based MMI string, shows exactly what it will do,
 * and only then launches the system dialer. The dialer interaction and any
 * carrier response remain outside the app sandbox, so the result is recorded as
 * unconfirmed until the user reports what happened.
 */
class MmiIntentBackend(private val appContext: Context) : CallForwardingBackend {
    override val type: BackendType = BackendType.USER_CONFIRMED_MMI

    override fun isAvailable(capability: DeviceTelephonyCapability): Boolean =
        capability.mmiHandoffAvailable

    fun prepare(
        action: OperationAction,
        condition: ForwardingCondition,
        subscriptionId: Int,
        destinationE164: String?,
        noReplyTimerSeconds: Int?,
        capability: DeviceTelephonyCapability,
    ): MmiHandOff {
        val mmi = MmiCodeBuilder.buildForAction(action, condition, destinationE164, noReplyTimerSeconds)
        return MmiHandOff(
            mmi = mmi,
            action = action,
            condition = condition,
            targetSubscriptionId = subscriptionId,
            phoneAccountResolved = resolvePhoneAccount(subscriptionId) != null,
            useActionCall = capability.callPhoneGranted,
        )
    }

    /**
     * Launches the system dialer. ACTION_CALL is used only when the user granted
     * CALL_PHONE; otherwise ACTION_DIAL pre-fills the string and the user presses
     * call themselves. Returns false if no dialer can handle the intent.
     */
    fun launch(handoff: MmiHandOff): Boolean {
        val uri = Uri.fromParts("tel", handoff.mmi, null)
        val callPhoneGranted = ContextCompat.checkSelfPermission(
            appContext, Manifest.permission.CALL_PHONE,
        ) == PackageManager.PERMISSION_GRANTED
        // ACTION_CALL only with an explicit live grant; otherwise pre-fill the dialer.
        val action = if (handoff.useActionCall && callPhoneGranted) {
            Intent.ACTION_CALL
        } else {
            Intent.ACTION_DIAL
        }
        val intent = Intent(action, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (handoff.phoneAccountResolved) {
                resolvePhoneAccount(handoff.targetSubscriptionId)?.let { handle ->
                    putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle)
                }
            }
        }
        return runCatching {
            appContext.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    private fun resolvePhoneAccount(subscriptionId: Int): android.telecom.PhoneAccountHandle? {
        if (ContextCompat.checkSelfPermission(
                appContext, Manifest.permission.READ_PHONE_STATE,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        val telecom = ContextCompat.getSystemService(appContext, TelecomManager::class.java) ?: return null
        val handles = runCatching { telecom.callCapablePhoneAccounts }.getOrDefault(emptyList())
        return handles.firstOrNull { handle ->
            val id = handle.id?.removePrefix("")?.substringAfterLast('_', "")
            id == subscriptionId.toString() || handle.id?.contains(subscriptionId.toString()) == true
        }
    }
}

/** Backend C — evidence-backed manual configuration guide. */
class ManualGuideBackend : CallForwardingBackend {
    override val type: BackendType = BackendType.MANUAL_GUIDE

    override fun isAvailable(capability: DeviceTelephonyCapability): Boolean = true

    fun buildGuide(
        condition: ForwardingCondition,
        destinationE164: String?,
        operatorDisplayName: String,
    ): ManualGuide {
        val register = destinationE164?.let {
            MmiCodeBuilder.buildRegister(condition, it)
        }
        val steps = buildList {
            add("Open the system Phone/Dialer app on the $operatorDisplayName SIM.")
            if (register != null) {
                add("Dial $register to register the forwarding rule, then press call.")
            }
            add("Wait for the carrier confirmation message before leaving the dialer.")
            add("Dial ${MmiCodeBuilder.buildQuery(condition)} to interrogate the current rule.")
            add("Return to Callora and record the observed result so status stays honest.")
        }
        return ManualGuide(
            condition = condition,
            steps = steps,
            queryMmi = MmiCodeBuilder.buildQuery(condition),
        )
    }
}

/** Backend D — honest unsupported fallback that preserves configuration. */
class UnsupportedBackend : CallForwardingBackend {
    override val type: BackendType = BackendType.UNSUPPORTED

    override fun isAvailable(capability: DeviceTelephonyCapability): Boolean = false

    fun explain(): BackendOutcome.Unavailable = BackendOutcome.Unavailable(
        FailureReason.UNSUPPORTED_CARRIER_SERVICE,
        "No supported mechanism can manage this rule on this device/SIM. Your configuration is saved but no carrier change was attempted.",
    )
}
