package ru.voidlol.tgsms.util

import ru.voidlol.tgsms.R

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat

object PhoneMetadataResolver {
    fun resolveSimPhoneNumber(context: Context, intent: Intent): String {
        val defaultLabel = context.getString(R.string.sim_unknown)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return defaultLabel
        }

        val subscriptionId = extractSubscriptionId(intent) ?: return defaultLabel

        val subscriptionManager = context.getSystemService(SubscriptionManager::class.java) ?: return defaultLabel
        val info = try {
            subscriptionManager.getActiveSubscriptionInfo(subscriptionId)
        } catch (_: SecurityException) {
            null
        } ?: return defaultLabel

        val number = info.number?.trim().orEmpty()
        return when {
            number.isNotBlank() -> number
            !info.displayName.isNullOrBlank() -> info.displayName.toString()
            else -> defaultLabel
        }
    }

    fun resolveSenderLabel(context: Context, senderNumber: String?): String {
        val fallback = senderNumber?.trim().orEmpty().ifBlank { context.getString(R.string.sender_unknown) }
        if (senderNumber.isNullOrBlank()) {
            return fallback
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return fallback
        }

        val uri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI.buildUpon()
            .appendPath(senderNumber)
            .build()

        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val displayName = cursor.getString(0)?.trim().orEmpty()
                    if (displayName.isNotBlank()) {
                        return@runCatching "$senderNumber\\$displayName"
                    }
                }
                fallback
            } ?: fallback
        }.getOrElse { fallback }
    }

    private fun extractSubscriptionId(intent: Intent): Int? {
        val keys = listOf("subscription", "subscription_id", "android.telephony.extra.SUBSCRIPTION_INDEX")
        for (key in keys) {
            val value = intent.getIntExtra(key, SubscriptionManager.INVALID_SUBSCRIPTION_ID)
            if (value != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                return value
            }
        }
        return null
    }
}
