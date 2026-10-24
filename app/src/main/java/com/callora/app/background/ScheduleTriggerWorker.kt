package com.callora.app.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.callora.app.R
import com.callora.app.core.di.AppContainer
import com.callora.app.core.domain.ScheduleCalculator
import com.callora.app.core.model.HistoryEventType
import com.callora.app.core.model.HistoryOutcome
import kotlinx.coroutines.flow.first
import java.time.Instant

/**
 * Evaluates profile schedules against the current time. Because exact alarms and
 * background activity launches are restricted, this worker only flips desired
 * state, records history, notifies the user and asks [ReconciliationWorker] to
 * reconcile — it never changes carrier settings directly.
 */
class ScheduleTriggerWorker(
    appContext: Context,
    params: WorkerParameters,
    private val container: AppContainer,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = runCatching {
        val now = Instant.now()
        val schedules = container.scheduleRepository.observeAll().first()
        val profiles = container.profileRepository.observeAll().first()

        profiles.forEach { profile ->
            val schedule = profile.scheduleId
                ?.let { id -> schedules.firstOrNull { it.id == id } }
                ?: return@forEach
            val zone = ScheduleCalculator.zoneIdOrDefault(schedule)
            val active = ScheduleCalculator.isActiveAt(schedule, now, zone)
            container.historyRepository.record(
                HistoryEventType.PROFILE_CHANGED,
                if (active) HistoryOutcome.INFO else HistoryOutcome.INFO,
                applicationContext.getString(
                    R.string.history_schedule_evaluated,
                    profile.name,
                    applicationContext.getString(
                        if (active) R.string.schedule_state_active else R.string.schedule_state_inactive,
                    ),
                ),
            )
        }

        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            ReconciliationWorker.BOOT_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            OneTimeWorkRequestBuilder<ReconciliationWorker>().build(),
        )
        Result.success()
    }.getOrElse { Result.retry() }
}
