package ru.voidlol.tgsms.update

data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val changelog: String = "",
    val publishedAt: String = ""
)
