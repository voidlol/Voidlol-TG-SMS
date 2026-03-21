package ru.voidlol.tgsms.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryStatusReaderTest {
    @Test
    fun extractPercent_returnsRoundedDownPercent() {
        assertEquals(14, BatteryStatusReader.extractPercent(level = 14, scale = 100))
        assertEquals(50, BatteryStatusReader.extractPercent(level = 2, scale = 4))
    }

    @Test
    fun extractPercent_rejectsInvalidValues() {
        assertNull(BatteryStatusReader.extractPercent(level = -1, scale = 100))
        assertNull(BatteryStatusReader.extractPercent(level = 10, scale = 0))
    }

    @Test
    fun extractPercent_clampsValuesToValidRange() {
        assertEquals(100, BatteryStatusReader.extractPercent(level = 110, scale = 100))
        assertEquals(0, BatteryStatusReader.extractPercent(level = 0, scale = 100))
    }
}
