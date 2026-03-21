package ru.voidlol.tgsms.receiver

import ru.voidlol.tgsms.data.AppSettingsStore
import ru.voidlol.tgsms.data.MessageQueue
import ru.voidlol.tgsms.service.RelayService

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        val appContext = context.applicationContext
        val settings = AppSettingsStore(appContext).load()
        if (!settings.isComplete) {
            return
        }

        MessageQueue.scheduleWorker(appContext)
        RelayService.start(appContext)
    }
}
