package com.example.voidloltg_sms

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceInfoFormatterTest {
    @Test
    fun combineDeviceName_avoidsRepeatingManufacturer() {
        assertEquals("Samsung SM-S911B", DeviceInfoFormatter.combineDeviceName("Samsung", "Samsung SM-S911B"))
    }

    @Test
    fun combineDeviceName_combinesDistinctParts() {
        assertEquals("Xiaomi 2312DRA50G", DeviceInfoFormatter.combineDeviceName("Xiaomi", "2312DRA50G"))
    }

    @Test
    fun combineDeviceName_handlesMissingValues() {
        assertEquals("Pixel 8", DeviceInfoFormatter.combineDeviceName("", "Pixel 8"))
        assertEquals("Google", DeviceInfoFormatter.combineDeviceName("Google", ""))
        assertEquals("Android device", DeviceInfoFormatter.combineDeviceName("", ""))
    }
}
