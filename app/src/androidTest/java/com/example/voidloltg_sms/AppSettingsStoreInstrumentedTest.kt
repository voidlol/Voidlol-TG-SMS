package com.example.voidloltg_sms

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppSettingsStoreInstrumentedTest {
    @Test
    fun saveAndLoad_roundTripsSettings() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = AppSettingsStore(context)
        val settings = AppSettings(
            botToken = "test-bot-token",
            chatId = "test-chat-id"
        )

        store.save(settings)

        assertEquals(settings, store.load())
    }
}
