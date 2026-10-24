package com.callora.app.background

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.callora.app.core.di.AppContainer

/** Instantiates Callora workers with the shared [AppContainer]. */
class CalloraWorkerFactory(
    private val container: AppContainer,
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? = when (workerClassName) {
        ReconciliationWorker::class.java.name ->
            ReconciliationWorker(appContext, workerParameters, container)
        ScheduleTriggerWorker::class.java.name ->
            ScheduleTriggerWorker(appContext, workerParameters, container)
        else -> null
    }
}
