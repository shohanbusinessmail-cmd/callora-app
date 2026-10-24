package com.callora.app.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.callora.app.core.model.AppLanguage
import com.callora.app.core.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.calloraDataStore by preferencesDataStore(name = "callora_settings")

data class AppSettings(
    val onboardingCompleted: Boolean = false,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val defaultProfileId: Long? = null,
    val lastReconciliationEpochMs: Long = 0L,
)

class SettingsRepository(private val appContext: Context) {

    private object Keys {
        val ONBOARDED = booleanPreferencesKey("onboarding_completed")
        val LANGUAGE = stringPreferencesKey("language_tag")
        val THEME = stringPreferencesKey("theme_mode")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val DEFAULT_PROFILE = longPreferencesKey("default_profile_id")
        val LAST_RECONCILE = longPreferencesKey("last_reconciliation_ms")
    }

    val settings: Flow<AppSettings> = appContext.calloraDataStore.data.map { prefs ->
        AppSettings(
            onboardingCompleted = prefs[Keys.ONBOARDED] ?: false,
            language = AppLanguage.fromTag(prefs[Keys.LANGUAGE]),
            themeMode = runCatching {
                prefs[Keys.THEME]?.let { ThemeMode.valueOf(it) } ?: ThemeMode.SYSTEM
            }.getOrDefault(ThemeMode.SYSTEM),
            notificationsEnabled = prefs[Keys.NOTIFICATIONS] ?: true,
            defaultProfileId = prefs[Keys.DEFAULT_PROFILE],
            lastReconciliationEpochMs = prefs[Keys.LAST_RECONCILE] ?: 0L,
        )
    }

    suspend fun setOnboardingCompleted(completed: Boolean) = edit { it[Keys.ONBOARDED] = completed }

    suspend fun setLanguage(language: AppLanguage) = edit { it[Keys.LANGUAGE] = language.tag }

    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.THEME] = mode.name }

    suspend fun setNotificationsEnabled(enabled: Boolean) =
        edit { it[Keys.NOTIFICATIONS] = enabled }

    suspend fun setDefaultProfile(id: Long?) = edit {
        if (id == null) it.remove(Keys.DEFAULT_PROFILE) else it[Keys.DEFAULT_PROFILE] = id
    }

    suspend fun setLastReconciliation(epochMs: Long) = edit { it[Keys.LAST_RECONCILE] = epochMs }

    suspend fun clearAll() = edit { it.clear() }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        appContext.calloraDataStore.edit(block)
    }
}
