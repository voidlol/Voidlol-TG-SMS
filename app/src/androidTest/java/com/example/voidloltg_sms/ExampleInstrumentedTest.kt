package com.example.voidloltg_sms

import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun appSettings_areMarkedCompleteOnlyWhenBothValuesExist() {
        assertFalse(AppSettings(botToken = "", chatId = "1").isComplete)
        assertFalse(AppSettings(botToken = "1", chatId = "").isComplete)
        assertTrue(AppSettings(botToken = "1", chatId = "2").isComplete)
    }
}
