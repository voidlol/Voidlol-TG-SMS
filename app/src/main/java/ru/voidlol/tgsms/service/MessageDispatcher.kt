package ru.voidlol.tgsms.service

import ru.voidlol.tgsms.data.AppSettingsStore
import ru.voidlol.tgsms.data.ForwardedMessageLog
import ru.voidlol.tgsms.data.MessageQueueStore
import ru.voidlol.tgsms.telegram.TelegramSender

import android.content.Context
import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Sends every queued message to Telegram. Shared by [TelegramWorker] and the SMS receiver;
 * the mutex keeps concurrent callers in one process from sending the same message twice.
 */
object MessageDispatcher {

    private const val TAG = "MessageDispatcher"
    private val mutex = Mutex()

    /** Returns true when the queue is empty afterwards. */
    suspend fun flush(context: Context): Boolean = mutex.withLock {
        val appContext = context.applicationContext
        val store = MessageQueueStore(appContext)
        val pending = store.readAll()
        if (pending.isEmpty()) {
            return@withLock true
        }
        val settings = AppSettingsStore(appContext).load()
        if (!settings.isComplete) {
            Log.w(TAG, "Settings are not available, keeping ${pending.size} queued message(s)")
            return@withLock false
        }

        val log = ForwardedMessageLog(appContext)
        var anyFailed = false
        for (message in pending) {
            val result = TelegramSender.sendMessage(settings, message.text)
            if (result.isSuccess) {
                log.add(message.text)
                store.remove(message.id)
            } else {
                anyFailed = true
                Log.w(TAG, "Failed to send queued message", result.exceptionOrNull())
            }
        }
        !anyFailed
    }
}
