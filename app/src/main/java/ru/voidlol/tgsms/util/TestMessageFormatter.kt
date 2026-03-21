package ru.voidlol.tgsms.util

object TestMessageFormatter {
    fun format(appName: String): String {
        val cleanName = appName.replace("\n", " ").trim().ifBlank { "App" }
        return "[$cleanName][test] Telegram delivery is working"
    }
}
