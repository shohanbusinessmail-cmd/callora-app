package com.callora.app.core.database

import com.callora.app.core.model.ForwardingCondition
import com.callora.app.core.model.ForwardingDestination
import com.callora.app.core.model.ForwardingOperation
import com.callora.app.core.model.ForwardingProfile
import com.callora.app.core.model.HistoryEvent
import com.callora.app.core.model.HistoryEventType
import com.callora.app.core.model.HistoryOutcome
import com.callora.app.core.model.ObservedForwardingStatus
import com.callora.app.core.model.OperationAction
import com.callora.app.core.model.ScheduleModel
import com.callora.app.core.model.SubscriptionSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

interface DestinationRepository {
    fun observeAll(): Flow<List<ForwardingDestination>>
    suspend fun save(destination: ForwardingDestination): Long
    suspend fun get(id: Long): ForwardingDestination?
    suspend fun delete(destination: ForwardingDestination)
}

class RoomDestinationRepository(private val dao: DestinationDao) : DestinationRepository {
    override fun observeAll(): Flow<List<ForwardingDestination>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun save(destination: ForwardingDestination): Long =
        dao.upsert(destination.toEntity())

    override suspend fun get(id: Long): ForwardingDestination? = dao.getById(id)?.toDomain()

    override suspend fun delete(destination: ForwardingDestination) =
        dao.delete(destination.toEntity())
}

interface ScheduleRepository {
    fun observeAll(): Flow<List<ScheduleModel>>
    suspend fun save(schedule: ScheduleModel): Long
    suspend fun get(id: Long): ScheduleModel?
    suspend fun delete(schedule: ScheduleModel)
}

class RoomScheduleRepository(private val dao: ScheduleDao) : ScheduleRepository {
    override fun observeAll(): Flow<List<ScheduleModel>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun save(schedule: ScheduleModel): Long = dao.upsert(schedule.toEntity())
    override suspend fun get(id: Long): ScheduleModel? = dao.getById(id)?.toDomain()
    override suspend fun delete(schedule: ScheduleModel) = dao.delete(schedule.toEntity())
}

interface ProfileRepository {
    fun observeAll(): Flow<List<ForwardingProfile>>
    suspend fun save(profile: ForwardingProfile): Long
    suspend fun get(id: Long): ForwardingProfile?
    suspend fun delete(profile: ForwardingProfile)
}

class RoomProfileRepository(private val dao: ProfileDao) : ProfileRepository {
    override fun observeAll(): Flow<List<ForwardingProfile>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun save(profile: ForwardingProfile): Long = dao.upsert(profile.toEntity())
    override suspend fun get(id: Long): ForwardingProfile? = dao.getById(id)?.toDomain()
    override suspend fun delete(profile: ForwardingProfile) = dao.delete(profile.toEntity())
}

/**
 * Per-subscription desired configuration. When a SIM disappears the row is
 * retained and automation is suspended — it is never silently re-applied to a
 * different SIM occupying the same slot.
 */
interface SubscriptionConfigRepository {
    fun observeAll(): Flow<List<SubscriptionConfigEntity>>
    suspend fun get(subscriptionId: Int): SubscriptionConfigEntity?
    suspend fun upsert(entity: SubscriptionConfigEntity)
    suspend fun reconcileWithVisibleSubscriptions(visible: List<SubscriptionSnapshot>, nowMs: Long)
    suspend fun suspendAutomation(subscriptionId: Int, reason: String?)
}

class RoomSubscriptionConfigRepository(
    private val dao: SubscriptionConfigDao,
) : SubscriptionConfigRepository {

    override fun observeAll(): Flow<List<SubscriptionConfigEntity>> = dao.observeAll()

    override suspend fun get(subscriptionId: Int): SubscriptionConfigEntity? =
        dao.getById(subscriptionId)

    override suspend fun upsert(entity: SubscriptionConfigEntity) = dao.upsert(entity)

    override suspend fun reconcileWithVisibleSubscriptions(
        visible: List<SubscriptionSnapshot>,
        nowMs: Long,
    ) {
        val visibleIds = visible.map { it.subscriptionId }.toSet()
        val stored = dao.observeAll().first()
        stored.forEach { row ->
            if (row.subscriptionId !in visibleIds && !row.automationSuspended) {
                dao.upsert(row.copy(automationSuspended = true, suspendReason = "SIM_NOT_PRESENT"))
            }
        }
        visible.forEach { snapshot ->
            val existing = dao.getById(snapshot.subscriptionId)
            if (existing == null) {
                dao.upsert(
                    SubscriptionConfigEntity(
                        subscriptionId = snapshot.subscriptionId,
                        displayName = snapshot.stableLabel,
                        operatorId = snapshot.operator.name,
                        mccMnc = snapshot.mccMnc,
                        slotIndex = snapshot.slotIndex,
                        profileId = null,
                        enabledConditionsCsv = "",
                        destinationId = null,
                        lastSeenEpochMs = nowMs,
                        automationSuspended = false,
                        suspendReason = null,
                    ),
                )
            } else {
                dao.upsert(
                    existing.copy(
                        displayName = snapshot.stableLabel,
                        operatorId = snapshot.operator.name,
                        mccMnc = snapshot.mccMnc,
                        slotIndex = snapshot.slotIndex,
                        lastSeenEpochMs = nowMs,
                        automationSuspended = false,
                        suspendReason = null,
                    ),
                )
            }
        }
    }

    override suspend fun suspendAutomation(subscriptionId: Int, reason: String?) {
        dao.getById(subscriptionId)?.let {
            dao.upsert(it.copy(automationSuspended = true, suspendReason = reason))
        }
    }
}

