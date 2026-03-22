package ru.voidlol.tgsms.data

import android.content.Context
import androidx.core.content.edit

class BatteryAlertStateStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun lastAlertedPercent(): Int? {
        val value = prefs.getInt(KEY_LAST_ALERTED_PERCENT, NO_VALUE)
        return value.takeIf { it != NO_VALUE }
    }

    fun setLastAlertedPercent(percent: Int) {
        prefs.edit { putInt(KEY_LAST_ALERTED_PERCENT, percent) }
    }

    fun clear() {
        prefs.edit { remove(KEY_LAST_ALERTED_PERCENT) }
    }

    companion object {
        private const val PREFS_NAME = "battery_alert_state"
        private const val KEY_LAST_ALERTED_PERCENT = "last_alerted_percent"
        private const val NO_VALUE = -1
    }
}
