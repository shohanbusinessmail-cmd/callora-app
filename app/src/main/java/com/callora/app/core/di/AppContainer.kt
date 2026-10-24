package com.callora.app.core.di

import android.content.Context
import com.callora.app.core.database.CalloraDatabase
import com.callora.app.core.database.RoomDestinationRepository
import com.callora.app.core.database.RoomHistoryRepository
import com.callora.app.core.database.RoomObservedStatusRepository
import com.callora.app.core.database.RoomOperationRepository
import com.callora.app.core.database.RoomProfileRepository
import com.callora.app.core.database.RoomScheduleRepository
import com.callora.app.core.database.RoomSubscriptionConfigRepository
import com.callora.app.core.datastore.SettingsRepository
import com.callora.app.core.domain.PhoneNumberNormalizer
import com.callora.app.core.domain.RetryPolicy
import com.callora.app.core.notification.CalloraNotificationCenter
import com.callora.app.core.telephony.ForwardingOperationCoordinator
import com.callora.app.core.telephony.SubscriptionScanner
import com.callora.app.core.telephony.TelephonyCapabilityProvider
import com.callora.app.core.telephony.backend.ManualGuideBackend
import com.callora.app.core.telephony.backend.MmiIntentBackend
import com.callora.app.core.telephony.backend.OfficialApiBackend
import com.callora.app.core.telephony.backend.UnsupportedBackend

/**
 * Lightweight manual dependency graph. The app is small enough that a
 * compile-time DI framework would add more complexity than value.
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    val database: CalloraDatabase by lazy { CalloraDatabase.build(appContext) }

    val destinationRepository by lazy { RoomDestinationRepository(database.destinationDao()) }
    val scheduleRepository by lazy { RoomScheduleRepository(database.scheduleDao()) }
    val profileRepository by lazy { RoomProfileRepository(database.profileDao()) }
    val subscriptionConfigRepository by lazy {
        RoomSubscriptionConfigRepository(database.subscriptionConfigDao())
    }
    val operationRepository by lazy { RoomOperationRepository(database.operationDao()) }
    val historyRepository by lazy { RoomHistoryRepository(database.historyDao()) }
    val observedStatusRepository by lazy { RoomObservedStatusRepository(database.observedStatusDao()) }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }

    val phoneNumberNormalizer: PhoneNumberNormalizer by lazy { PhoneNumberNormalizer() }
    val subscriptionScanner: SubscriptionScanner by lazy { SubscriptionScanner(appContext) }
    val capabilityProvider: TelephonyCapabilityProvider by lazy {
        TelephonyCapabilityProvider(appContext)
    }

    private val mmiIntentBackend: MmiIntentBackend by lazy { MmiIntentBackend(appContext) }
    private val manualGuideBackend: ManualGuideBackend by lazy { ManualGuideBackend() }
    private val officialApiBackend: OfficialApiBackend by lazy { OfficialApiBackend() }
    private val unsupportedBackend: UnsupportedBackend by lazy { UnsupportedBackend() }

    val operationCoordinator: ForwardingOperationCoordinator by lazy {
        ForwardingOperationCoordinator(
            operationRepository = operationRepository,
            observedStatusRepository = observedStatusRepository,
            historyRepository = historyRepository,
            capabilityProvider = capabilityProvider,
            mmiIntentBackend = mmiIntentBackend,
            manualGuideBackend = manualGuideBackend,
            officialApiBackend = officialApiBackend,
            unsupportedBackend = unsupportedBackend,
            retryPolicy = RetryPolicy(),
        )
    }

    val notificationCenter: CalloraNotificationCenter by lazy {
        CalloraNotificationCenter(appContext)
    }

    fun mmiBackend(): MmiIntentBackend = mmiIntentBackend
}
