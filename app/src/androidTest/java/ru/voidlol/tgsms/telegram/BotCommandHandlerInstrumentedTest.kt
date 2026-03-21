package ru.voidlol.tgsms.telegram

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BotCommandHandlerInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    // ── Command routing ──────────────────────────────────

    @Test
    fun handle_pingReturnsPong() {
        val result = BotCommandHandler.handle(context, "/ping")
        assertNotNull(result)
        assertTrue(result!!.startsWith("Pong!"))
    }

    @Test
    fun handle_helpReturnsCommandList() {
        val result = BotCommandHandler.handle(context, "/help")
        assertNotNull(result)
        assertTrue(result!!.contains("/ping"))
        assertTrue(result.contains("/status"))
        assertTrue(result.contains("/log"))
        assertTrue(result.contains("/help"))
    }

    @Test
    fun handle_startReturnsSameAsHelp() {
        val help = BotCommandHandler.handle(context, "/help")
        val start = BotCommandHandler.handle(context, "/start")
        assertEquals(help, start)
    }

    @Test
    fun handle_statusContainsExpectedSections() {
        val result = BotCommandHandler.handle(context, "/status")
        assertNotNull(result)
        assertTrue(result!!.contains("Battery:"))
        assertTrue(result.contains("Network:"))
        assertTrue(result.contains("Uptime:"))
        assertTrue(result.contains("Pending:"))
        assertTrue(result.contains("Device:"))
        assertTrue(result.contains("Android:"))
    }

    @Test
    fun handle_logWithNoEntriesReturnsEmptyMessage() {
        val result = BotCommandHandler.handle(context, "/log")
        assertNotNull(result)
        assertTrue(result!!.contains("No forwarded messages yet"))
    }

    @Test
    fun handle_unknownCommandReturnsError() {
        val result = BotCommandHandler.handle(context, "/foobar")
        assertNotNull(result)
        assertTrue(result!!.contains("Unknown command"))
        assertTrue(result.contains("/foobar"))
    }

    @Test
    fun handle_nonCommandReturnsNull() {
        assertNull(BotCommandHandler.handle(context, "hello"))
        assertNull(BotCommandHandler.handle(context, "just some text"))
    }

    @Test
    fun handle_emptyTextReturnsNull() {
        assertNull(BotCommandHandler.handle(context, ""))
        assertNull(BotCommandHandler.handle(context, "   "))
    }

    // ── Case insensitivity ───────────────────────────────

    @Test
    fun handle_isCaseInsensitive() {
        val lower = BotCommandHandler.handle(context, "/ping")
        val upper = BotCommandHandler.handle(context, "/PING")
        val mixed = BotCommandHandler.handle(context, "/Ping")

        assertNotNull(lower)
        assertNotNull(upper)
        assertNotNull(mixed)
        assertTrue(lower!!.startsWith("Pong!"))
        assertTrue(upper!!.startsWith("Pong!"))
        assertTrue(mixed!!.startsWith("Pong!"))
    }

    // ── Extra arguments are ignored ──────────────────────

    @Test
    fun handle_ignoresExtraArguments() {
        val result = BotCommandHandler.handle(context, "/ping extra args here")
        assertNotNull(result)
        assertTrue(result!!.startsWith("Pong!"))
    }

    // ── Leading/trailing whitespace ──────────────────────

    @Test
    fun handle_trimsWhitespace() {
        val result = BotCommandHandler.handle(context, "  /ping  ")
        assertNotNull(result)
        assertTrue(result!!.startsWith("Pong!"))
    }
}
