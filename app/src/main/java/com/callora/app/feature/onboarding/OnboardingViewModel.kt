package com.callora.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.callora.app.CalloraApplication
import com.callora.app.CalloraViewModel
import kotlinx.coroutines.launch

class OnboardingViewModel(app: CalloraApplication) : CalloraViewModel(app) {

    fun completeOnboarding() {
        viewModelScope.launch {
            container.settingsRepository.setOnboardingCompleted(true)
        }
    }

    companion object {
        fun factory(app: CalloraApplication): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    OnboardingViewModel(app) as T
            }
    }
}
