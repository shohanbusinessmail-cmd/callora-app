package com.callora.app

import android.app.Application
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.callora.app.background.CalloraWorkerFactory
import com.callora.app.background.ReconciliationWorker
import com.callora.app.core.di.AppContainer
import com.callora.app.core.localization.LocaleManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

/**
 * Application root. Hosts the manual DI container and explicitly configures
 * WorkManager with a custom [CalloraWorkerFactory].
 */
class CalloraApplication : Application(), Configuration.Provider {

    lateinit var container: AppContainer
        private set

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(CalloraWorkerFactory(container))
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Apply the persisted language before any activity is created.
        runBlocking {
            val settings = container.settingsRepository.settings.first()
            LocaleManager.apply(settings.language)
        }

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            ReconciliationWorker.PERIODIC_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ReconciliationWorker>(12, TimeUnit.HOURS).build(),
        )
    }
}
