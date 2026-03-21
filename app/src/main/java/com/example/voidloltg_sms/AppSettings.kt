package com.example.voidloltg_sms

data class AppSettings(
    val botToken: String = "",
    val chatId: String = ""
) {
    val isComplete: Boolean
        get() = botToken.isNotBlank() && chatId.isNotBlank()
}
