package ru.voidlol.tgsms.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.core.content.edit

class AppSettingsStore(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (_: Exception) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun load(): AppSettings = AppSettings(
        botToken = prefs.getString(KEY_BOT_TOKEN, "").orEmpty(),
        chatId = prefs.getString(KEY_CHAT_ID, "").orEmpty(),
        batteryAlertThresholdPercent = prefs.getInt(KEY_BATTERY_ALERT_THRESHOLD, DEFAULT_BATTERY_ALERT_THRESHOLD)
    )

    fun save(settings: AppSettings) {
        prefs.edit {
            putString(KEY_BOT_TOKEN, settings.botToken.trim())
                .putString(KEY_CHAT_ID, settings.chatId.trim())
                .putInt(
                    KEY_BATTERY_ALERT_THRESHOLD,
                    settings.batteryAlertThresholdPercent.coerceIn(
                        MIN_BATTERY_ALERT_THRESHOLD,
                        MAX_BATTERY_ALERT_THRESHOLD
                    )
                )
        }
    }

    companion object {
        private const val PREFS_NAME = "telegram_forwarder_settings"
        private const val KEY_BOT_TOKEN = "bot_token"
        private const val KEY_CHAT_ID = "chat_id"
        private const val KEY_BATTERY_ALERT_THRESHOLD = "battery_alert_threshold"
        const val DEFAULT_BATTERY_ALERT_THRESHOLD = 30
        const val MIN_BATTERY_ALERT_THRESHOLD = 10
        const val MAX_BATTERY_ALERT_THRESHOLD = 60
    }
}
