package com.example.voidloltg_sms

import android.content.Context

class AppSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): AppSettings = AppSettings(
        botToken = prefs.getString(KEY_BOT_TOKEN, "").orEmpty(),
        chatId = prefs.getString(KEY_CHAT_ID, "").orEmpty()
    )

    fun save(settings: AppSettings) {
        prefs.edit()
            .putString(KEY_BOT_TOKEN, settings.botToken.trim())
            .putString(KEY_CHAT_ID, settings.chatId.trim())
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "telegram_forwarder_settings"
        private const val KEY_BOT_TOKEN = "bot_token"
        private const val KEY_CHAT_ID = "chat_id"
    }
}
