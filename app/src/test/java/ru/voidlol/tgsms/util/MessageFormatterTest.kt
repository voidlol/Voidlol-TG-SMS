package ru.voidlol.tgsms.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MessageFormatterTest {
    @Test
    fun smsMessage_usesRequestedFormat() {
        val message = MessageFormatter.smsMessage(
            simPhoneNumber = "+15550001",
            senderLabel = "+1999000\\Bank",
            text = "Code: 1234"
        )

        assertEquals("[+15550001][+1999000\\Bank]\n\nCode: 1234", message)
    }

    @Test
    fun smsMessage_sanitizesBlankValuesAndText() {
        val message = MessageFormatter.smsMessage(
            simPhoneNumber = " ",
            senderLabel = "\n",
            text = "  Hello world  "
        )

        assertEquals("[unknown][unknown]\n\nHello world", message)
    }

    @Test
    fun testMessage_usesRealAppNameSlot() {
        assertEquals(
            "[Voidlol TG-SMS][test] Telegram delivery is working",
            TestMessageFormatter.format("Voidlol TG-SMS")
        )
    }

    @Test
    fun smsMessage_preservesSpecialCharactersInBody() {
        val message = MessageFormatter.smsMessage(
            simPhoneNumber = "+1234",
            senderLabel = "+5678",
            text = "Your code: 1234 & pin: 5678 <html>"
        )
        assertEquals("[+1234][+5678]\n\nYour code: 1234 & pin: 5678 <html>", message)
    }

    @Test
    fun smsMessage_handlesEmptyBody() {
        val message = MessageFormatter.smsMessage(
            simPhoneNumber = "+1234",
            senderLabel = "+5678",
            text = ""
        )
        assertEquals("[+1234][+5678]\n\n", message)
    }

    @Test
    fun smsMessage_sanitizesNewlinesInLabels() {
        val message = MessageFormatter.smsMessage(
            simPhoneNumber = "line1\nline2",
            senderLabel = "name\nwrap",
            text = "hi"
        )
        assertEquals("[line1 line2][name wrap]\n\nhi", message)
    }
}
