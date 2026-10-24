package com.callora.app.core.telephony

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.callora.app.core.model.BackendType

/** Snapshot of what this device/SIM actually permits Callora to do. */
data class DeviceTelephonyCapability(
    val readPhoneStateGranted: Boolean,
    val callPhoneGranted: Boolean,
    val notificationsGranted: Boolean,
    val hasCarrierPrivileges: Boolean,
    val networkConnected: Boolean,
) {
    /**
     * The official TelephonyManager call-forwarding API is restricted to
     * carrier-privileged / system applications on every Android version that
     * exposes it. A normal Play-installed app therefore never gets this backend.
     */
    val officialApiAvailable: Boolean get() = hasCarrierPrivileges

    val mmiHandoffAvailable: Boolean get() = true

    val recommendedBackend: BackendType
        get() = when {
            officialApiAvailable -> BackendType.OFFICIAL_ANDROID_API
            mmiHandoffAvailable -> BackendType.USER_CONFIRMED_MMI
            else -> BackendType.MANUAL_GUIDE
        }
}

class TelephonyCapabilityProvider(private val context: Context) {

    private fun granted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun current(): DeviceTelephonyCapability {
        val telephony = ContextCompat.getSystemService(context, TelephonyManager::class.java)
        val privileges = runCatching { telephony?.hasCarrierPrivileges() == true }.getOrDefault(false)
        return DeviceTelephonyCapability(
            readPhoneStateGranted = granted(Manifest.permission.READ_PHONE_STATE),
            callPhoneGranted = granted(Manifest.permission.CALL_PHONE),
            notificationsGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                granted(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                true
            },
            hasCarrierPrivileges = privileges,
            networkConnected = isNetworkConnected(),
        )
    }

    private fun isNetworkConnected(): Boolean {
        val cm = ContextCompat.getSystemService(context, ConnectivityManager::class.java) ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED)
    }
}
