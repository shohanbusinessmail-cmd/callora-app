package com.callora.app.feature.sims

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.callora.app.CalloraApplication
import com.callora.app.CalloraViewModel
import com.callora.app.core.database.SubscriptionConfigEntity
import com.callora.app.core.model.DestinationValidation
import com.callora.app.core.model.ForwardingCondition
import com.callora.app.core.model.ForwardingDestination
import com.callora.app.core.model.ForwardingOperation
import com.callora.app.core.model.ObservedForwardingStatus
import com.callora.app.core.model.OperationAction
import com.callora.app.core.model.SubscriptionSnapshot
import com.callora.app.core.telephony.OperationRequestResult
import com.callora.app.core.telephony.backend.MmiHandOff
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

data class SimsUiState(
    val loading: Boolean = true,
    val subscriptions: List<SubscriptionSnapshot> = emptyList(),
    val configs: List<SubscriptionConfigEntity> = emptyList(),
    val destinations: List<ForwardingDestination> = emptyList(),
    val observed: List<ObservedForwardingStatus> = emptyList(),
) {
    fun configFor(subscriptionId: Int): SubscriptionConfigEntity? =
        configs.firstOrNull { it.subscriptionId == subscriptionId }

    fun observedFor(subscriptionId: Int): List<ObservedForwardingStatus> =
        observed.filter { it.subscriptionId == subscriptionId }
}

sealed interface SimsMessage {
    data class InvalidDestination(val reason: DestinationValidation.InvalidReason) : SimsMessage
    data class Text(val text: String) : SimsMessage
}

sealed interface ForwardingDialog {
    data class DestinationEditor(val subscriptionId: Int, val initial: String = "") : ForwardingDialog
    data class Confirm(
        val operation: ForwardingOperation,
        val handoff: MmiHandOff,
        val condition: ForwardingCondition,
        val destinationText: String?,
    ) : ForwardingDialog

    data class Manual(
        val operation: ForwardingOperation,
        val steps: List<String>,
        val queryMmi: String,
    ) : ForwardingDialog

    data class ResultQuestion(val operation: ForwardingOperation) : ForwardingDialog
}

class SimsViewModel(app: CalloraApplication) : CalloraViewModel(app) {

    private val subscriptions = MutableStateFlow<List<SubscriptionSnapshot>>(emptyList())
    private val loading = MutableStateFlow(true)
    private val dialogInternal = MutableStateFlow<ForwardingDialog?>(null)
    val dialog: StateFlow<ForwardingDialog?> = dialogInternal

    private val _messages = Channel<SimsMessage>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    val uiState: StateFlow<SimsUiState> = combine(
        subscriptions,
        container.subscriptionConfigRepository.observeAll(),
        container.destinationRepository.observeAll(),
        container.observedStatusRepository.observeAll(),
    ) { subs, configs, destinations, observed ->
        SimsUiState(
            loading = loading.value,
            subscriptions = subs,
            configs = configs,
            destinations = destinations,
            observed = observed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SimsUiState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            loading.value = true
            runCatching {
                val subs = container.subscriptionScanner.scan()
                subscriptions.value = subs
                container.subscriptionConfigRepository
                    .reconcileWithVisibleSubscriptions(subs, System.currentTimeMillis())
            }
            loading.value = false
        }
    }

    fun dismissDialog() {
        dialogInternal.value = null
    }

    fun openDestinationEditor(subscriptionId: Int) {
        dialogInternal.value = ForwardingDialog.DestinationEditor(subscriptionId)
    }

    fun validateDestination(input: String): DestinationValidation =
        container.phoneNumberNormalizer.validate(input)

    fun saveDestination(subscriptionId: Int, rawInput: String, onSaved: () -> Unit) {
        viewModelScope.launch {
            when (val validation = container.phoneNumberNormalizer.validate(rawInput)) {
                is DestinationValidation.Invalid -> {
                    _messages.send(SimsMessage.InvalidDestination(validation.reason))
                    return@launch
                }
                is DestinationValidation.Valid -> {
                    val now = System.currentTimeMillis()
                    val destinationId = container.destinationRepository.save(
                        ForwardingDestination(
                            e164 = validation.e164,
                            label = validation.formatted,
                            rawInput = rawInput,
                            createdAtEpochMs = now,
                        ),
                    )
                    val existing = container.subscriptionConfigRepository.get(subscriptionId)
                    val snapshot = subscriptions.value.firstOrNull { it.subscriptionId == subscriptionId }
                    container.subscriptionConfigRepository.upsert(
                        (existing ?: defaultConfig(subscriptionId, snapshot))
                            .copy(destinationId = destinationId),
                    )
                    dialogInternal.value = null
                    onSaved()
                }
            }
        }
    }

