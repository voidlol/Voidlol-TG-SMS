package com.example.voidloltg_sms

import android.content.Context

object BatteryAlertFormatter {
    fun formatMessage(context: Context, batteryPercent: Int?): String {
        val deviceName = DeviceInfoFormatter.deviceName()
        return if (batteryPercent != null) {
            context.getString(R.string.battery_low_message, deviceName, batteryPercent)
        } else {
            context.getString(R.string.battery_low_message_unknown, deviceName)
        }
    }
}
