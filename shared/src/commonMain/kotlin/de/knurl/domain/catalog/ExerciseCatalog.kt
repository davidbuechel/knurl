package de.knurl.domain.catalog

import de.knurl.domain.model.Equipment
import de.knurl.domain.model.Equipment.BARBELL
import de.knurl.domain.model.Equipment.BODYWEIGHT
import de.knurl.domain.model.Equipment.CABLE
import de.knurl.domain.model.Equipment.DUMBBELL
import de.knurl.domain.model.Equipment.EZ_BAR
import de.knurl.domain.model.Equipment.KETTLEBELL
import de.knurl.domain.model.Equipment.MACHINE
import de.knurl.domain.model.Equipment.OTHER
import de.knurl.domain.model.Equipment.SMITH
import de.knurl.domain.model.Exercise
import de.knurl.domain.model.Muscle
import de.knurl.domain.model.Muscle.BACK
import de.knurl.domain.model.Muscle.BICEPS
import de.knurl.domain.model.Muscle.CALVES
import de.knurl.domain.model.Muscle.CHEST
import de.knurl.domain.model.Muscle.CORE
import de.knurl.domain.model.Muscle.FOREARMS
import de.knurl.domain.model.Muscle.GLUTES
import de.knurl.domain.model.Muscle.HAMSTRINGS
import de.knurl.domain.model.Muscle.QUADS
import de.knurl.domain.model.Muscle.SHOULDERS
import de.knurl.domain.model.Muscle.TRICEPS
import de.knurl.domain.model.MovementPattern
import de.knurl.domain.model.MovementPattern.CORE as CORE_PATTERN
import de.knurl.domain.model.MovementPattern.HINGE
import de.knurl.domain.model.MovementPattern.HORIZONTAL_PULL
import de.knurl.domain.model.MovementPattern.HORIZONTAL_PUSH
import de.knurl.domain.model.MovementPattern.ISOLATION
import de.knurl.domain.model.MovementPattern.LUNGE
import de.knurl.domain.model.MovementPattern.SQUAT
import de.knurl.domain.model.MovementPattern.VERTICAL_PULL
import de.knurl.domain.model.MovementPattern.VERTICAL_PUSH
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The exercise catalog. [core] is our own base list; the full catalog (about 160 hand-picked gym,
 * CrossFit and Hyrox exercises from scripts/exercise_list.py, plus hidden retired ones, see
 * [RetiredExercises]) is loaded from the app's assets with [load]. IDs are stable forever: workouts
 * reference them. Never rename or reuse one.
 */
object ExerciseCatalog {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private class CatalogFile(val version: Int, val exercises: List<Exercise>)

    private var loaded: List<Exercise>? = null
    private var byId: Map<String, Exercise> = emptyMap()

    /** Everything known: the full catalog once loaded, otherwise the core list. */
    val all: List<Exercise> get() = loaded ?: core

    fun find(id: String): Exercise? = byId[id] ?: coreById[id]

    /** Reads the catalog file (assets/exercises.json). Core exercises keep working if this fails. */
    fun load(text: String) {
        val exercises = json.decodeFromString(CatalogFile.serializer(), text).exercises
        loaded = exercises
        byId = exercises.associateBy { it.id }
    }

