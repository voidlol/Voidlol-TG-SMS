package ru.voidlol.tgsms.telegram

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramBotPollerTest {

    // ── matchesChatId ────────────────────────────────────

    @Test
    fun matchesChatId_matchesPositiveId() {
        assertTrue(TelegramBotPoller.matchesChatId(123456789, "123456789"))
    }

    @Test
    fun matchesChatId_matchesNegativeGroupId() {
        assertTrue(TelegramBotPoller.matchesChatId(-1001234567890, "-1001234567890"))
    }

    @Test
    fun matchesChatId_trimsWhitespace() {
        assertTrue(TelegramBotPoller.matchesChatId(42, "  42  "))
    }

    @Test
    fun matchesChatId_rejectsMismatch() {
        assertFalse(TelegramBotPoller.matchesChatId(123, "456"))
    }

    @Test
    fun matchesChatId_rejectsEmpty() {
        assertFalse(TelegramBotPoller.matchesChatId(123, ""))
    }

    // ── parseUpdates ─────────────────────────────────────

    @Test
    fun parseUpdates_parsesValidResponse() {
        val json = """
            {
                "ok": true,
                "result": [
                    {
                        "update_id": 100,
                        "message": {
                            "message_id": 1,
                            "chat": { "id": 42 },
                            "text": "/ping"
                        }
                    }
                ]
            }
        """.trimIndent()

        val updates = TelegramBotPoller.parseUpdates(json)
        assertEquals(1, updates.size)
        assertEquals(100L, updates[0].updateId)
        assertEquals(42L, updates[0].chatId)
        assertEquals("/ping", updates[0].text)
    }

    @Test
    fun parseUpdates_parsesMultipleUpdates() {
        val json = """
            {
                "ok": true,
                "result": [
                    {
                        "update_id": 100,
                        "message": { "message_id": 1, "chat": { "id": 42 }, "text": "/ping" }
                    },
                    {
                        "update_id": 101,
                        "message": { "message_id": 2, "chat": { "id": 42 }, "text": "/status" }
                    }
                ]
            }
        """.trimIndent()

        val updates = TelegramBotPoller.parseUpdates(json)
        assertEquals(2, updates.size)
        assertEquals("/ping", updates[0].text)
        assertEquals("/status", updates[1].text)
    }

    @Test
    fun parseUpdates_returnsEmptyForNotOk() {
        val json = """{"ok": false, "result": []}"""
        assertTrue(TelegramBotPoller.parseUpdates(json).isEmpty())
    }

    @Test
    fun parseUpdates_returnsEmptyForEmptyResult() {
        val json = """{"ok": true, "result": []}"""
        assertTrue(TelegramBotPoller.parseUpdates(json).isEmpty())
    }

    @Test
    fun parseUpdates_skipsUpdatesWithoutMessage() {
        val json = """
            {
                "ok": true,
                "result": [
                    { "update_id": 100 },
                    {
                        "update_id": 101,
                        "message": { "message_id": 1, "chat": { "id": 42 }, "text": "hello" }
                    }
                ]
            }
        """.trimIndent()

        val updates = TelegramBotPoller.parseUpdates(json)
        assertEquals(1, updates.size)
        assertEquals(101L, updates[0].updateId)
    }

    @Test
    fun parseUpdates_skipsUpdatesWithoutChat() {
        val json = """
            {
                "ok": true,
                "result": [
                    {
                        "update_id": 100,
                        "message": { "message_id": 1, "text": "no chat" }
                    }
                ]
            }
        """.trimIndent()

        assertTrue(TelegramBotPoller.parseUpdates(json).isEmpty())
    }

    @Test
    fun parseUpdates_handlesNegativeChatId() {
        val json = """
            {
                "ok": true,
                "result": [
                    {
                        "update_id": 100,
                        "message": { "message_id": 1, "chat": { "id": -1001234567890 }, "text": "/help" }
                    }
                ]
            }
        """.trimIndent()

        val updates = TelegramBotPoller.parseUpdates(json)
        assertEquals(-1001234567890L, updates[0].chatId)
    }

    @Test
    fun parseUpdates_defaultsToEmptyTextWhenMissing() {
        val json = """
            {
                "ok": true,
                "result": [
                    {
                        "update_id": 100,
                        "message": { "message_id": 1, "chat": { "id": 42 } }
                    }
                ]
            }
        """.trimIndent()

        val updates = TelegramBotPoller.parseUpdates(json)
        assertEquals("", updates[0].text)
    }
}
