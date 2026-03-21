package ru.voidlol.tgsms.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TestMessageFormatterTest {

    @Test
    fun format_usesAppName() {
        assertEquals(
            "[MyApp][test] Telegram delivery is working",
            TestMessageFormatter.format("MyApp")
        )
    }

    @Test
    fun format_fallsBackToAppWhenBlank() {
        assertEquals(
            "[App][test] Telegram delivery is working",
            TestMessageFormatter.format("")
        )
        assertEquals(
            "[App][test] Telegram delivery is working",
            TestMessageFormatter.format("   ")
        )
    }

    @Test
    fun format_sanitizesNewlines() {
        assertEquals(
            "[My App][test] Telegram delivery is working",
            TestMessageFormatter.format("My\nApp")
        )
    }

    @Test
    fun format_trimsWhitespace() {
        assertEquals(
            "[TG-SMS][test] Telegram delivery is working",
            TestMessageFormatter.format("  TG-SMS  ")
        )
    }
}
