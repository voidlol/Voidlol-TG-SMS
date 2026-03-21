package ru.voidlol.tgsms.service

import ru.voidlol.tgsms.data.AppSettingsStore
import ru.voidlol.tgsms.data.ForwardedMessageLog
import ru.voidlol.tgsms.data.MessageQueueStore
import ru.voidlol.tgsms.telegram.TelegramSender

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TelegramWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val settings = AppSettingsStore(applicationContext).load()
        if (!settings.isComplete) {
            return@withContext Result.failure()
        }

        val store = MessageQueueStore(applicationContext)
        val log = ForwardedMessageLog(applicationContext)
        val pending = store.readAll()
        if (pending.isEmpty()) {
            return@withContext Result.success()
        }

        var anyFailed = false
        for (message in pending) {
            val result = TelegramSender.sendMessage(settings, message.text)
            if (result.isSuccess) {
                log.add(message.text)
                store.remove(message.id)
            } else {
                anyFailed = true
            }
        }

        if (anyFailed) Result.retry() else Result.success()
    }
}
