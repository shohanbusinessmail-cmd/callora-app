package com.callora.app.core.database

import com.callora.app.core.model.AutomationPolicy
import com.callora.app.core.model.BackendType
import com.callora.app.core.model.FailureReason
import com.callora.app.core.model.ForwardingCondition
import com.callora.app.core.model.ForwardingDestination
import com.callora.app.core.model.ForwardingOperation
import com.callora.app.core.model.ForwardingProfile
import com.callora.app.core.model.ForwardingState
import com.callora.app.core.model.HistoryEvent
import com.callora.app.core.model.HistoryEventType
import com.callora.app.core.model.HistoryOutcome
import com.callora.app.core.model.ObservedForwardingStatus
import com.callora.app.core.model.OperationAction
import com.callora.app.core.model.OperationStatus
import com.callora.app.core.model.ProfileType
import com.callora.app.core.model.ScheduleModel
import com.callora.app.core.model.VerificationProvenance

private fun <T : Enum<T>> enumOrNull(name: String?, values: Array<T>): T? =
    name?.let { runCatching { enumValueOf<T>(it) }.getOrNull() }

private fun csvToConditions(csv: String): Set<ForwardingCondition> =
    csv.split(",").filter { it.isNotBlank() }
        .mapNotNull { enumOrNull(it, ForwardingCondition.entries.toTypedArray()) }
        .toSet()

private fun Set<ForwardingCondition>.toCsv(): String = joinToString(",") { it.name }

fun DestinationEntity.toDomain(): ForwardingDestination = ForwardingDestination(
    id = id,
    e164 = e164,
    label = label,
    rawInput = rawInput,
    createdAtEpochMs = createdAtEpochMs,
)

fun ForwardingDestination.toEntity(): DestinationEntity = DestinationEntity(
    id = id,
    e164 = e164,
    label = label,
    rawInput = rawInput,
    createdAtEpochMs = createdAtEpochMs,
)

fun ProfileEntity.toDomain(): ForwardingProfile = ForwardingProfile(
    id = id,
    name = name,
    type = enumOrNull(type, ProfileType.entries.toTypedArray()) ?: ProfileType.CUSTOM,
    enabledConditions = csvToConditions(enabledConditionsCsv),
    destinationId = destinationId,
    automationPolicy = enumOrNull(automationPolicy, AutomationPolicy.entries.toTypedArray())
        ?: AutomationPolicy.MANUAL,
    scheduleId = scheduleId,
    isDefault = isDefault,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
)

fun ForwardingProfile.toEntity(): ProfileEntity = ProfileEntity(
    id = id,
    name = name,
    type = type.name,
    enabledConditionsCsv = enabledConditions.toCsv(),
    destinationId = destinationId,
    automationPolicy = automationPolicy.name,
    scheduleId = scheduleId,
    isDefault = isDefault,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
)

fun ScheduleEntity.toDomain(): ScheduleModel = ScheduleModel(
    id = id,
    name = name,
    enabled = enabled,
    activeWeekdays = activeWeekdaysCsv.split(",").mapNotNull { it.toIntOrNull() }.toSet(),
    startMinuteOfDay = startMinuteOfDay,
    endMinuteOfDay = endMinuteOfDay,
    zoneId = zoneId,
)

fun ScheduleModel.toEntity(): ScheduleEntity = ScheduleEntity(
    id = id,
    name = name,
    enabled = enabled,
    activeWeekdaysCsv = activeWeekdays.joinToString(",") { it.toString() },
    startMinuteOfDay = startMinuteOfDay,
    endMinuteOfDay = endMinuteOfDay,
    zoneId = zoneId,
)

fun OperationEntity.toDomain(): ForwardingOperation = ForwardingOperation(
    operationId = operationId,
    subscriptionId = subscriptionId,
    condition = enumOrNull(condition, ForwardingCondition.entries.toTypedArray())
        ?: ForwardingCondition.CFNRC,
    action = enumOrNull(action, OperationAction.entries.toTypedArray()) ?: OperationAction.QUERY,
    destinationE164 = destinationE164,
    backend = enumOrNull(backend, BackendType.entries.toTypedArray()) ?: BackendType.UNSUPPORTED,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
    status = enumOrNull(status, OperationStatus.entries.toTypedArray()) ?: OperationStatus.FAILED,
    attempts = attempts,
    maxAttempts = maxAttempts,
    provenance = enumOrNull(provenance, VerificationProvenance.entries.toTypedArray())
        ?: VerificationProvenance.NONE,
    failureReason = enumOrNull(failureReason, FailureReason.entries.toTypedArray()),
    retryEligible = retryEligible,
    detail = detail,
)

fun ForwardingOperation.toEntity(): OperationEntity = OperationEntity(
    operationId = operationId,
    subscriptionId = subscriptionId,
    condition = condition.name,
    action = action.name,
    destinationE164 = destinationE164,
    backend = backend.name,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
    status = status.name,
    attempts = attempts,
    maxAttempts = maxAttempts,
    provenance = provenance.name,
    failureReason = failureReason?.name,
    retryEligible = retryEligible,
    detail = detail,
)

fun HistoryEntity.toDomain(): HistoryEvent = HistoryEvent(
    id = id,
    timestampEpochMs = timestampEpochMs,
    type = enumOrNull(type, HistoryEventType.entries.toTypedArray())
        ?: HistoryEventType.RECONCILIATION,
    outcome = enumOrNull(outcome, HistoryOutcome.entries.toTypedArray()) ?: HistoryOutcome.INFO,
    subscriptionId = subscriptionId,
    operationId = operationId,
    message = message,
)

fun HistoryEvent.toEntity(): HistoryEntity = HistoryEntity(
    id = id,
    timestampEpochMs = timestampEpochMs,
    type = type.name,
    outcome = outcome.name,
    subscriptionId = subscriptionId,
    operationId = operationId,
    message = message,
)

fun ObservedStatusEntity.toDomain(): ObservedForwardingStatus = ObservedForwardingStatus(
    subscriptionId = subscriptionId,
    condition = enumOrNull(condition, ForwardingCondition.entries.toTypedArray())
        ?: ForwardingCondition.CFNRC,
    state = enumOrNull(state, ForwardingState.entries.toTypedArray()) ?: ForwardingState.UNKNOWN,
    provenance = enumOrNull(provenance, VerificationProvenance.entries.toTypedArray())
        ?: VerificationProvenance.NONE,
    destinationE164 = destinationE164,
    observedAtEpochMs = observedAtEpochMs,
    detail = detail,
)

fun ObservedForwardingStatus.toEntity(): ObservedStatusEntity = ObservedStatusEntity(
    subscriptionId = subscriptionId,
    condition = condition.name,
    state = state.name,
    provenance = provenance.name,
    destinationE164 = destinationE164,
    observedAtEpochMs = observedAtEpochMs,
    detail = detail,
)
