package ru.voidlol.tgsms.telegram

import ru.voidlol.tgsms.data.AppSettingsStore

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import androidx.core.content.edit

object TelegramBotPoller {

    private const val POLL_TIMEOUT = 60
    private const val PREFS_NAME = "bot_poller"
    private const val KEY_OFFSET = "offset"

    @Volatile
    private var running = false

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout((POLL_TIMEOUT + 15).toLong(), TimeUnit.SECONDS)
        .build()

    suspend fun startPolling(context: Context) {
        if (running) return
        running = true

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var offset = prefs.getLong(KEY_OFFSET, 0)

        while (running) {
            try {
                val settings = AppSettingsStore(context).load()
                if (!settings.isComplete) {
                    delay(60_000)
                    continue
                }

                if (offset == 0L) {
                    offset = skipOldUpdates(settings.botToken)
                    prefs.edit { putLong(KEY_OFFSET, offset) }
                }

                val updates = getUpdates(settings.botToken, offset, POLL_TIMEOUT)
                for (update in updates) {
                    if (matchesChatId(update.chatId, settings.chatId)) {
                        val response = BotCommandHandler.handle(context, update.text)
                        if (response != null) {
                            TelegramSender.sendMessage(settings, response)
                        }
                    }
                    offset = update.updateId + 1
                    prefs.edit { putLong(KEY_OFFSET, offset) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                delay(30_000)
            }
        }
    }

    fun stop() {
        running = false
    }

    private fun skipOldUpdates(botToken: String): Long {
        val updates = getUpdates(botToken, 0, timeout = 0)
        return if (updates.isNotEmpty()) updates.last().updateId + 1 else 0
    }

    private fun getUpdates(botToken: String, offset: Long, timeout: Int): List<TelegramUpdate> {
        val url = "https://api.telegram.org/bot${botToken}/getUpdates".toHttpUrl()
            .newBuilder()
            .addQueryParameter("offset", offset.toString())
            .addQueryParameter("timeout", timeout.toString())
            .addQueryParameter("allowed_updates", "[\"message\"]")
            .build()

        val request = Request.Builder().url(url).get().build()
        val response = client.newCall(request).execute()
        val json = response.use { it.body?.string() } ?: return emptyList()
        return parseUpdates(json)
    }

    internal fun matchesChatId(messageChatId: Long, settingsChatId: String): Boolean {
        return messageChatId.toString() == settingsChatId.trim()
    }

    internal fun parseUpdates(json: String): List<TelegramUpdate> {
        val root = JSONObject(json)
        if (!root.optBoolean("ok", false)) return emptyList()

        val result = root.optJSONArray("result") ?: return emptyList()
        return (0 until result.length()).mapNotNull { i ->
            val obj = result.getJSONObject(i)
            val message = obj.optJSONObject("message") ?: return@mapNotNull null
            val chat = message.optJSONObject("chat") ?: return@mapNotNull null
            TelegramUpdate(
                updateId = obj.getLong("update_id"),
                chatId = chat.getLong("id"),
                text = message.optString("text", "")
            )
        }
    }
}

data class TelegramUpdate(
    val updateId: Long,
    val chatId: Long,
    val text: String
)
