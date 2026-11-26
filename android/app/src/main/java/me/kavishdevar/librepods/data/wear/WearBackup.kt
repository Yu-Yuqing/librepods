package me.kavishdevar.librepods.data.wear

import android.util.JsonReader
import android.util.JsonWriter
import java.io.InputStream
import java.io.OutputStream

internal object WearBackup {
    private const val FORMAT = "librepods-wear-v1"

    fun write(output: OutputStream, sessions: List<WearSession>) {
        JsonWriter(output.bufferedWriter()).use { json ->
            json.beginObject().name("format").value(FORMAT).name("sessions").beginArray()
            for (s in sessions) {
                json.beginArray().value(s.startMillis).value(s.endMillis).endArray()
            }
            json.endArray().endObject()
        }
    }

    fun read(input: InputStream): List<WearSession> = JsonReader(input.bufferedReader()).use { json ->
        var format: String? = null
        var sessions: List<WearSession>? = null
        json.beginObject()
        while (json.hasNext()) {
            when (json.nextName()) {
                "format" -> {
                    require(format == null)
                    format = json.nextString()
                }
                "sessions" -> {
                    require(sessions == null)
                    json.beginArray()
                    sessions = buildList {
                        while (json.hasNext()) {
                            require(size < 1_000_000)
                            json.beginArray()
                            val start = json.nextLong()
                            val end = json.nextLong()
                            require(start >= 0 && end > start && end < 253402300800000L)
                            add(WearSession(0, start, end))
                            json.endArray()
                        }
                    }
                    json.endArray()
                }
                else -> json.skipValue()
            }
        }
        json.endObject()
        require(json.peek() == android.util.JsonToken.END_DOCUMENT && format == FORMAT)
        requireNotNull(sessions)
    }
}
