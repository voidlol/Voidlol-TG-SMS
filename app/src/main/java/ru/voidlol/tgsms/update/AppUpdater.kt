package ru.voidlol.tgsms.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class AppUpdater(
    private val context: Context,
    private val httpClient: OkHttpClient = OkHttpClient()
) {
    fun currentVersionName(): String = packageInfo().versionName.orEmpty()

    fun canRequestPackageInstalls(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()
    }

    fun createUnknownSourcesSettingsIntent(): Intent {
        return Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun createInstallIntent(apkFile: File): Intent {
        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, APK_MIME_TYPE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    suspend fun checkForUpdate(): AppUpdateInfo? {
        val request = Request.Builder()
            .url(UPDATE_URL)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Update check failed with HTTP ${response.code}")
            }

            val body = response.body.string()
            val updateInfo = AppUpdateJsonParser.parse(body)

            return updateInfo.takeIf { it.versionCode > currentVersionCode() }
        }
    }

    suspend fun downloadUpdate(
        updateInfo: AppUpdateInfo,
        onProgress: (Int) -> Unit = {}
    ): File {
        val request = Request.Builder()
            .url(updateInfo.apkUrl)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Update download failed with HTTP ${response.code}")
            }

            val body = response.body
            val targetDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apkFile = File(targetDir, "VoidlolTGSMS-${updateInfo.versionName}.apk")
            val totalBytes = body.contentLength()

            body.byteStream().use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var downloadedBytes = 0L

                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break

                        output.write(buffer, 0, read)
                        downloadedBytes += read

                        if (totalBytes > 0) {
                            val progress = ((downloadedBytes * 100) / totalBytes).toInt()
                            onProgress(progress.coerceIn(0, 100))
                        }
                    }

                    output.flush()
                }
            }

            onProgress(100)
            return apkFile
        }
    }

    private fun currentVersionCode(): Int {
        return PackageInfoCompat.getLongVersionCode(packageInfo()).toInt()
    }

    private fun packageInfo() = context.packageManager.getPackageInfo(context.packageName, 0)

    companion object {
        private const val UPDATE_URL = "https://voidlol.github.io/Voidlol-TG-SMS/update.json"
        private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    }
}
