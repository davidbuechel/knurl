package de.knurl.data.firebase

import com.google.firebase.Timestamp
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Maps the shared @Serializable models to Firestore documents and back, so the models stay free of
 * Firebase types. Timestamps written by the server come back as epoch milliseconds.
 */
object FirestoreJson {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun <T> encode(serializer: KSerializer<T>, value: T): Map<String, Any?> =
        (json.encodeToJsonElement(serializer, value) as JsonObject).toFirestore()

    fun <T> decode(serializer: KSerializer<T>, data: Map<String, Any?>): T =
        json.decodeFromJsonElement(serializer, data.toJson())

    private fun JsonObject.toFirestore(): Map<String, Any?> = mapValues { (_, v) -> v.toFirestore() }

    private fun JsonElement.toFirestore(): Any? = when (this) {
        JsonNull -> null
        is JsonObject -> toFirestore()
        is JsonArray -> map { it.toFirestore() }
        is JsonPrimitive -> when {
            isString -> content
            booleanOrNull != null -> booleanOrNull
            longOrNull != null -> longOrNull
            else -> doubleOrNull
        }
    }

    private fun Any?.toJson(): JsonElement = when (this) {
        null -> JsonNull
        is String -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is Timestamp -> JsonPrimitive(seconds * 1000 + nanoseconds / 1_000_000)
        is Map<*, *> -> JsonObject(entries.associate { (k, v) -> k.toString() to v.toJson() })
        is List<*> -> JsonArray(map { it.toJson() })
        else -> JsonPrimitive(toString())
    }
}