interface OperationRepository {
    suspend fun createIfAbsent(operation: ForwardingOperation): ForwardingOperation
    suspend fun update(operation: ForwardingOperation)
    suspend fun get(id: String): ForwardingOperation?
    fun observeRecent(limit: Int = 100): Flow<List<ForwardingOperation>>
    suspend fun inFlightKeys(): Set<String>
    suspend fun hasInFlight(subscriptionId: Int, condition: ForwardingCondition, action: OperationAction): Boolean
}

class RoomOperationRepository(private val dao: OperationDao) : OperationRepository {
    override suspend fun createIfAbsent(operation: ForwardingOperation): ForwardingOperation {
        val existing = dao.getById(operation.operationId)
        if (existing != null) return existing.toDomain()
        dao.upsert(operation.toEntity())
        return operation
    }

    override suspend fun update(operation: ForwardingOperation) =
        dao.upsert(operation.toEntity())

    override suspend fun get(id: String): ForwardingOperation? = dao.getById(id)?.toDomain()

    override fun observeRecent(limit: Int): Flow<List<ForwardingOperation>> =
        dao.observeRecent(limit).map { rows -> rows.map { it.toDomain() } }

    override suspend fun inFlightKeys(): Set<String> =
        dao.getInFlight().map {
            com.callora.app.core.domain.ReconciliationEngine.key(
                it.subscriptionId,
                com.callora.app.core.model.ForwardingCondition.valueOf(it.condition),
            )
        }.toSet()

    override suspend fun hasInFlight(
        subscriptionId: Int,
        condition: ForwardingCondition,
        action: OperationAction,
    ): Boolean = dao.countInFlight(subscriptionId, condition.name, action.name) > 0
}

interface HistoryRepository {
    fun observeRecent(limit: Int = 200): Flow<List<HistoryEvent>>
    suspend fun record(
        type: HistoryEventType,
        outcome: HistoryOutcome,
        message: String,
        subscriptionId: Int? = null,
        operationId: String? = null,
        timestampMs: Long = System.currentTimeMillis(),
    )
    suspend fun clear()
}

class RoomHistoryRepository(private val dao: HistoryDao) : HistoryRepository {
    override fun observeRecent(limit: Int): Flow<List<HistoryEvent>> =
        dao.observeRecent(limit).map { rows -> rows.map { it.toDomain() } }

    override suspend fun record(
        type: HistoryEventType,
        outcome: HistoryOutcome,
        message: String,
        subscriptionId: Int?,
        operationId: String?,
        timestampMs: Long,
    ) {
        dao.insert(
            HistoryEntity(
                timestampEpochMs = timestampMs,
                type = type.name,
                outcome = outcome.name,
                subscriptionId = subscriptionId,
                operationId = operationId,
                message = message,
            ),
        )
    }

    override suspend fun clear() = dao.clear()
}

interface ObservedStatusRepository {
    fun observeAll(): Flow<List<ObservedForwardingStatus>>
    suspend fun get(subscriptionId: Int, condition: ForwardingCondition): ObservedForwardingStatus?
    suspend fun save(status: ObservedForwardingStatus)
}

class RoomObservedStatusRepository(private val dao: ObservedStatusDao) : ObservedStatusRepository {
    override fun observeAll(): Flow<List<ObservedForwardingStatus>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun get(
        subscriptionId: Int,
        condition: ForwardingCondition,
    ): ObservedForwardingStatus? = dao.get(subscriptionId, condition.name)?.toDomain()

    override suspend fun save(status: ObservedForwardingStatus) =
        dao.upsert(status.toEntity())
}
