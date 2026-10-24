package com.callora.app.feature.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.callora.app.CalloraApplication
import com.callora.app.CalloraViewModel
import com.callora.app.core.model.AutomationPolicy
import com.callora.app.core.model.ForwardingCondition
import com.callora.app.core.model.ForwardingDestination
import com.callora.app.core.model.ForwardingProfile
import com.callora.app.core.model.ProfileType
import com.callora.app.core.model.ScheduleModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfilesUiState(
    val profiles: List<ForwardingProfile> = emptyList(),
    val destinations: List<ForwardingDestination> = emptyList(),
    val schedules: List<ScheduleModel> = emptyList(),
    val defaultProfileId: Long? = null,
)

class ProfilesViewModel(app: CalloraApplication) : CalloraViewModel(app) {

    val uiState: StateFlow<ProfilesUiState> = combine(
        container.profileRepository.observeAll(),
        container.destinationRepository.observeAll(),
        container.scheduleRepository.observeAll(),
        container.settingsRepository.settings,
    ) { profiles, destinations, schedules, settings ->
        ProfilesUiState(
            profiles = profiles,
            destinations = destinations,
            schedules = schedules,
            defaultProfileId = settings.defaultProfileId,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfilesUiState())

    fun loadProfile(profileId: Long, onLoaded: (ForwardingProfile?) -> Unit) {
        viewModelScope.launch {
            onLoaded(if (profileId >= 0) container.profileRepository.get(profileId) else null)
        }
    }

    fun saveProfile(
        existingId: Long,
        name: String,
        type: ProfileType,
        conditions: Set<ForwardingCondition>,
        destinationId: Long?,
        policy: AutomationPolicy,
        scheduleId: Long?,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = if (existingId >= 0) container.profileRepository.get(existingId) else null
            val profile = ForwardingProfile(
                id = existing?.id ?: 0L,
                name = name.ifBlank { type.name },
                type = type,
                enabledConditions = conditions,
                destinationId = destinationId,
                automationPolicy = policy,
                scheduleId = scheduleId,
                isDefault = existing?.isDefault ?: false,
                createdAtEpochMs = existing?.createdAtEpochMs ?: now,
                updatedAtEpochMs = now,
            )
            container.profileRepository.save(profile)
            onSaved()
        }
    }

    fun setDefault(profile: ForwardingProfile) {
        viewModelScope.launch {
            container.profileRepository.observeAll().first().forEach { other ->
                if (other.isDefault && other.id != profile.id) {
                    container.profileRepository.save(other.copy(isDefault = false))
                }
            }
            container.profileRepository.save(profile.copy(isDefault = true))
            container.settingsRepository.setDefaultProfile(profile.id)
        }
    }

    fun delete(profile: ForwardingProfile) {
        viewModelScope.launch {
            container.profileRepository.delete(profile)
            if (container.settingsRepository.settings.first().defaultProfileId == profile.id) {
                container.settingsRepository.setDefaultProfile(null)
            }
        }
    }

    companion object {
        fun factory(app: CalloraApplication): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ProfilesViewModel(app) as T
            }
    }
}
