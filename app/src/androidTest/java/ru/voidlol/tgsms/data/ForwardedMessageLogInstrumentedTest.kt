package ru.voidlol.tgsms.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ForwardedMessageLogInstrumentedTest {

    private lateinit var log: ForwardedMessageLog

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.filesDir, "forwarded_log.json").delete()
        log = ForwardedMessageLog(context)
    }

    @Test
    fun readAll_returnsEmptyOnFreshLog() {
        assertTrue(log.readAll().isEmpty())
    }

    @Test
    fun add_andReadAll_returnsEntry() {
        log.add("test message")
        val entries = log.readAll()
        assertEquals(1, entries.size)
        assertEquals("test message", entries[0].text)
    }

    @Test
    fun add_setsTimestamp() {
        val before = System.currentTimeMillis()
        log.add("timed")
        val after = System.currentTimeMillis()

        val entry = log.readAll()[0]
        assertTrue(entry.timestamp in before..after)
    }

    @Test
    fun add_preservesOrder() {
        log.add("first")
        log.add("second")
        log.add("third")
        val entries = log.readAll()
        assertEquals("first", entries[0].text)
        assertEquals("second", entries[1].text)
        assertEquals("third", entries[2].text)
    }

    @Test
    fun add_capsAtMaxEntries() {
        repeat(35) { i ->
            log.add("message $i")
        }
        val entries = log.readAll()
        assertEquals(30, entries.size)
        assertEquals("message 5", entries[0].text)
        assertEquals("message 34", entries[29].text)
    }

    @Test
    fun persistence_survivesNewInstance() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        log.add("persistent entry")

        val newLog = ForwardedMessageLog(context)
        val entries = newLog.readAll()
        assertEquals(1, entries.size)
        assertEquals("persistent entry", entries[0].text)
    }
}
