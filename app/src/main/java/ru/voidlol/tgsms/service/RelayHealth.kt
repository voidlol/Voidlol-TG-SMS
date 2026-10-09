package ru.voidlol.tgsms.service

import ru.voidlol.tgsms.data.MessageQueue

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.content.edit

/**
 * Android revokes permissions of apps that are not opened for a few months ("pause app activity
 * if unused"), and OEM managers may revoke them too. Without RECEIVE_SMS the system simply stops
 * delivering SMS to the app, so report it to Telegram once instead of failing silently.
 */
object RelayHealth {

    private const val PREFS_NAME = "relay_health"
    private const val KEY_SMS_PERMISSION_ALERTED = "sms_permission_alerted"

    fun check(context: Context) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val granted = ContextCompat.checkSelfPermission(
            appContext, Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED

        val alerted = prefs.getBoolean(KEY_SMS_PERMISSION_ALERTED, false)
        when {
            !granted && !alerted -> {
                MessageQueue.enqueue(
                    appContext,
                    "⚠️ SMS permission was revoked — SMS are NOT being forwarded. " +
                        "Open the TG-SMS app on the phone and grant permissions again."
                )
                prefs.edit { putBoolean(KEY_SMS_PERMISSION_ALERTED, true) }
            }
            granted && alerted -> prefs.edit { putBoolean(KEY_SMS_PERMISSION_ALERTED, false) }
        }
    }
}
