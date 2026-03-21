package ru.voidlol.tgsms.util

object MessageFormatter {
    fun smsMessage(simPhoneNumber: String, senderLabel: String, text: String): String {
        return "[SIM: ${sanitize(simPhoneNumber)}]\n[FROM: ${sanitize(senderLabel)}]\n\n${text.trim()}"
    }

    private fun sanitize(value: String): String {
        return value.replace("\n", " ").trim().ifBlank { "unknown" }
    }
}
