package ru.voidlol.tgsms.service

import android.content.Context
import androidx.core.content.edit

class RelayServiceStateStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun shouldRun(): Boolean = prefs.getBoolean(KEY_SHOULD_RUN, false)

    fun setShouldRun(value: Boolean) {
        prefs.edit { putBoolean(KEY_SHOULD_RUN, value) }
    }

    companion object {
        private const val PREFS_NAME = "relay_service_state"
        private const val KEY_SHOULD_RUN = "should_run"
    }
}
