package com.callora.app.core.database

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.callora.app.core.model.ForwardingCondition
import com.callora.app.core.model.OperationAction
import com.callora.app.core.model.OperationStatus
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class RoomDatabaseTest {

    private lateinit var database: CalloraDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        database = Room.inMemoryDatabaseBuilder(context, CalloraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun destination_crud_round_trips() = runTest {
        val dao = database.destinationDao()
        val id = dao.upsert(
            DestinationEntity(
                e164 = "+8801712345678",
                label = "01712345678",
                rawInput = "01712345678",
                createdAtEpochMs = 1_000L,
            ),
        )
        assertThat(id).isGreaterThan(0L)
        val loaded = dao.getById(id)
        assertThat(loaded?.e164).isEqualTo("+8801712345678")

        dao.delete(loaded!!)
        assertThat(dao.getById(id)).isNull()
    }

    @Test
    fun operation_conflict_count_distinguishes_in_flight() = runTest {
        val dao = database.operationDao()
        val op = OperationEntity(
            operationId = "op-1",
            subscriptionId = 3,
            condition = ForwardingCondition.CFNRC.name,
            action = OperationAction.ACTIVATE.name,
            destinationE164 = "+8801712345678",
            backend = "USER_CONFIRMED_MMI",
            createdAtEpochMs = 1L,
            updatedAtEpochMs = 1L,
            status = OperationStatus.PENDING.name,
            attempts = 0,
            maxAttempts = 4,
            provenance = "LOCAL_DESIRED_POLICY",
            failureReason = null,
            retryEligible = true,
            detail = null,
        )
        dao.upsert(op)
        assertThat(
            dao.countInFlight(3, ForwardingCondition.CFNRC.name, OperationAction.ACTIVATE.name),
        ).isEqualTo(1)
        assertThat(
            dao.countInFlight(3, ForwardingCondition.CFU.name, OperationAction.ACTIVATE.name),
        ).isEqualTo(0)

        dao.upsert(op.copy(status = OperationStatus.CONFIRMED.name))
        assertThat(
            dao.countInFlight(3, ForwardingCondition.CFNRC.name, OperationAction.ACTIVATE.name),
        ).isEqualTo(0)
    }

    @Test
    fun history_insert_and_clear() = runTest {
        val dao = database.historyDao()
        dao.insert(
            HistoryEntity(
                timestampEpochMs = 1L,
                type = "RECONCILIATION",
                outcome = "INFO",
                subscriptionId = null,
                operationId = null,
                message = "hello",
            ),
        )
        assertThat(dao.observeRecent().first()).hasSize(1)
        dao.clear()
        assertThat(dao.observeRecent().first()).isEmpty()
    }

    @Test
    fun subscription_config_is_retained_when_suspended() = runTest {
        val dao = database.subscriptionConfigDao()
        val entity = SubscriptionConfigEntity(
            subscriptionId = 9,
            displayName = "Robi",
            operatorId = "ROBI",
            mccMnc = "47002",
            slotIndex = 0,
            profileId = null,
            enabledConditionsCsv = "CFNRC",
            destinationId = null,
            lastSeenEpochMs = 5L,
            automationSuspended = false,
            suspendReason = null,
        )
        dao.upsert(entity)
        dao.upsert(entity.copy(automationSuspended = true, suspendReason = "SIM_NOT_PRESENT"))
        val stored = dao.getById(9)
        assertThat(stored?.automationSuspended).isTrue()
        assertThat(stored?.enabledConditionsCsv).isEqualTo("CFNRC")
    }
}
