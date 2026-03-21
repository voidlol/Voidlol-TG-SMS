package ru.voidlol.tgsms.telegram

import ru.voidlol.tgsms.data.AppSettings

import okhttp3.Call
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object TelegramSender {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun sendMessage(
        settings: AppSettings,
        message: String,
        callFactory: Call.Factory = client
    ): Result<Unit> {
        if (!settings.isComplete) {
            return Result.failure(IllegalStateException("Bot token or chat ID is missing"))
        }

        return runCatching {
            val endpoint = "https://api.telegram.org/bot${settings.botToken}/sendMessage"

            val body = FormBody.Builder()
                .add("chat_id", settings.chatId)
                .add("text", message)
                .build()

            val request = Request.Builder()
                .url(endpoint)
                .post(body)
                .build()

            val response = callFactory.newCall(request).execute()
            response.use {
                if (!it.isSuccessful) {
                    val errorBody = it.body?.string().orEmpty()
                    error("Telegram API error ${it.code}: $errorBody")
                }
            }
        }
    }
}
