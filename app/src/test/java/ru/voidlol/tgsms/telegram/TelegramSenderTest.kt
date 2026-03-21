package ru.voidlol.tgsms.telegram

import ru.voidlol.tgsms.data.AppSettings

import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramSenderTest {

    @Test
    fun sendMessage_postsToTelegramEndpoint() {
        var capturedRequest: Request? = null

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                capturedRequest = chain.request()
                Response.Builder()
                    .code(200)
                    .message("OK")
                    .protocol(Protocol.HTTP_1_1)
                    .request(chain.request())
                    .body("""{"ok":true}""".toResponseBody())
                    .build()
            }
            .build()

        val result = TelegramSender.sendMessage(
            settings = AppSettings(botToken = "bot-token", chatId = "42"),
            message = "ping",
            callFactory = client
        )

        assertTrue(result.isSuccess)
        assertEquals("POST", capturedRequest?.method)
        assertEquals(
            "https://api.telegram.org/botbot-token/sendMessage",
            capturedRequest?.url.toString()
        )
    }

    @Test
    fun sendMessage_returnsFailureForTelegramErrors() {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .code(400)
                    .message("Bad Request")
                    .protocol(Protocol.HTTP_1_1)
                    .request(chain.request())
                    .body("""{"ok":false}""".toResponseBody())
                    .build()
            }
            .build()

        val result = TelegramSender.sendMessage(
            settings = AppSettings(botToken = "bot-token", chatId = "42"),
            message = "ping",
            callFactory = client
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("Telegram API error 400"))
    }

    @Test
    fun sendMessage_returnsFailureWhenSettingsIncomplete() {
        val result = TelegramSender.sendMessage(
            settings = AppSettings(botToken = "", chatId = "42"),
            message = "ping"
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("Bot token or chat ID is missing"))
    }

    @Test
    fun sendMessage_returnsFailureWhenBothFieldsEmpty() {
        val result = TelegramSender.sendMessage(
            settings = AppSettings(),
            message = "ping"
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun sendMessage_sendsCorrectFormBody() {
        var capturedBody: String? = null

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val buffer = Buffer()
                chain.request().body?.writeTo(buffer)
                capturedBody = buffer.readUtf8()
                Response.Builder()
                    .code(200)
                    .message("OK")
                    .protocol(Protocol.HTTP_1_1)
                    .request(chain.request())
                    .body("""{"ok":true}""".toResponseBody())
                    .build()
            }
            .build()

        TelegramSender.sendMessage(
            settings = AppSettings(botToken = "tok", chatId = "123"),
            message = "hello world",
            callFactory = client
        )

        assertTrue(capturedBody!!.contains("chat_id=123"))
        assertTrue(capturedBody!!.contains("text=hello"))
    }

    @Test
    fun sendMessage_urlEncodesSpecialCharacters() {
        var capturedBody: String? = null

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val buffer = Buffer()
                chain.request().body?.writeTo(buffer)
                capturedBody = buffer.readUtf8()
                Response.Builder()
                    .code(200)
                    .message("OK")
                    .protocol(Protocol.HTTP_1_1)
                    .request(chain.request())
                    .body("""{"ok":true}""".toResponseBody())
                    .build()
            }
            .build()

        TelegramSender.sendMessage(
            settings = AppSettings(botToken = "tok", chatId = "42"),
            message = "hello & goodbye",
            callFactory = client
        )

        assertFalse("Body should URL-encode &", capturedBody!!.contains("&goodbye"))
        assertTrue(capturedBody!!.contains("text=hello"))
    }

    @Test
    fun sendMessage_includesBotTokenInUrl() {
        var capturedUrl: String? = null

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                capturedUrl = chain.request().url.toString()
                Response.Builder()
                    .code(200)
                    .message("OK")
                    .protocol(Protocol.HTTP_1_1)
                    .request(chain.request())
                    .body("""{"ok":true}""".toResponseBody())
                    .build()
            }
            .build()

        TelegramSender.sendMessage(
            settings = AppSettings(botToken = "123:ABC-def", chatId = "42"),
            message = "test",
            callFactory = client
        )

        assertEquals(
            "https://api.telegram.org/bot123:ABC-def/sendMessage",
            capturedUrl
        )
    }
}
