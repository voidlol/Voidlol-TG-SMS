package com.example.voidloltg_sms

import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object TelegramSender {
    fun sendMessage(
        settings: AppSettings,
        message: String,
        openConnection: (String) -> HttpURLConnection = { endpoint ->
            URL(endpoint).openConnection() as HttpURLConnection
        }
    ): Result<Unit> {
        if (!settings.isComplete) {
            return Result.failure(IllegalStateException("Bot token or chat ID is missing"))
        }

        return runCatching {
            val endpoint = "https://api.telegram.org/bot${settings.botToken}/sendMessage"
            val connection = openConnection(endpoint).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 15_000
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            }

            val body = createRequestBody(settings.chatId, message)
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(body)
            }

            val statusCode = connection.responseCode
            val responseText = if (statusCode in 200..299) {
                connection.inputStream.bufferedReader().use(BufferedReader::readText)
            } else {
                connection.errorStream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
            }

            if (statusCode !in 200..299) {
                error("Telegram API error $statusCode: $responseText")
            }
        }
    }

    internal fun createRequestBody(chatId: String, message: String): String {
        return "chat_id=${urlEncode(chatId)}&text=${urlEncode(message)}"
    }

    private fun urlEncode(value: String): String =
        java.net.URLEncoder.encode(value, Charsets.UTF_8.name())
}
