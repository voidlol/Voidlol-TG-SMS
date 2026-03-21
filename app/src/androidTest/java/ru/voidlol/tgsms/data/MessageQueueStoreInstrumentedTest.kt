package ru.voidlol.tgsms.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MessageQueueStoreInstrumentedTest {

    private lateinit var store: MessageQueueStore

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        store = MessageQueueStore(context)
        store.clear()
    }

    @After
    fun tearDown() {
        store.clear()
    }

    @Test
    fun readAll_returnsEmptyOnFreshStore() {
        assertTrue(store.readAll().isEmpty())
    }

    @Test
    fun add_andReadAll_returnsMessage() {
        store.add("hello")
        val messages = store.readAll()
        assertEquals(1, messages.size)
        assertEquals("hello", messages[0].text)
    }

    @Test
    fun add_assignsUniqueIds() {
        val m1 = store.add("first")
        val m2 = store.add("second")
        assertTrue(m1.id != m2.id)
    }

    @Test
    fun add_multiple_preservesOrder() {
        store.add("first")
        store.add("second")
        store.add("third")
        val messages = store.readAll()
        assertEquals(3, messages.size)
        assertEquals("first", messages[0].text)
        assertEquals("second", messages[1].text)
        assertEquals("third", messages[2].text)
    }

    @Test
    fun remove_deletesSpecificMessage() {
        store.add("keep")
        val toRemove = store.add("remove me")
        store.add("also keep")

        store.remove(toRemove.id)

        val remaining = store.readAll()
        assertEquals(2, remaining.size)
        assertEquals("keep", remaining[0].text)
        assertEquals("also keep", remaining[1].text)
    }

    @Test
    fun remove_nonExistentId_doesNothing() {
        store.add("hello")
        store.remove("non-existent-id")
        assertEquals(1, store.readAll().size)
    }

    @Test
    fun clear_removesAllMessages() {
        store.add("one")
        store.add("two")
        store.clear()
        assertTrue(store.readAll().isEmpty())
    }

    @Test
    fun persistence_survivesNewInstance() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        store.add("persistent")

        val newStore = MessageQueueStore(context)
        val messages = newStore.readAll()
        assertEquals(1, messages.size)
        assertEquals("persistent", messages[0].text)
    }

    @Test
    fun add_setsTimestamp() {
        val before = System.currentTimeMillis()
        val message = store.add("timed")
        val after = System.currentTimeMillis()

        assertTrue(message.timestamp in before..after)
    }
}
