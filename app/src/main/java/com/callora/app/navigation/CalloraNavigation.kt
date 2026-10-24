package com.callora.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val ONBOARDING = "onboarding"
    const val DASHBOARD = "dashboard"
    const val SIMS = "sims"
    const val SIM_DETAIL = "sim_detail/{subId}"
    fun simDetail(subscriptionId: Int): String = "sim_detail/$subscriptionId"
    const val PROFILES = "profiles"
    const val PROFILE_EDIT = "profile_edit/{profileId}"
    fun profileEdit(profileId: Long): String = "profile_edit/$profileId"
    const val SCHEDULING = "scheduling"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val TROUBLESHOOTING = "troubleshooting"
}

enum class TopLevelDestination(
    val route: String,
    val icon: ImageVector,
    val labelRes: Int,
) {
    HOME(Routes.DASHBOARD, Icons.Filled.Home, com.callora.app.R.string.nav_home),
    SIMS(Routes.SIMS, Icons.Filled.SimCard, com.callora.app.R.string.nav_sims),
    PROFILES(Routes.PROFILES, Icons.Filled.SwitchAccount, com.callora.app.R.string.nav_profiles),
    HISTORY(Routes.HISTORY, Icons.Filled.History, com.callora.app.R.string.nav_history),
    SETTINGS(Routes.SETTINGS, Icons.Filled.Settings, com.callora.app.R.string.nav_settings),
}
