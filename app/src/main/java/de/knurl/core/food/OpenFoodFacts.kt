package de.knurl.core.food

import android.util.Log
import de.knurl.domain.model.Food
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Open Food Facts (openfoodfacts.org, open database, ODbL): products by barcode and by name.
 * Only the barcode or the search words are sent; nothing about the person. Products without
 * calories or protein per 100 g are left out, they can't be logged meaningfully.
 */
object OpenFoodFacts {

    private const val BASE = "https://world.openfoodfacts.org"
    /** The newer, faster search (search-a-licious). */
    private const val SEARCH = "https://search.openfoodfacts.org"
    private const val FIELDS = "code,product_name,product_name_de,product_name_en,brands,nutriments,serving_quantity"
    /** Open Food Facts asks apps to name themselves; nothing personal in it. */
    private const val USER_AGENT = "Knurl/0.1 (Android gym app)"

    private val json = Json { ignoreUnknownKeys = true }

    /** The product with this barcode, or null if unknown or not usable. Throws on network errors. */
    suspend fun byBarcode(barcode: String, german: Boolean): Food? = withContext(Dispatchers.IO) {
        val body = get("$BASE/api/v2/product/${URLEncoder.encode(barcode, "UTF-8")}.json?fields=$FIELDS") ?: return@withContext null
        val root = json.parseToJsonElement(body).jsonObject
        if ((root["status"] as? JsonPrimitive)?.content != "1") return@withContext null
        root["product"]?.jsonObject?.let { toFood(it, german) }
    }

    /**
     * Products matching [query], best first: the fast search service, and the classic search if
     * that one fails. Throws when neither answers (offline).
     */
    suspend fun search(query: String, german: Boolean): List<Food> = withContext(Dispatchers.IO) {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val lc = if (german) "de" else "en"
        val foods = runCatching {
            val body = get("$SEARCH/search?q=$q&langs=$lc&page_size=40&fields=$FIELDS") ?: error("search service unavailable")
            (json.parseToJsonElement(body).jsonObject["hits"] as? JsonArray).orEmpty()
        }.recoverCatching {
            val body = get("$BASE/cgi/search.pl?search_terms=$q&search_simple=1&action=process&json=1&page_size=40&lc=$lc&fields=$FIELDS")
                ?: error("classic search unavailable")
            (json.parseToJsonElement(body).jsonObject["products"] as? JsonArray).orEmpty()
        }.getOrThrow().mapNotNull { runCatching { toFood(it.jsonObject, german) }.getOrNull() }
        relevant(foods, query)
    }

        /**
     * The database also matches categories and ingredients ("skyr" finds fromage blanc). Products
     * whose name or brand contains every search word come first; the rest only if nothing does.
     */
    internal fun relevant(foods: List<Food>, query: String): List<Food> {
        val words = query.lowercase().split(' ', '-').filter { it.length > 1 }
        if (words.isEmpty()) return foods
        val matching = foods.filter { f -> val text = "${f.name} ${f.brand}".lowercase(); words.all { it in text } }
        return (matching.ifEmpty { foods }).distinctBy { "${it.name.lowercase()}|${it.brand.lowercase()}" }.take(25)
    }

    private fun toFood(p: JsonObject, german: Boolean): Food? {
        val code = p.str("code") ?: return null
        val name = (if (german) p.str("product_name_de") else p.str("product_name_en"))?.takeIf { it.isNotBlank() }
            ?: p.str("product_name")?.takeIf { it.isNotBlank() } ?: return null
        val n = p["nutriments"]?.jsonObject ?: return null
        val kcal = n.num("energy-kcal_100g") ?: n.num("energy_100g")?.let { it / 4.184 } ?: return null
        val protein = n.num("proteins_100g") ?: return null
        if (kcal !in 0.0..1000.0 || protein !in 0.0..100.0) return null
        return Food(
            id = "off_$code",
            name = name.trim().take(120),
            // The classic API gives "Arla, Skyr", the search service a list.
            brand = (p.str("brands") ?: (p["brands"] as? JsonArray)?.firstOrNull()?.let { (it as? JsonPrimitive)?.content })
                ?.substringBefore(',')?.trim()?.take(120).orEmpty(),
            barcode = code.take(32),
            kcalPer100 = round1(kcal),
            proteinPer100 = round1(protein),
            carbsPer100 = n.num("carbohydrates_100g")?.takeIf { it in 0.0..100.0 }?.let(::round1),
            fatPer100 = n.num("fat_100g")?.takeIf { it in 0.0..100.0 }?.let(::round1),
            servingG = p.num("serving_quantity")?.takeIf { it in 1.0..2000.0 },
        )
    }

    private fun get(url: String): String? {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("User-Agent", USER_AGENT)
            if (connection.responseCode == 404) return null
            if (connection.responseCode !in 200..299) {
                Log.w("OpenFoodFacts", "HTTP ${connection.responseCode}")
                return null
            }
            // Under load the site answers with an HTML page and status 200: that is no answer.
            connection.inputStream.bufferedReader().use { it.readText() }.takeIf { it.trimStart().startsWith("{") }
        } finally {
            connection.disconnect()
        }
    }

    private fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
    private fun JsonObject.num(key: String): Double? = (this[key] as? JsonPrimitive)?.let { it.doubleOrNull ?: it.content.toDoubleOrNull() }
    private fun round1(v: Double) = Math.round(v * 10) / 10.0
}
