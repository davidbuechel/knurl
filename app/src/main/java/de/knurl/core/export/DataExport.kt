package de.knurl.core.export

import de.knurl.data.nutrition.NutritionData
import de.knurl.domain.model.Food
import de.knurl.domain.model.FoodEntry
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

import de.knurl.data.TrainingData
import de.knurl.domain.calc.ExerciseLookup
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Everything a person can take with them (DSGVO Art. 20): the full data as JSON, every set as CSV. */
object DataExport {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    /** Training and, when given, the whole food log and saved foods, in one JSON file. */
    fun json(data: TrainingData, nutrition: NutritionData? = null): String {
        val training = json.encodeToJsonElement(TrainingData.serializer(), data).jsonObject
        if (nutrition == null) return json.encodeToString(JsonObject.serializer(), training)
        val all = JsonObject(training + mapOf(
            "foodLog" to json.encodeToJsonElement(ListSerializer(FoodEntry.serializer()), nutrition.entries),
            "foods" to json.encodeToJsonElement(ListSerializer(Food.serializer()), nutrition.foods),
        ))
        return json.encodeToString(JsonObject.serializer(), all)
    }

    /** One row per set, oldest first. Weights in kilograms, as stored. */
    fun csv(data: TrainingData, lookup: ExerciseLookup): String = buildString {
        appendLine("date,workout,exercise,set,kind,weight_kg,reps,rir")
        val dateFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        for (workout in data.workouts.sortedBy { it.startedAtMs }) {
            val date = dateFormat.format(Instant.ofEpochMilli(workout.startedAtMs).atZone(ZoneId.systemDefault()).toLocalDateTime().withNano(0))
            for (item in workout.exercises) {
                val name = lookup(item.exerciseId)?.nameEn ?: item.exerciseId
                item.sets.forEachIndexed { index, set ->
                    appendLine(listOf(date, workout.name, name, "${index + 1}", set.kind.name.lowercase(), "${set.weightKg}", "${set.reps}", set.rir?.toString().orEmpty()).joinToString(",") { cell(it) })
                }
            }
        }
    }

    /** Quotes a cell when it holds a comma, quote or line break. */
    private fun cell(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' }) "\"" + value.replace("\"", "\"\"") + "\"" else value
}
