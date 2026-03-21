package ru.voidlol.tgsms.telegram

import ru.voidlol.tgsms.data.ForwardedMessageLog
import ru.voidlol.tgsms.data.MessageQueueStore
import ru.voidlol.tgsms.service.RelayService
import ru.voidlol.tgsms.util.DeviceInfoFormatter

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.SystemClock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object BotCommandHandler {

    fun handle(context: Context, text: String): String? {
        val command = text.trim().lowercase().split("\\s+".toRegex()).firstOrNull() ?: return null
        return when {
            command == "/ping" -> handlePing()
            command == "/status" -> handleStatus(context)
            command == "/log" -> handleLog(context)
            command == "/help" || command == "/start" -> handleHelp()
            command.startsWith("/") -> "Unknown command: $command\nType /help for available commands."
            else -> null
        }
    }

    private fun handlePing(): String {
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US).format(Date())
        return "Pong! $time"
    }

    private fun handleStatus(context: Context): String {
        val battery = readBatteryInfo(context)
        val network = readNetworkInfo(context)
        val uptime = formatUptime()
        val pending = MessageQueueStore(context).readAll().size
        val device = DeviceInfoFormatter.deviceName()

        return buildString {
            appendLine("TG-SMS Relay Status")
            appendLine("---")
            appendLine("Battery: $battery")
            appendLine("Network: $network")
            appendLine("Uptime: $uptime")
            appendLine("Pending: $pending message(s)")
            appendLine("Device: $device")
            append("Android: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
        }
    }

    private fun handleLog(context: Context): String {
        val entries = ForwardedMessageLog(context).readAll()
        if (entries.isEmpty()) {
            return "No forwarded messages yet."
        }

        val show = entries.takeLast(10)
        return buildString {
            appendLine("Recent forwards (${show.size} of ${entries.size}):")
            appendLine("---")
            for (entry in show) {
                val time = formatRelativeTime(entry.timestamp)
                appendLine("[$time] ${truncate(entry.text, 80)}")
            }
        }.trimEnd()
    }

    private fun handleHelp(): String {
        return buildString {
            appendLine("TG-SMS Bot Commands")
            appendLine("---")
            appendLine("/ping  -- Check if relay is alive")
            appendLine("/status  -- Battery, network, uptime")
            appendLine("/log  -- Recent forwarded messages")
            append("/help  -- Show this help")
        }
    }

    private fun readBatteryInfo(context: Context): String {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return "unknown"
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)

        val percent = if (level >= 0 && scale > 0) {
            "${((level.toFloat() / scale) * 100).toInt()}%"
        } else {
            "unknown"
        }

        val charging = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "wireless"
            else -> null
        }

        return if (charging != null) "$percent (charging via $charging)" else "$percent (discharging)"
    }

    private fun readNetworkInfo(context: Context): String {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return "unknown"
        val network = cm.activeNetwork ?: return "offline"
        val caps = cm.getNetworkCapabilities(network) ?: return "offline"

        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "connected"
        }
    }

    private fun formatUptime(): String {
        val serviceElapsed = RelayService.startedAtElapsed
        if (serviceElapsed == 0L) return "service not running"

        val millis = SystemClock.elapsedRealtime() - serviceElapsed
        val days = TimeUnit.MILLISECONDS.toDays(millis)
        val hours = TimeUnit.MILLISECONDS.toHours(millis) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60

        return when {
            days > 0 -> "${days}d ${hours}h ${minutes}m"
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }

    private fun formatRelativeTime(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)

        return when {
            minutes < 1 -> "just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            else -> SimpleDateFormat("MMM dd", Locale.US).format(Date(timestamp))
        }
    }

    private fun truncate(text: String, maxLen: Int): String {
        val oneLine = text.replace('\n', ' ')
        return if (oneLine.length <= maxLen) oneLine else oneLine.take(maxLen - 3) + "..."
    }
}
