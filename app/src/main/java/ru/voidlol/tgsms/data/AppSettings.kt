package ru.voidlol.tgsms.data

data class AppSettings(
    val botToken: String = "",
    val chatId: String = ""
) {
    val isComplete: Boolean
        get() = botToken.isNotBlank() && chatId.isNotBlank()
}
