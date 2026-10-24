package com.callora.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.callora.app.CalloraApplication
import com.callora.app.CalloraViewModel
import com.callora.app.core.datastore.AppSettings
import com.callora.app.core.localization.LocaleManager
import com.callora.app.core.model.AppLanguage
import com.callora.app.core.model.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: CalloraApplication) : CalloraViewModel(app) {

    val settings: StateFlow<AppSettings> =
        container.settingsRepository.settings
            .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    fun setLanguage(language: AppLanguage) {
        LocaleManager.apply(language)
        viewModelScope.launch { container.settingsRepository.setLanguage(language) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { container.settingsRepository.setThemeMode(mode) }
    }

    fun setNotifications(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setNotificationsEnabled(enabled) }
    }

    fun replayOnboarding() {
        viewModelScope.launch { container.settingsRepository.setOnboardingCompleted(false) }
    }

    fun clearAllData() {
        viewModelScope.launch {
            container.database.clearAllTables()
            container.settingsRepository.clearAll()
        }
    }

    companion object {
        fun factory(app: CalloraApplication): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SettingsViewModel(app) as T
            }
    }
}
