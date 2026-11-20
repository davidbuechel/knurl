package de.knurl.domain.catalog

import de.knurl.domain.model.RepRange
import de.knurl.domain.model.Routine
import de.knurl.domain.model.RoutineItem

/** Routines a new user starts with: a push/pull/legs split and a full-body day. They can edit or delete them. */
object StarterRoutines {

    private val strength = RepRange(5, 8)
    private val volume = RepRange(8, 12)
    private val pump = RepRange(12, 15)

    fun all(german: Boolean): List<Routine> = listOf(
        Routine(
            id = "starter_push",
            position = 0,
            name = "Push",
            items = listOf(
                RoutineItem("bench_press", 3, strength, 180),
                RoutineItem("db_incline_press", 3, volume, 120),
                RoutineItem("db_shoulder_press", 3, volume, 120),
                RoutineItem("lateral_raise", 3, pump, 60),
                RoutineItem("triceps_pushdown", 3, volume, 60),
            ),
        ),
        Routine(
            id = "starter_pull",
            position = 1,
            name = "Pull",
            items = listOf(
                RoutineItem("lat_pulldown", 3, volume, 120),
                RoutineItem("barbell_row", 3, strength, 150),
                RoutineItem("seated_cable_row", 3, volume, 90),
                RoutineItem("face_pull", 3, pump, 60),
                RoutineItem("ez_curl", 3, volume, 60),
            ),
        ),
        Routine(
            id = "starter_legs",
            position = 2,
            name = if (german) "Beine" else "Legs",
            items = listOf(
                RoutineItem("back_squat", 3, strength, 180),
                RoutineItem("romanian_deadlift", 3, volume, 150),
                RoutineItem("leg_press", 3, volume, 120),
                RoutineItem("lying_leg_curl", 3, volume, 90),
                RoutineItem("standing_calf_raise", 3, pump, 60),
            ),
        ),
        Routine(
            id = "starter_full_body",
            position = 3,
            name = if (german) "Ganzkörper" else "Full body",
            items = listOf(
                RoutineItem("back_squat", 3, strength, 180),
                RoutineItem("bench_press", 3, strength, 180),
                RoutineItem("lat_pulldown", 3, volume, 120),
                RoutineItem("hip_thrust", 3, volume, 120),
                RoutineItem("cable_crunch", 3, pump, 60),
            ),
        ),
    )
}
