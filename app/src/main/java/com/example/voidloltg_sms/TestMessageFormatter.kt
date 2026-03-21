package com.example.voidloltg_sms

object TestMessageFormatter {
    fun format(appName: String): String {
        val cleanName = appName.replace("\n", " ").trim().ifBlank { "App" }
        return "[$cleanName][test] Telegram delivery is working"
    }
}
