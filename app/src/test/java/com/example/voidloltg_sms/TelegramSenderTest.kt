package com.example.voidloltg_sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

class TelegramSenderTest {
    @Test
    fun createRequestBody_urlEncodesChatIdAndMessage() {
        val body = TelegramSender.createRequestBody(
            chatId = "123 456",
            message = "hello & goodbye"
        )

        assertEquals("chat_id=123+456&text=hello+%26+goodbye", body)
    }

    @Test
    fun sendMessage_postsToTelegramEndpoint() {
        val connection = FakeHttpURLConnection(URL("https://api.telegram.org"))

        val result = TelegramSender.sendMessage(
            settings = AppSettings(botToken = "bot-token", chatId = "42"),
            message = "ping",
            openConnection = { endpoint ->
                assertEquals("https://api.telegram.org/botbot-token/sendMessage", endpoint)
                connection
            }
        )

        assertTrue(result.isSuccess)
        assertEquals("POST", connection.requestMethod)
        assertEquals(
            "application/x-www-form-urlencoded; charset=UTF-8",
            connection.headerValues["Content-Type"]
        )
        assertEquals("chat_id=42&text=ping", connection.outputAsString())
    }

    @Test
    fun sendMessage_returnsFailureForTelegramErrors() {
        val connection = FakeHttpURLConnection(
            url = URL("https://api.telegram.org"),
            responseCodeValue = 400,
            errorBody = """{"ok":false}"""
        )

        val result = TelegramSender.sendMessage(
            settings = AppSettings(botToken = "bot-token", chatId = "42"),
            message = "ping",
            openConnection = { connection }
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("Telegram API error 400"))
    }

    private class FakeHttpURLConnection(
        url: URL,
        private val responseCodeValue: Int = 200,
        private val responseBody: String = """{"ok":true}""",
        private val errorBody: String = ""
    ) : HttpURLConnection(url) {
        private val output = ByteArrayOutputStream()
        private val requestHeaders = linkedMapOf<String, String>()

        override fun disconnect() = Unit

        override fun usingProxy(): Boolean = false

        override fun connect() = Unit

        override fun setRequestProperty(key: String?, value: String?) {
            if (key != null && value != null) {
                requestHeaders[key] = value
            }
        }

        override fun getRequestProperty(key: String?): String? = requestHeaders[key]

        override fun getOutputStream(): OutputStream = output

        override fun getInputStream(): InputStream {
            return ByteArrayInputStream(responseBody.toByteArray())
        }

        override fun getErrorStream(): InputStream {
            return ByteArrayInputStream(errorBody.toByteArray())
        }

        override fun getResponseCode(): Int = responseCodeValue

        fun outputAsString(): String = output.toString(Charsets.UTF_8.name())

        val headerValues: Map<String, String>
            get() = requestHeaders
    }
}
