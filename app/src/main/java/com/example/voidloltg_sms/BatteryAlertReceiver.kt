package com.example.voidloltg_sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlin.concurrent.thread

class BatteryAlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BATTERY_LOW != intent.action) {
            return
        }

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        thread(name = "battery-forwarder") {
            try {
                val settings = AppSettingsStore(appContext).load()
                if (!settings.isComplete) {
                    return@thread
                }

                val batteryPercent = BatteryStatusReader.readBatteryPercent(appContext)
                val message = BatteryAlertFormatter.formatMessage(appContext, batteryPercent)

                TelegramSender.sendMessage(
                    settings = settings,
                    message = message
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
