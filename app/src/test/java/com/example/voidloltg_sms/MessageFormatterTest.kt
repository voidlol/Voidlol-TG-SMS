package com.example.voidloltg_sms

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

        assertEquals("[+15550001][+1999000\\Bank] Code: 1234", message)
    }

    @Test
    fun smsMessage_sanitizesBlankValuesAndText() {
        val message = MessageFormatter.smsMessage(
            simPhoneNumber = " ",
            senderLabel = "\n",
            text = "  Hello world  "
        )

        assertEquals("[unknown][unknown] Hello world", message)
    }

    @Test
    fun testMessage_usesRealAppNameSlot() {
        assertEquals(
            "[Voidlol TG-SMS][test] Telegram delivery is working",
            TestMessageFormatter.format("Voidlol TG-SMS")
        )
    }
}
