package ru.voidlol.tgsms.update

import android.content.Context
import androidx.core.content.edit
import org.json.JSONObject

class AppUpdateStateStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadAvailableUpdate(): AppUpdateInfo? {
        val raw = prefs.getString(KEY_AVAILABLE_UPDATE, null) ?: return null
        return runCatching { AppUpdateJsonParser.parse(raw) }.getOrNull()
    }

    fun saveAvailableUpdate(updateInfo: AppUpdateInfo?) {
        prefs.edit {
            if (updateInfo == null) {
                remove(KEY_AVAILABLE_UPDATE)
            } else {
                putString(
                    KEY_AVAILABLE_UPDATE,
                    JSONObject()
                        .put("versionCode", updateInfo.versionCode)
                        .put("versionName", updateInfo.versionName)
                        .put("apkUrl", updateInfo.apkUrl)
                        .put("changelog", updateInfo.changelog)
                        .put("publishedAt", updateInfo.publishedAt)
                        .toString()
                )
            }
        }
    }

    fun lastNotifiedVersionCode(): Int = prefs.getInt(KEY_LAST_NOTIFIED_VERSION_CODE, -1)

    fun setLastNotifiedVersionCode(versionCode: Int) {
        prefs.edit { putInt(KEY_LAST_NOTIFIED_VERSION_CODE, versionCode) }
    }

    companion object {
        private const val PREFS_NAME = "app_update_state"
        private const val KEY_AVAILABLE_UPDATE = "available_update"
        private const val KEY_LAST_NOTIFIED_VERSION_CODE = "last_notified_version_code"
    }
}