    fun toggleCondition(subscriptionId: Int, condition: ForwardingCondition, enabled: Boolean) {
        viewModelScope.launch {
            val snapshot = subscriptions.value.firstOrNull { it.subscriptionId == subscriptionId }
            val existing = container.subscriptionConfigRepository.get(subscriptionId)
                ?: defaultConfig(subscriptionId, snapshot)
            val current = existing.enabledConditionsCsv.split(",").filter { it.isNotBlank() }.toMutableSet()
            if (enabled) current.add(condition.name) else current.remove(condition.name)
            container.subscriptionConfigRepository.upsert(
                existing.copy(enabledConditionsCsv = current.joinToString(",")),
            )
        }
    }

    fun requestOperation(
        subscriptionId: Int,
        condition: ForwardingCondition,
        action: OperationAction,
    ) {
        viewModelScope.launch {
            val config = container.subscriptionConfigRepository.get(subscriptionId)
            val destination = config?.destinationId
                ?.let { container.destinationRepository.get(it) }
            if (action == OperationAction.ACTIVATE && destination == null) {
                dialogInternal.value = ForwardingDialog.DestinationEditor(subscriptionId)
                return@launch
            }
            val snapshot = subscriptions.value.firstOrNull { it.subscriptionId == subscriptionId }
            val result = container.operationCoordinator.prepareOperation(
                subscriptionId = subscriptionId,
                condition = condition,
                action = action,
                destinationE164 = destination?.e164,
                operatorDisplayName = snapshot?.stableLabel.orEmpty(),
            )
            when (result) {
                is OperationRequestResult.HandoffReady -> dialogInternal.value =
                    ForwardingDialog.Confirm(
                        operation = result.operation,
                        handoff = result.handoff,
                        condition = condition,
                        destinationText = destination?.e164,
                    )
                is OperationRequestResult.ManualRequired -> dialogInternal.value =
                    ForwardingDialog.Manual(result.operation, result.steps, result.queryMmi)
                is OperationRequestResult.Rejected ->
                    _messages.send(SimsMessage.Text(result.message))
            }
        }
    }

    fun launchConfirmedHandoff() {
        val current = dialogInternal.value as? ForwardingDialog.Confirm ?: return
        viewModelScope.launch {
            val launched = container.mmiBackend().launch(current.handoff)
            if (launched) {
                container.operationCoordinator.recordHandoffLaunched(current.operation.operationId)
                dialogInternal.value = ForwardingDialog.ResultQuestion(current.operation)
            } else {
                container.operationCoordinator.recordFailure(
                    current.operation.operationId,
                    com.callora.app.core.model.FailureReason.INTERNAL_ERROR,
                    "No system dialer could handle the MMI intent.",
                )
                dialogInternal.value = null
                _messages.send(SimsMessage.Text("No dialer available"))
            }
        }
    }

    fun recordUserResult(accepted: Boolean, detail: String?) {
        val current = dialogInternal.value as? ForwardingDialog.ResultQuestion ?: return
        viewModelScope.launch {
            container.operationCoordinator.recordUserResult(
                current.operation.operationId,
                carrierAccepted = accepted,
                detail = detail,
            )
            dialogInternal.value = null
        }
    }

    private fun defaultConfig(
        subscriptionId: Int,
        snapshot: SubscriptionSnapshot?,
    ): SubscriptionConfigEntity = SubscriptionConfigEntity(
        subscriptionId = subscriptionId,
        displayName = snapshot?.stableLabel.orEmpty(),
        operatorId = snapshot?.operator?.name.orEmpty(),
        mccMnc = snapshot?.mccMnc,
        slotIndex = snapshot?.slotIndex ?: 0,
        profileId = null,
        enabledConditionsCsv = "",
        destinationId = null,
        lastSeenEpochMs = System.currentTimeMillis(),
        automationSuspended = false,
        suspendReason = null,
    )

    companion object {
        fun factory(app: CalloraApplication): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = SimsViewModel(app) as T
            }
    }
}
