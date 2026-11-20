package de.knurl.data.local

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * One value kept as a JSON file in app storage. Writes go to a temp file first and are then
 * renamed, so a crash mid-write never leaves a half-written file behind. A file that cannot be
 * read is kept as `.broken` rather than overwritten, so nothing is lost silently.
 */
class JsonFileStore<T>(
    private val file: File,
    private val serializer: KSerializer<T>,
    private val json: Json = DefaultJson,
) {
    private val lock = Mutex()

    suspend fun read(): T? = withContext(Dispatchers.IO) {
        lock.withLock {
            if (!file.exists()) return@withLock null
            runCatching { json.decodeFromString(serializer, file.readText()) }
                .onFailure { error ->
                    Log.e(TAG, "Could not read ${file.name}; keeping it as .broken", error)
                    file.renameTo(File(file.parentFile, "${file.name}.broken"))
                }
                .getOrNull()
        }
    }

    suspend fun write(value: T) = withContext(Dispatchers.IO) {
        lock.withLock {
            val temp = File(file.parentFile, "${file.name}.tmp")
            temp.writeText(json.encodeToString(serializer, value))
            if (!temp.renameTo(file)) {
                file.delete()
                temp.renameTo(file)
            }
        }
    }

    companion object {
        private const val TAG = "JsonFileStore"
        val DefaultJson = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}
