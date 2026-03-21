package com.example.voidloltg_sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import kotlin.concurrent.thread

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION != intent.action) {
            return
        }

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        thread(name = "sms-forwarder") {
            try {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                if (messages.isEmpty()) {
                    return@thread
                }

                val settings = AppSettingsStore(appContext).load()
                if (!settings.isComplete) {
                    return@thread
                }

                val senderNumber = messages.firstOrNull()?.originatingAddress
                val senderLabel = PhoneMetadataResolver.resolveSenderLabel(appContext, senderNumber)
                val simPhoneNumber = PhoneMetadataResolver.resolveSimPhoneNumber(appContext, intent)
                val body = messages.joinToString(separator = "") { it.messageBody.orEmpty() }
                val formattedMessage = MessageFormatter.smsMessage(simPhoneNumber, senderLabel, body)

                TelegramSender.sendMessage(settings, formattedMessage)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
