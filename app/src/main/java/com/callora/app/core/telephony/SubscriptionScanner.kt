package com.callora.app.core.telephony

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.callora.app.core.model.OperatorId
import com.callora.app.core.model.SimState
import com.callora.app.core.model.SubscriptionSnapshot

/**
 * Real subscription discovery backed by [SubscriptionManager] / [TelephonyManager].
 *
 * Android 10+ restricts ICCID/IMSI/phone-number access for third-party apps, so
 * this scanner only reads fields that are legitimately available with
 * READ_PHONE_STATE and reports [SubscriptionSnapshot.phoneNumberAvailable]
 * instead of guessing numbers.
 */
class SubscriptionScanner(private val context: Context) {

    private val subscriptionManager: SubscriptionManager?
        get() = ContextCompat.getSystemService(context, SubscriptionManager::class.java)

    private val telephonyManager: TelephonyManager?
        get() = ContextCompat.getSystemService(context, TelephonyManager::class.java)

    fun hasReadPhoneStatePermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Returns the currently visible subscriptions. An empty list is meaningful:
     * either no SIM is present, the permission was denied, or the OEM blocked
     * access — callers distinguish via [hasReadPhoneStatePermission].
     */
    fun scan(): List<SubscriptionSnapshot> {
        if (!hasReadPhoneStatePermission()) return emptyList()
        val manager = subscriptionManager ?: return emptyList()
        val infos: List<SubscriptionInfo> = runCatching {
            @Suppress("MissingPermission")
            manager.activeSubscriptionInfoList.orEmpty()
        }.getOrDefault(emptyList())

        return infos.map { info ->
            val subTelephony = telephonyManagerFor(info.subscriptionId)
            val mccMnc = runCatching { subTelephony?.simOperator }.orNull()
            val operator = OperatorId.fromMccMnc(mccMnc)
            val simState = mapSimState(runCatching { subTelephony?.simState }.getOrNull())
            SubscriptionSnapshot(
                subscriptionId = info.subscriptionId,
                displayName = info.displayName?.toString().orEmpty(),
                carrierName = info.carrierName?.toString().orEmpty(),
                operator = if (operator == OperatorId.UNKNOWN && !mccMnc.isNullOrBlank()) {
                    OperatorId.UNKNOWN
                } else {
                    operator
                },
                mccMnc = mccMnc?.takeIf { it.length >= 5 },
                slotIndex = info.simSlotIndex,
                simState = simState,
                // Only the observable SIM state is used; hidden status APIs are avoided.
                isActive = simState == SimState.READY,
                phoneNumberAvailable = hasNumber(info),
            )
        }.sortedBy { it.slotIndex }
    }

    private fun telephonyManagerFor(subscriptionId: Int): TelephonyManager? {
        val base = telephonyManager ?: return null
        // createForSubscriptionId was added in Android Q (API 29).
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return runCatching { base.createForSubscriptionId(subscriptionId) }.getOrNull()
    }

    private fun hasNumber(info: SubscriptionInfo): Boolean =
        runCatching { !info.number.isNullOrBlank() }.getOrDefault(false)

    private fun mapSimState(state: Int?): SimState = when (state) {
        null -> SimState.UNKNOWN
        TelephonyManager.SIM_STATE_ABSENT -> SimState.ABSENT
        TelephonyManager.SIM_STATE_PIN_REQUIRED -> SimState.PIN_REQUIRED
        TelephonyManager.SIM_STATE_PUK_REQUIRED -> SimState.PUK_REQUIRED
        TelephonyManager.SIM_STATE_NETWORK_LOCKED -> SimState.NETWORK_LOCKED
        TelephonyManager.SIM_STATE_READY -> SimState.READY
        // SIM_STATE_DISABLED is not part of the public SDK surface; OEM-disabled
        // SIMs fall through to the honest NOT_READY bucket.
        else -> SimState.NOT_READY
    }

    private inline fun <T> Result<T>.orNull(): T? = getOrNull()
}
