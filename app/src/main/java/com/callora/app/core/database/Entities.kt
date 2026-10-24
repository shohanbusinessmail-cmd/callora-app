package com.callora.app.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "destinations")
data class DestinationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val e164: String,
    val label: String,
    val rawInput: String,
    val createdAtEpochMs: Long,
)

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val enabled: Boolean,
    /** Comma-separated ISO weekdays (1=Mon..7=Sun). */
    val activeWeekdaysCsv: String,
    val startMinuteOfDay: Int?,
    val endMinuteOfDay: Int?,
    val zoneId: String,
)

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val type: String,
    /** Comma-separated [com.callora.app.core.model.ForwardingCondition] names. */
    val enabledConditionsCsv: String,
    val destinationId: Long?,
    val automationPolicy: String,
    val scheduleId: Long?,
    val isDefault: Boolean,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

/**
 * Persisted per-subscription configuration. Keyed by the Android subscription
 * handle; if a SIM disappears its row is preserved but automation is suspended.
 */
@Entity(tableName = "subscription_configs")
data class SubscriptionConfigEntity(
    @PrimaryKey val subscriptionId: Int,
    val displayName: String,
    val operatorId: String,
    val mccMnc: String?,
    val slotIndex: Int,
    val profileId: Long?,
    val enabledConditionsCsv: String,
    val destinationId: Long?,
    val lastSeenEpochMs: Long,
    val automationSuspended: Boolean,
    val suspendReason: String?,
)

@Entity(tableName = "operations")
data class OperationEntity(
    @PrimaryKey val operationId: String,
    val subscriptionId: Int,
    val condition: String,
    val action: String,
    val destinationE164: String?,
    val backend: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val status: String,
    val attempts: Int,
    val maxAttempts: Int,
    val provenance: String,
    val failureReason: String?,
    val retryEligible: Boolean,
    val detail: String?,
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestampEpochMs: Long,
    val type: String,
    val outcome: String,
    val subscriptionId: Int?,
    val operationId: String?,
    val message: String,
)

@Entity(tableName = "observed_status", primaryKeys = ["subscriptionId", "condition"])
data class ObservedStatusEntity(
    val subscriptionId: Int,
    val condition: String,
    val state: String,
    val provenance: String,
    val destinationE164: String?,
    val observedAtEpochMs: Long,
    val detail: String?,
)
