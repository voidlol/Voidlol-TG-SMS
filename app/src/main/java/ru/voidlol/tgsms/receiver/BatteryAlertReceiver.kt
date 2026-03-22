package ru.voidlol.tgsms.receiver

import ru.voidlol.tgsms.data.BatteryAlertStateStore
import ru.voidlol.tgsms.data.AppSettingsStore
import ru.voidlol.tgsms.data.MessageQueue
import ru.voidlol.tgsms.util.BatteryAlertFormatter
import ru.voidlol.tgsms.util.BatteryAlertPolicy
import ru.voidlol.tgsms.util.BatteryStatusReader

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BatteryAlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BATTERY_CHANGED != intent.action) {
            return
        }

        val appContext = context.applicationContext
        val settings = AppSettingsStore(appContext).load()
        if (!settings.isComplete) {
            return
        }

        val alertStateStore = BatteryAlertStateStore(appContext)
        val batterySnapshot = BatteryStatusReader.fromIntent(intent)
        val batteryPercent = batterySnapshot.percent
        val threshold = settings.batteryAlertThresholdPercent

        if (BatteryAlertPolicy.shouldReset(batteryPercent, batterySnapshot.isCharging, threshold)) {
            alertStateStore.clear()
            return
        }

        if (!BatteryAlertPolicy.shouldAlert(
                batteryPercent = batteryPercent,
                isCharging = batterySnapshot.isCharging,
                thresholdPercent = threshold,
                lastAlertedPercent = alertStateStore.lastAlertedPercent()
            )
        ) {
            return
        }

        val message = BatteryAlertFormatter.formatMessage(appContext, batteryPercent)
        MessageQueue.enqueue(appContext, message)
        batteryPercent?.let(alertStateStore::setLastAlertedPercent)
    }
}
