package ru.voidlol.tgsms.util

import ru.voidlol.tgsms.R

import android.content.Context
import android.os.Build

object DeviceInfoFormatter {
    fun appName(context: Context): String = context.getString(R.string.app_name).trim()

    fun deviceName(): String {
        return combineDeviceName(Build.MANUFACTURER, Build.MODEL)
    }

    internal fun combineDeviceName(manufacturer: String?, model: String?): String {
        val cleanManufacturer = manufacturer.orEmpty().trim()
        val cleanModel = model.orEmpty().trim()
        return when {
            cleanManufacturer.isBlank() && cleanModel.isBlank() -> "Android device"
            cleanManufacturer.isBlank() -> cleanModel
            cleanModel.isBlank() -> cleanManufacturer
            cleanModel.startsWith(cleanManufacturer, ignoreCase = true) -> cleanModel
            else -> "$cleanManufacturer $cleanModel"
        }
    }
}
