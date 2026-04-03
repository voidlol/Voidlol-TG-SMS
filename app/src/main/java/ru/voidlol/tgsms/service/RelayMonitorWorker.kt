package ru.voidlol.tgsms.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import ru.voidlol.tgsms.data.AppSettingsStore

class RelayMonitorWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val appContext = applicationContext
        val serviceState = RelayServiceStateStore(appContext)
        val settings = AppSettingsStore(appContext).load()

        if (!serviceState.shouldRun() || !settings.isComplete) {
            return Result.success()
        }

        if (!RelayService.isRunning) {
            RelayService.start(appContext)
        }

        return Result.success()
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "relay-monitor-periodic"
        private const val IMMEDIATE_WORK_NAME = "relay-monitor-immediate"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<RelayMonitorWorker>(15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context.applicationContext)
                .enqueueUniquePeriodicWork(
                    PERIODIC_WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )
        }

        fun scheduleImmediate(context: Context) {
            val request = OneTimeWorkRequestBuilder<RelayMonitorWorker>().build()
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork(
                    IMMEDIATE_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    request
                )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context.applicationContext)
                .cancelUniqueWork(PERIODIC_WORK_NAME)
            WorkManager.getInstance(context.applicationContext)
                .cancelUniqueWork(IMMEDIATE_WORK_NAME)
        }
    }
}
