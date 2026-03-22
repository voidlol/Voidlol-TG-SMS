package ru.voidlol.tgsms.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

object BatteryStatusReader {
    data class BatterySnapshot(
        val percent: Int?,
        val isCharging: Boolean
    )

    fun readBatteryPercent(context: Context): Int? {
        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return null
        return extractPercent(
            level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1),
            scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        )
    }

    fun readBatterySnapshot(context: Context): BatterySnapshot? {
        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return null
        return fromIntent(batteryStatus)
    }

    fun fromIntent(intent: Intent): BatterySnapshot {
        return BatterySnapshot(
            percent = extractPercent(
                level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1),
                scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            ),
            isCharging = isCharging(intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1))
        )
    }

    fun extractPercent(level: Int, scale: Int): Int? {
        if (level < 0 || scale <= 0) {
            return null
        }
        return ((level.toFloat() / scale.toFloat()) * 100).toInt()
            .coerceIn(0, 100)
    }

    fun isCharging(status: Int): Boolean {
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
    }
}
