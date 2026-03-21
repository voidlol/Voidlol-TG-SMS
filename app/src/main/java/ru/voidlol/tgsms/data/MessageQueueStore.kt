package ru.voidlol.tgsms.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class MessageQueueStore(context: Context) {

    private val file = File(context.filesDir, QUEUE_FILE)

    @Synchronized
    fun add(text: String): PendingMessage {
        val message = PendingMessage(
            id = UUID.randomUUID().toString(),
            text = text,
            timestamp = System.currentTimeMillis()
        )
        val messages = readAll().toMutableList()
        messages.add(message)
        writeToDisk(messages)
        return message
    }

    @Synchronized
    fun readAll(): List<PendingMessage> {
        if (!file.exists()) return emptyList()
        return runCatching {
            val json = file.readText()
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                PendingMessage(
                    id = obj.getString("id"),
                    text = obj.getString("text"),
                    timestamp = obj.getLong("timestamp")
                )
            }
        }.getOrDefault(emptyList())
    }

    @Synchronized
    fun remove(id: String) {
        val messages = readAll().filterNot { it.id == id }
        writeToDisk(messages)
    }

    @Synchronized
    fun clear() {
        file.delete()
    }

    private fun writeToDisk(messages: List<PendingMessage>) {
        val array = JSONArray()
        for (msg in messages) {
            array.put(
                JSONObject().apply {
                    put("id", msg.id)
                    put("text", msg.text)
                    put("timestamp", msg.timestamp)
                }
            )
        }
        val temp = File(file.parent, "${file.name}.tmp")
        temp.writeText(array.toString())
        temp.renameTo(file)
    }

    companion object {
        private const val QUEUE_FILE = "pending_messages.json"
    }
}
