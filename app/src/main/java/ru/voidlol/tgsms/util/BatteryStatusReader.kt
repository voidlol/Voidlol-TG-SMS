package ru.voidlol.tgsms.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

object BatteryStatusReader {
    fun readBatteryPercent(context: Context): Int? {
        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return null
        return extractPercent(
            level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1),
            scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        )
    }

    fun extractPercent(level: Int, scale: Int): Int? {
        if (level < 0 || scale <= 0) {
            return null
        }
        return ((level.toFloat() / scale.toFloat()) * 100).toInt()
            .coerceIn(0, 100)
    }
}
