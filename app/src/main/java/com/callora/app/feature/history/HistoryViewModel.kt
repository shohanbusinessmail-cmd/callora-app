package com.callora.app.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.callora.app.CalloraApplication
import com.callora.app.CalloraViewModel
import com.callora.app.core.model.HistoryEvent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(app: CalloraApplication) : CalloraViewModel(app) {

    val events: StateFlow<List<HistoryEvent>> =
        container.historyRepository.observeRecent(200)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun clear() {
        viewModelScope.launch { container.historyRepository.clear() }
    }

    companion object {
        fun factory(app: CalloraApplication): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    HistoryViewModel(app) as T
            }
    }
}
