package ru.voidlol.tgsms.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdaterJsonParsingTest {
    @Test
    fun parseUpdateJson_returnsParsedMetadata() {
        val json = """
            {
              "versionCode": 10005,
              "versionName": "1.0.5",
              "apkUrl": "https://example.com/app.apk",
              "changelog": "Bug fixes",
              "publishedAt": "2026-03-22T10:00:00Z"
            }
        """.trimIndent()

        val result = AppUpdateJsonParser.parse(json)

        assertEquals(10005, result.versionCode)
        assertEquals("1.0.5", result.versionName)
        assertEquals("https://example.com/app.apk", result.apkUrl)
        assertEquals("Bug fixes", result.changelog)
        assertEquals("2026-03-22T10:00:00Z", result.publishedAt)
    }

    @Test
    fun parseUpdateJson_rejectsMissingVersionCode() {
        val json = """
            {
              "versionName": "1.0.5",
              "apkUrl": "https://example.com/app.apk"
            }
        """.trimIndent()

        val error = runCatching { AppUpdateJsonParser.parse(json) }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertEquals("Invalid update metadata: versionCode is missing", error?.message)
    }
}
