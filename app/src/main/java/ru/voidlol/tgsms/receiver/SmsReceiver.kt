package ru.voidlol.tgsms.receiver

import ru.voidlol.tgsms.data.AppSettingsStore
import ru.voidlol.tgsms.data.MessageQueue
import ru.voidlol.tgsms.util.MessageFormatter
import ru.voidlol.tgsms.util.PhoneMetadataResolver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION != intent.action) {
            return
        }

        val appContext = context.applicationContext

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) {
            return
        }

        val settings = AppSettingsStore(appContext).load()
        if (!settings.isComplete) {
            return
        }

        val senderNumber = messages.firstOrNull()?.originatingAddress
        val senderLabel = PhoneMetadataResolver.resolveSenderLabel(appContext, senderNumber)
        val simPhoneNumber = PhoneMetadataResolver.resolveSimPhoneNumber(appContext, intent)
        val body = messages.joinToString(separator = "") { it.messageBody.orEmpty() }
        val formattedMessage = MessageFormatter.smsMessage(simPhoneNumber, senderLabel, body)

        MessageQueue.enqueue(appContext, formattedMessage)
    }
}
