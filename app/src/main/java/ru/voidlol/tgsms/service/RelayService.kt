package ru.voidlol.tgsms.service

import ru.voidlol.tgsms.MainActivity
import ru.voidlol.tgsms.R
import ru.voidlol.tgsms.data.AppSettingsStore
import ru.voidlol.tgsms.data.MessageQueue
import ru.voidlol.tgsms.telegram.TelegramBotPoller

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.voidlol.tgsms.receiver.BatteryAlertReceiver

class RelayService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val batteryAlertReceiver: BroadcastReceiver = BatteryAlertReceiver()
    private val stateStore by lazy { RelayServiceStateStore(applicationContext) }
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var pollerJob: Job? = null
    private var flushLoopJob: Job? = null
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            scope.launch { MessageDispatcher.flush(applicationContext) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        startedAtElapsed = SystemClock.elapsedRealtime()
        createNotificationChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        registerReceiver(batteryAlertReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        acquireLocks()
        runCatching {
            getSystemService(ConnectivityManager::class.java)
                ?.registerDefaultNetworkCallback(networkCallback)
        }
        isRunning = true
        stateStore.setShouldRun(true)
        RelayMonitorWorker.schedule(applicationContext)
        WatchdogReceiver.schedule(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stateStore.setShouldRun(true)
        RelayMonitorWorker.schedule(applicationContext)
        WatchdogReceiver.schedule(applicationContext)
        MessageQueue.scheduleWorker(applicationContext)
        if (pollerJob?.isActive != true) {
            pollerJob = scope.launch { TelegramBotPoller.startPolling(applicationContext) }
        }
        if (flushLoopJob?.isActive != true) {
            // Retry queued messages from the service itself: it keeps network access in Doze,
            // unlike WorkManager jobs, which get deferred to rare maintenance windows.
            flushLoopJob = scope.launch {
                while (isActive) {
                    RelayHealth.check(applicationContext)
                    MessageDispatcher.flush(applicationContext)
                    delay(FLUSH_INTERVAL_MS)
                }
            }
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (stateStore.shouldRun()) {
            RelayMonitorWorker.scheduleImmediate(applicationContext)
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        isRunning = false
        startedAtElapsed = 0L
        runCatching { unregisterReceiver(batteryAlertReceiver) }
        runCatching {
            getSystemService(ConnectivityManager::class.java)
                ?.unregisterNetworkCallback(networkCallback)
        }
        releaseLocks()
        scope.cancel()
        if (stateStore.shouldRun()) {
            RelayMonitorWorker.scheduleImmediate(applicationContext)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * The phone is expected to lie idle for days and must never stop relaying, so keep the CPU
     * and Wi-Fi awake for as long as the relay runs. Battery cost is accepted by design.
     */
    private fun acquireLocks() {
        if (wakeLock == null) {
            wakeLock = getSystemService(PowerManager::class.java)
                ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TgSms:relay")
                ?.apply {
                    setReferenceCounted(false)
                    acquire()
                }
        }
        if (wifiLock == null) {
            @Suppress("DEPRECATION")
            wifiLock = runCatching {
                applicationContext.getSystemService(WifiManager::class.java)
                    ?.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "TgSms:relay")
                    ?.apply {
                        setReferenceCounted(false)
                        acquire()
                    }
            }.getOrNull()
        }
    }

    private fun releaseLocks() {
        runCatching { wakeLock?.takeIf { it.isHeld }?.release() }
        runCatching { wifiLock?.takeIf { it.isHeld }?.release() }
        wakeLock = null
        wifiLock = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "relay_service"
        private const val FLUSH_INTERVAL_MS = 60_000L

        @Volatile
        var isRunning = false
            private set

        var startedAtElapsed = 0L
            private set

        fun start(context: Context) {
            val appContext = context.applicationContext
            RelayServiceStateStore(appContext).setShouldRun(true)
            RelayMonitorWorker.schedule(appContext)
            val intent = Intent(appContext, RelayService::class.java)
            ContextCompat.startForegroundService(appContext, intent)
        }

        fun stop(context: Context) {
            val appContext = context.applicationContext
            RelayServiceStateStore(appContext).setShouldRun(false)
            RelayMonitorWorker.cancel(appContext)
            WatchdogReceiver.cancel(appContext)
            appContext.stopService(Intent(appContext, RelayService::class.java))
        }

        fun shouldRun(context: Context): Boolean {
            return RelayServiceStateStore(context.applicationContext).shouldRun()
        }

        fun ensureRunning(context: Context) {
            val appContext = context.applicationContext
            val settings = AppSettingsStore(appContext).load()
            if (!shouldRun(appContext) || !settings.isComplete) {
                return
            }

            RelayMonitorWorker.schedule(appContext)
            if (!isRunning) {
                start(appContext)
            }
        }
    }
}
