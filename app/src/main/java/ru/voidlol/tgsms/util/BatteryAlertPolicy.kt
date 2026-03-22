package ru.voidlol.tgsms.util

object BatteryAlertPolicy {
    fun shouldAlert(
        batteryPercent: Int?,
        isCharging: Boolean,
        thresholdPercent: Int,
        lastAlertedPercent: Int?
    ): Boolean {
        if (batteryPercent == null || isCharging) {
            return false
        }

        if (batteryPercent > thresholdPercent) {
            return false
        }

        return lastAlertedPercent == null || batteryPercent <= lastAlertedPercent - ALERT_STEP_PERCENT
    }

    fun shouldReset(
        batteryPercent: Int?,
        isCharging: Boolean,
        thresholdPercent: Int
    ): Boolean {
        return isCharging || (batteryPercent != null && batteryPercent > thresholdPercent)
    }

    private const val ALERT_STEP_PERCENT = 2
}