    val core: List<Exercise> = listOf(
        // Chest
        ex("bench_press", "Bankdrücken", "Bench Press", CHEST, BARBELL, HORIZONTAL_PUSH, TRICEPS, SHOULDERS),
        ex("incline_bench_press", "Schrägbankdrücken", "Incline Bench Press", CHEST, BARBELL, HORIZONTAL_PUSH, SHOULDERS, TRICEPS),
        ex("db_bench_press", "Kurzhantel-Bankdrücken", "Dumbbell Bench Press", CHEST, DUMBBELL, HORIZONTAL_PUSH, TRICEPS, SHOULDERS),
        ex("db_incline_press", "Kurzhantel-Schrägbankdrücken", "Incline Dumbbell Press", CHEST, DUMBBELL, HORIZONTAL_PUSH, SHOULDERS, TRICEPS),
        ex("machine_chest_press", "Brustpresse", "Machine Chest Press", CHEST, MACHINE, HORIZONTAL_PUSH, TRICEPS),
        ex("pec_deck", "Butterfly", "Pec Deck", CHEST, MACHINE, ISOLATION),
        ex("cable_fly", "Kabelzug-Fliegende", "Cable Fly", CHEST, CABLE, ISOLATION),
        ex("dips", "Dips", "Dips", CHEST, BODYWEIGHT, VERTICAL_PUSH, TRICEPS, SHOULDERS),
        ex("push_up", "Liegestütz", "Push-Up", CHEST, BODYWEIGHT, HORIZONTAL_PUSH, TRICEPS),
        ex("smith_bench_press", "Bankdrücken an der Multipresse", "Smith Machine Bench Press", CHEST, SMITH, HORIZONTAL_PUSH, TRICEPS),

        // Back
        ex("deadlift", "Kreuzheben", "Deadlift", BACK, BARBELL, HINGE, HAMSTRINGS, GLUTES, FOREARMS),
        ex("barbell_row", "Langhantelrudern", "Barbell Row", BACK, BARBELL, HORIZONTAL_PULL, BICEPS),
        ex("db_row", "Kurzhantelrudern einarmig", "One-Arm Dumbbell Row", BACK, DUMBBELL, HORIZONTAL_PULL, BICEPS),
        ex("seated_cable_row", "Rudern am Kabel sitzend", "Seated Cable Row", BACK, CABLE, HORIZONTAL_PULL, BICEPS),
        ex("machine_row", "Rudermaschine", "Machine Row", BACK, MACHINE, HORIZONTAL_PULL, BICEPS),
        ex("lat_pulldown", "Latzug", "Lat Pulldown", BACK, CABLE, VERTICAL_PULL, BICEPS),
        ex("pull_up", "Klimmzug", "Pull-Up", BACK, BODYWEIGHT, VERTICAL_PULL, BICEPS, FOREARMS),
        ex("straight_arm_pulldown", "Überzüge am Kabel", "Straight-Arm Pulldown", BACK, CABLE, ISOLATION),
        ex("back_extension", "Hyperextension", "Back Extension", BACK, BODYWEIGHT, HINGE, GLUTES, HAMSTRINGS),

        // Shoulders
        ex("overhead_press", "Schulterdrücken stehend", "Overhead Press", SHOULDERS, BARBELL, VERTICAL_PUSH, TRICEPS),
        ex("db_shoulder_press", "Kurzhantel-Schulterdrücken", "Dumbbell Shoulder Press", SHOULDERS, DUMBBELL, VERTICAL_PUSH, TRICEPS),
        ex("machine_shoulder_press", "Schulterpresse", "Machine Shoulder Press", SHOULDERS, MACHINE, VERTICAL_PUSH, TRICEPS),
        ex("lateral_raise", "Seitheben", "Lateral Raise", SHOULDERS, DUMBBELL, ISOLATION),
        ex("cable_lateral_raise", "Seitheben am Kabel", "Cable Lateral Raise", SHOULDERS, CABLE, ISOLATION),
        ex("reverse_pec_deck", "Reverse Butterfly", "Reverse Pec Deck", SHOULDERS, MACHINE, ISOLATION, BACK),
        ex("face_pull", "Face Pull", "Face Pull", SHOULDERS, CABLE, HORIZONTAL_PULL, BACK),

        // Arms
        ex("barbell_curl", "Langhantel-Curl", "Barbell Curl", BICEPS, BARBELL, ISOLATION, FOREARMS),
        ex("ez_curl", "SZ-Curl", "EZ-Bar Curl", BICEPS, EZ_BAR, ISOLATION, FOREARMS),
        ex("db_curl", "Kurzhantel-Curl", "Dumbbell Curl", BICEPS, DUMBBELL, ISOLATION, FOREARMS),
        ex("hammer_curl", "Hammer-Curl", "Hammer Curl", BICEPS, DUMBBELL, ISOLATION, FOREARMS),
        ex("cable_curl", "Curl am Kabel", "Cable Curl", BICEPS, CABLE, ISOLATION),
        ex("triceps_pushdown", "Trizepsdrücken am Kabel", "Triceps Pushdown", TRICEPS, CABLE, ISOLATION),
        ex("overhead_triceps_extension", "Trizeps über Kopf am Kabel", "Overhead Cable Triceps Extension", TRICEPS, CABLE, ISOLATION),
        ex("skull_crusher", "French Press", "Skull Crusher", TRICEPS, EZ_BAR, ISOLATION),
        ex("close_grip_bench", "Enges Bankdrücken", "Close-Grip Bench Press", TRICEPS, BARBELL, HORIZONTAL_PUSH, CHEST),

        // Legs
        ex("back_squat", "Kniebeuge", "Back Squat", QUADS, BARBELL, SQUAT, GLUTES, HAMSTRINGS),
        ex("front_squat", "Frontkniebeuge", "Front Squat", QUADS, BARBELL, SQUAT, GLUTES, CORE),
        ex("leg_press", "Beinpresse", "Leg Press", QUADS, MACHINE, SQUAT, GLUTES),
        ex("hack_squat", "Hackenschmidt", "Hack Squat", QUADS, MACHINE, SQUAT, GLUTES),
        ex("smith_squat", "Kniebeuge an der Multipresse", "Smith Machine Squat", QUADS, SMITH, SQUAT, GLUTES),
        ex("goblet_squat", "Goblet Squat", "Goblet Squat", QUADS, KETTLEBELL, SQUAT, GLUTES),
        ex("bulgarian_split_squat", "Bulgarian Split Squat", "Bulgarian Split Squat", QUADS, DUMBBELL, LUNGE, GLUTES),
        ex("walking_lunge", "Ausfallschritte gehend", "Walking Lunge", QUADS, DUMBBELL, LUNGE, GLUTES),
        ex("leg_extension", "Beinstrecker", "Leg Extension", QUADS, MACHINE, ISOLATION),
        ex("romanian_deadlift", "Rumänisches Kreuzheben", "Romanian Deadlift", HAMSTRINGS, BARBELL, HINGE, GLUTES, BACK),
        ex("lying_leg_curl", "Beinbeuger liegend", "Lying Leg Curl", HAMSTRINGS, MACHINE, ISOLATION),
        ex("seated_leg_curl", "Beinbeuger sitzend", "Seated Leg Curl", HAMSTRINGS, MACHINE, ISOLATION),
        ex("hip_thrust", "Hip Thrust", "Hip Thrust", GLUTES, BARBELL, HINGE, HAMSTRINGS),
        ex("machine_hip_thrust", "Hip-Thrust-Maschine", "Machine Hip Thrust", GLUTES, MACHINE, HINGE, HAMSTRINGS),
        ex("hip_abduction", "Abduktor", "Hip Abduction", GLUTES, MACHINE, ISOLATION),
        ex("hip_adduction", "Adduktor", "Hip Adduction", QUADS, MACHINE, ISOLATION),
        ex("kettlebell_swing", "Kettlebell Swing", "Kettlebell Swing", GLUTES, KETTLEBELL, HINGE, HAMSTRINGS, CORE),
        ex("standing_calf_raise", "Wadenheben stehend", "Standing Calf Raise", CALVES, MACHINE, ISOLATION),
        ex("seated_calf_raise", "Wadenheben sitzend", "Seated Calf Raise", CALVES, MACHINE, ISOLATION),

        // Core
        ex("plank", "Unterarmstütz", "Plank", CORE, BODYWEIGHT, CORE_PATTERN),
        ex("cable_crunch", "Crunch am Kabel", "Cable Crunch", CORE, CABLE, CORE_PATTERN),
        ex("hanging_leg_raise", "Beinheben hängend", "Hanging Leg Raise", CORE, BODYWEIGHT, CORE_PATTERN),
        ex("ab_wheel", "Bauchroller", "Ab Wheel Rollout", CORE, BODYWEIGHT, CORE_PATTERN),

        // HIIT and Hyrox stations (no photos yet)
        ex("burpee", "Burpees", "Burpees", CORE, BODYWEIGHT, CORE_PATTERN, QUADS, CHEST),
        ex("wall_ball", "Wall Balls", "Wall Balls", QUADS, OTHER, SQUAT, SHOULDERS, GLUTES),
        ex("sled_push", "Sled Push", "Sled Push", QUADS, OTHER, LUNGE, GLUTES, CALVES),
        ex("sled_pull", "Sled Pull", "Sled Pull", BACK, OTHER, HORIZONTAL_PULL, BICEPS, HAMSTRINGS),
        ex("farmers_carry", "Farmer's Carry", "Farmer's Carry", FOREARMS, DUMBBELL, CORE_PATTERN, BACK, CORE),
        ex("sandbag_lunge", "Ausfallschritte mit Sandsack", "Sandbag Lunges", QUADS, OTHER, LUNGE, GLUTES, CORE),
        ex("ski_erg", "SkiErg", "SkiErg", BACK, MACHINE, VERTICAL_PULL, TRICEPS, CORE),
        ex("mountain_climber", "Mountain Climbers", "Mountain Climbers", CORE, BODYWEIGHT, CORE_PATTERN, SHOULDERS),
    )

    private val coreById: Map<String, Exercise> = core.associateBy { it.id }

    private fun ex(
        id: String,
        de: String,
        en: String,
        primary: Muscle,
        equipment: Equipment,
        pattern: MovementPattern,
        vararg secondary: Muscle,
    ) = Exercise(id, de, en, primary, secondary.toList(), equipment, pattern)
}
