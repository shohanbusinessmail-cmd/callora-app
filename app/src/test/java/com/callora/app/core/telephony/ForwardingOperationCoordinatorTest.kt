package com.callora.app.core.telephony

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.callora.app.core.database.CalloraDatabase
import com.callora.app.core.database.RoomHistoryRepository
import com.callora.app.core.database.RoomObservedStatusRepository
import com.callora.app.core.database.RoomOperationRepository
import com.callora.app.core.domain.RetryPolicy
import com.callora.app.core.model.ForwardingCondition
import com.callora.app.core.model.ForwardingState
import com.callora.app.core.model.OperationAction
import com.callora.app.core.model.OperationStatus
import com.callora.app.core.model.VerificationProvenance
import com.callora.app.core.telephony.backend.ManualGuideBackend
import com.callora.app.core.telephony.backend.MmiIntentBackend
import com.callora.app.core.telephony.backend.OfficialApiBackend
import com.callora.app.core.telephony.backend.UnsupportedBackend
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
class ForwardingOperationCoordinatorTest {

    private lateinit var database: CalloraDatabase
    private lateinit var coordinator: ForwardingOperationCoordinator
    private var clockMs = 10_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        database = Room.inMemoryDatabaseBuilder(context, CalloraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val operationRepository = RoomOperationRepository(database.operationDao())
        val observedRepository = RoomObservedStatusRepository(database.observedStatusDao())
        val historyRepository = RoomHistoryRepository(database.historyDao())
        coordinator = ForwardingOperationCoordinator(
            operationRepository = operationRepository,
            observedStatusRepository = observedRepository,
            historyRepository = historyRepository,
            capabilityProvider = TelephonyCapabilityProvider(context),
            mmiIntentBackend = MmiIntentBackend(context),
            manualGuideBackend = ManualGuideBackend(),
            officialApiBackend = OfficialApiBackend(),
            unsupportedBackend = UnsupportedBackend(),
            retryPolicy = RetryPolicy(),
            now = { clockMs },
        )
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun prepare_returns_mmi_handoff_with_standard_code() = runTest {
        val result = coordinator.prepareOperation(
            subscriptionId = 1,
            condition = ForwardingCondition.CFNRC,
            action = OperationAction.ACTIVATE,
            destinationE164 = "+8801712345678",
            operatorDisplayName = "Grameenphone",
        )
        assertThat(result).isInstanceOf(OperationRequestResult.HandoffReady::class.java)
        val handoff = (result as OperationRequestResult.HandoffReady).handoff
        assertThat(handoff.mmi).isEqualTo("**62*8801712345678#")
        assertThat(handoff.condition).isEqualTo(ForwardingCondition.CFNRC)
    }

    @Test
    fun conflicting_in_flight_operation_is_rejected() = runTest {
        val first = coordinator.prepareOperation(
            1, ForwardingCondition.CFNRC, OperationAction.ACTIVATE, "+8801712345678", "GP",
        ) as OperationRequestResult.HandoffReady
        coordinator.recordHandoffLaunched(first.operation.operationId)

        val second = coordinator.prepareOperation(
            1, ForwardingCondition.CFNRC, OperationAction.ACTIVATE, "+8801712345678", "GP",
        )
        assertThat(second).isInstanceOf(OperationRequestResult.Rejected::class.java)
    }

    @Test
    fun submitted_handoff_is_recorded_as_unconfirmed_not_verified() = runTest {
        val prepared = coordinator.prepareOperation(
            1, ForwardingCondition.CFNRC, OperationAction.ACTIVATE, "+8801712345678", "GP",
        ) as OperationRequestResult.HandoffReady

        val updated = coordinator.recordHandoffLaunched(prepared.operation.operationId)
        requireNotNull(updated)
        assertThat(updated.status).isEqualTo(OperationStatus.SUBMITTED)
        assertThat(updated.provenance)
            .isEqualTo(VerificationProvenance.COMMAND_SUBMITTED_UNCONFIRMED)

        val observed = database.observedStatusDao()
            .get(1, ForwardingCondition.CFNRC.name)
        assertThat(observed?.state).isEqualTo(ForwardingState.ACTIVE_UNVERIFIED.name)
    }

    @Test
    fun user_reported_success_never_becomes_network_verified() = runTest {
        val prepared = coordinator.prepareOperation(
            1, ForwardingCondition.CFNRC, OperationAction.ACTIVATE, "+8801712345678", "GP",
        ) as OperationRequestResult.HandoffReady
        coordinator.recordHandoffLaunched(prepared.operation.operationId)
        coordinator.recordUserResult(prepared.operation.operationId, carrierAccepted = true, detail = "OK")

        val op = coordinator.let {
            database.operationDao().getById(prepared.operation.operationId)
        }
        assertThat(op?.status).isEqualTo(OperationStatus.CONFIRMED.name)
        assertThat(op?.provenance).isEqualTo(VerificationProvenance.USER_REPORTED.name)

        val observed = database.observedStatusDao().get(1, ForwardingCondition.CFNRC.name)
        assertThat(observed?.state).isEqualTo(ForwardingState.ACTIVE_UNVERIFIED.name)
        assertThat(database.historyDao().observeRecent().first()).isNotEmpty()
    }

    @Test
    fun carrier_rejection_is_not_retried_and_marks_error() = runTest {
        val prepared = coordinator.prepareOperation(
            1, ForwardingCondition.CFU, OperationAction.ACTIVATE, "+8801712345678", "GP",
        ) as OperationRequestResult.HandoffReady
        coordinator.recordHandoffLaunched(prepared.operation.operationId)
        coordinator.recordUserResult(prepared.operation.operationId, carrierAccepted = false, detail = "Rejected")

        val op = database.operationDao().getById(prepared.operation.operationId)
        assertThat(op?.status).isEqualTo(OperationStatus.REJECTED.name)
        assertThat(op?.retryEligible).isFalse()
        val observed = database.observedStatusDao().get(1, ForwardingCondition.CFU.name)
        assertThat(observed?.state).isEqualTo(ForwardingState.ERROR.name)
    }

    @Test
    fun deactivation_records_inactive_on_user_confirmation() = runTest {
        val prepared = coordinator.prepareOperation(
            1, ForwardingCondition.CFNRC, OperationAction.DEACTIVATE, destinationE164 = null,
            operatorDisplayName = "GP",
        ) as OperationRequestResult.HandoffReady
        coordinator.recordHandoffLaunched(prepared.operation.operationId)
        coordinator.recordUserResult(prepared.operation.operationId, carrierAccepted = true, detail = null)

        val observed = database.observedStatusDao().get(1, ForwardingCondition.CFNRC.name)
        assertThat(observed?.state).isEqualTo(ForwardingState.INACTIVE.name)
    }
}
