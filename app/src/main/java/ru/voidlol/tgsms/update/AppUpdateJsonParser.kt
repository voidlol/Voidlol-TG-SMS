package ru.voidlol.tgsms.update

import org.json.JSONObject

internal object AppUpdateJsonParser {
    fun parse(json: String): AppUpdateInfo {
        val payload = JSONObject(json)
        val versionCode = payload.optInt("versionCode", -1)
        val versionName = payload.optString("versionName").trim()
        val apkUrl = payload.optString("apkUrl").trim()

        require(versionCode > 0) { "Invalid update metadata: versionCode is missing" }
        require(versionName.isNotBlank()) { "Invalid update metadata: versionName is missing" }
        require(apkUrl.isNotBlank()) { "Invalid update metadata: apkUrl is missing" }

        return AppUpdateInfo(
            versionCode = versionCode,
            versionName = versionName,
            apkUrl = apkUrl,
            changelog = payload.optString("changelog").trim(),
            publishedAt = payload.optString("publishedAt").trim()
        )
    }
}
