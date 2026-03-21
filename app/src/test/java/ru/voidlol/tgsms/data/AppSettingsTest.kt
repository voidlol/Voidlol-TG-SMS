package ru.voidlol.tgsms.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {

    @Test
    fun isComplete_trueWhenBothPresent() {
        assertTrue(AppSettings(botToken = "token", chatId = "123").isComplete)
    }

    @Test
    fun isComplete_falseWhenBotTokenBlank() {
        assertFalse(AppSettings(botToken = "", chatId = "123").isComplete)
    }

    @Test
    fun isComplete_falseWhenChatIdBlank() {
        assertFalse(AppSettings(botToken = "token", chatId = "").isComplete)
    }

    @Test
    fun isComplete_falseWhenBothBlank() {
        assertFalse(AppSettings(botToken = "", chatId = "").isComplete)
    }

    @Test
    fun isComplete_falseWhenWhitespaceOnly() {
        assertFalse(AppSettings(botToken = "   ", chatId = "123").isComplete)
        assertFalse(AppSettings(botToken = "token", chatId = "  \t  ").isComplete)
    }

    @Test
    fun defaults_areEmptyStrings() {
        val settings = AppSettings()
        assertEquals("", settings.botToken)
        assertEquals("", settings.chatId)
        assertFalse(settings.isComplete)
    }
}
