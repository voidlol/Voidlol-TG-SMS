package ru.voidlol.tgsms.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Doze-proof heartbeat. WorkManager jobs are deferred for hours while the phone lies still,
 * but allow-while-idle alarms still fire (at most every ~9 min in Doze) and give the app a
 * short network/foreground-service-start window. Each tick restarts the relay if it was killed,
 * checks that SMS permission is still granted, and sends anything left in the queue.
 */
class WatchdogReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        if (!RelayService.shouldRun(appContext)) {
            return
        }
        schedule(appContext)

        runCatching { RelayService.ensureRunning(appContext) }
            .onFailure { Log.w(TAG, "Could not restart relay service", it) }

        val pendingResult = goAsync()
        val job = scope.launch {
            RelayHealth.check(appContext)
            MessageDispatcher.flush(appContext)
        }
        scope.launch {
            try {
                withTimeoutOrNull(TICK_TIMEOUT_MS) { job.join() }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "WatchdogReceiver"
        private const val REQUEST_CODE = 42
        private const val INTERVAL_MS = 10 * 60 * 1000L
        private const val TICK_TIMEOUT_MS = 8_000L
        private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        fun schedule(context: Context) {
            val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
            val triggerAt = SystemClock.elapsedRealtime() + INTERVAL_MS
            val pendingIntent = pendingIntent(context)
            val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                alarmManager.canScheduleExactAlarms()
            when {
                canExact ->
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent
                    )
                else ->
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent
                    )
            }
        }

        fun cancel(context: Context) {
            context.getSystemService(AlarmManager::class.java)?.cancel(pendingIntent(context))
        }

        private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, WatchdogReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
