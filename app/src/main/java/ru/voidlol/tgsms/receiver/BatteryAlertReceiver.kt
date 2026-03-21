package ru.voidlol.tgsms.receiver

import ru.voidlol.tgsms.data.AppSettingsStore
import ru.voidlol.tgsms.data.MessageQueue
import ru.voidlol.tgsms.util.BatteryAlertFormatter
import ru.voidlol.tgsms.util.BatteryStatusReader

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BatteryAlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BATTERY_LOW != intent.action) {
            return
        }

        val appContext = context.applicationContext

        val settings = AppSettingsStore(appContext).load()
        if (!settings.isComplete) {
            return
        }

        val batteryPercent = BatteryStatusReader.readBatteryPercent(appContext)
        val message = BatteryAlertFormatter.formatMessage(appContext, batteryPercent)

        MessageQueue.enqueue(appContext, message)
    }
}
