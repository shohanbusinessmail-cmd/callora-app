package com.callora.app.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.callora.app.CalloraApplication
import com.callora.app.CalloraViewModel
import com.callora.app.core.database.SubscriptionConfigEntity
import com.callora.app.core.datastore.AppSettings
import com.callora.app.core.model.ForwardingOperation
import com.callora.app.core.model.HistoryEvent
import com.callora.app.core.model.ObservedForwardingStatus
import com.callora.app.core.model.SubscriptionSnapshot
import com.callora.app.core.telephony.DeviceTelephonyCapability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

data class DashboardUiState(
    val loading: Boolean = true,
    val capability: DeviceTelephonyCapability? = null,
    val subscriptions: List<SubscriptionSnapshot> = emptyList(),
    val configs: List<SubscriptionConfigEntity> = emptyList(),
    val observed: List<ObservedForwardingStatus> = emptyList(),
    val pendingOperations: List<ForwardingOperation> = emptyList(),
    val recentHistory: List<HistoryEvent> = emptyList(),
    val settings: AppSettings = AppSettings(),
)

class DashboardViewModel(app: CalloraApplication) : CalloraViewModel(app) {

    private val subscriptions = MutableStateFlow<List<SubscriptionSnapshot>>(emptyList())
    private val capability = MutableStateFlow<DeviceTelephonyCapability?>(null)
    private val loading = MutableStateFlow(true)

    val uiState: StateFlow<DashboardUiState> = combine(
        subscriptions,
        capability,
        container.subscriptionConfigRepository.observeAll(),
        container.observedStatusRepository.observeAll(),
        container.operationRepository.observeRecent(20),
    ) { subs, caps, configs, observed, operations ->
        Quints(subs, caps, configs, observed, operations)
    }.combine(
        combine(
            container.historyRepository.observeRecent(3),
            container.settingsRepository.settings,
        ) { history, settings -> history to settings },
    ) { quints, pair ->
        DashboardUiState(
            loading = loading.value,
            capability = quints.capability,
            subscriptions = quints.subscriptions,
            configs = quints.configs,
            observed = quints.observed,
            pendingOperations = quints.operations.filter { it.isInFlight },
            recentHistory = pair.first,
            settings = pair.second,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            loading.value = true
            runCatching {
                val caps = container.capabilityProvider.current()
                val subs = container.subscriptionScanner.scan()
                capability.value = caps
                subscriptions.value = subs
                container.subscriptionConfigRepository
                    .reconcileWithVisibleSubscriptions(subs, System.currentTimeMillis())
            }
            loading.value = false
        }
    }

    private data class Quints(
        val subscriptions: List<SubscriptionSnapshot>,
        val capability: DeviceTelephonyCapability?,
        val configs: List<SubscriptionConfigEntity>,
        val observed: List<ObservedForwardingStatus>,
        val operations: List<ForwardingOperation>,
    )

    companion object {
        fun factory(app: CalloraApplication): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    DashboardViewModel(app) as T
            }
    }
}
