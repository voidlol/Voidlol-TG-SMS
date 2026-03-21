package ru.voidlol.tgsms.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ForwardedMessageLog(context: Context) {

    private val file = File(context.filesDir, LOG_FILE)

    @Synchronized
    fun add(text: String) {
        val entries = readAll().toMutableList()
        entries.add(LogEntry(text = text, timestamp = System.currentTimeMillis()))
        if (entries.size > MAX_ENTRIES) {
            entries.subList(0, entries.size - MAX_ENTRIES).clear()
        }
        writeToDisk(entries)
    }

    @Synchronized
    fun readAll(): List<LogEntry> {
        if (!file.exists()) return emptyList()
        return runCatching {
            val array = JSONArray(file.readText())
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                LogEntry(
                    text = obj.getString("text"),
                    timestamp = obj.getLong("timestamp")
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun writeToDisk(entries: List<LogEntry>) {
        val array = JSONArray()
        for (entry in entries) {
            array.put(JSONObject().apply {
                put("text", entry.text)
                put("timestamp", entry.timestamp)
            })
        }
        val temp = File(file.parent, "${file.name}.tmp")
        temp.writeText(array.toString())
        temp.renameTo(file)
    }

    data class LogEntry(val text: String, val timestamp: Long)

    companion object {
        private const val LOG_FILE = "forwarded_log.json"
        private const val MAX_ENTRIES = 30
    }
}
