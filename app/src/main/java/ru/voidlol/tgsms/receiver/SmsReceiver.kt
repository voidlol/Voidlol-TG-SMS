package ru.voidlol.tgsms.receiver

import ru.voidlol.tgsms.data.MessageQueue
import ru.voidlol.tgsms.service.MessageDispatcher
import ru.voidlol.tgsms.service.RelayService
import ru.voidlol.tgsms.util.MessageFormatter
import ru.voidlol.tgsms.util.PhoneMetadataResolver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION != intent.action) {
            return
        }

        val appContext = context.applicationContext

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) {
            return
        }

        val senderNumber = messages.firstOrNull()?.originatingAddress
        val senderLabel = PhoneMetadataResolver.resolveSenderLabel(appContext, senderNumber)
        val simPhoneNumber = PhoneMetadataResolver.resolveSimPhoneNumber(appContext, intent)
        val body = messages.joinToString(separator = "") { it.messageBody.orEmpty() }
        val formattedMessage = MessageFormatter.smsMessage(simPhoneNumber, senderLabel, body)

        // Persist first and keep WorkManager as the retry path, then try to send right away:
        // OEM battery managers (Honor/Huawei/Xiaomi) can defer background jobs for hours.
        MessageQueue.enqueue(appContext, formattedMessage)
        // The SMS broadcast is proof the process is alive again; bring the relay back if it was killed.
        runCatching { RelayService.ensureRunning(appContext) }

        // OkHttp calls are blocking and ignore cancellation, so wait on the send with a timeout
        // instead of cancelling it. If the process dies afterwards the worker resends from the queue.
        val pendingResult = goAsync()
        val sendJob = scope.launch { MessageDispatcher.flush(appContext) }
        scope.launch {
            try {
                withTimeoutOrNull(SEND_TIMEOUT_MS) { sendJob.join() }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        // Broadcast receivers get ~10s after goAsync() before an ANR.
        const val SEND_TIMEOUT_MS = 8_000L
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }
}
