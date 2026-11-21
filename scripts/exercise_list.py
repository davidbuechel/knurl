"""
Knurl's exercise list: what a normal gym, a CrossFit box or a Hyrox class actually uses. This file
is the only place that decides which exercises the app offers (scripts/import_exercises.py reads it).

Rows:
  core(id)                                 one of our core exercises (shared/.../ExerciseCatalog.kt), names
                                           and muscles come from there
  db(db_id, english, german, primary, equipment, pattern, secondary)
                                           from free-exercise-db: photos and English instructions from
                                           there; id = slug(db_id), so ids from earlier imports stay
  own(id, english, german, primary, equipment, pattern, secondary, level)
                                           not in the database: no photo, instructions in OWN_INSTRUCTIONS

Ids never change once released (workouts reference them). Names may be improved any time.
Exercises that leave this list are kept in the catalog as hidden ("archived") so the history still
shows their names; they no longer appear in the library or search.
"""


def core(id):
    return {"kind": "core", "id": id}


def db(db_id, en, de, primary, equipment, pattern, secondary=""):
    return {"kind": "db", "db": db_id, "nameEn": en, "nameDe": de, "primary": primary, "equipment": equipment,
            "pattern": pattern, "secondary": [s for s in secondary.split(",") if s]}


def own(id, en, de, primary, equipment, pattern, secondary="", level="BEGINNER"):
    return {"kind": "own", "id": id, "nameEn": en, "nameDe": de, "primary": primary, "equipment": equipment,
            "pattern": pattern, "secondary": [s for s in secondary.split(",") if s], "level": level}


H_PUSH, V_PUSH, H_PULL, V_PULL = "HORIZONTAL_PUSH", "VERTICAL_PUSH", "HORIZONTAL_PULL", "VERTICAL_PULL"
SQUAT, HINGE, LUNGE, ISO, CORE = "SQUAT", "HINGE", "LUNGE", "ISOLATION", "CORE"

EXERCISES = [
    # ---- Chest
    core("bench_press"), core("incline_bench_press"), core("db_bench_press"), core("db_incline_press"),
    core("machine_chest_press"), core("pec_deck"), core("cable_fly"), core("dips"), core("push_up"), core("smith_bench_press"),
    db("Decline_Barbell_Bench_Press", "Decline Bench Press", "Negativ-Bankdrücken", "CHEST", "BARBELL", H_PUSH, "TRICEPS,SHOULDERS"),
    db("Decline_Dumbbell_Bench_Press", "Decline Dumbbell Press", "Kurzhantel-Negativdrücken", "CHEST", "DUMBBELL", H_PUSH, "TRICEPS"),
    db("Smith_Machine_Incline_Bench_Press", "Smith Machine Incline Press", "Schrägbankdrücken an der Multipresse", "CHEST", "SMITH", H_PUSH, "SHOULDERS,TRICEPS"),
    db("Leverage_Incline_Chest_Press", "Incline Machine Press", "Schrägbank-Brustpresse", "CHEST", "MACHINE", H_PUSH, "SHOULDERS,TRICEPS"),
    db("Dumbbell_Flyes", "Dumbbell Fly", "Kurzhantel-Fliegende", "CHEST", "DUMBBELL", ISO),
    db("Incline_Dumbbell_Flyes", "Incline Dumbbell Fly", "Schrägbank-Fliegende", "CHEST", "DUMBBELL", ISO, "SHOULDERS"),
    db("Low_Cable_Crossover", "Low-to-High Cable Fly", "Kabel-Fliegende von unten", "CHEST", "CABLE", ISO, "SHOULDERS"),
    db("Incline_Push-Up", "Incline Push-Up", "Liegestütz erhöht", "CHEST", "BODYWEIGHT", H_PUSH, "TRICEPS"),
    db("Decline_Push-Up", "Decline Push-Up", "Liegestütz mit erhöhten Füßen", "CHEST", "BODYWEIGHT", H_PUSH, "SHOULDERS,TRICEPS"),
    db("Bent-Arm_Dumbbell_Pullover", "Dumbbell Pullover", "Kurzhantel-Überzüge", "CHEST", "DUMBBELL", ISO, "BACK"),

    # ---- Back
    core("deadlift"), core("barbell_row"), core("db_row"), core("seated_cable_row"), core("machine_row"),
    core("lat_pulldown"), core("pull_up"), core("straight_arm_pulldown"), core("back_extension"),
    db("Sumo_Deadlift", "Sumo Deadlift", "Sumo-Kreuzheben", "BACK", "BARBELL", HINGE, "GLUTES,HAMSTRINGS,QUADS"),
    db("Trap_Bar_Deadlift", "Trap Bar Deadlift", "Trap-Bar-Kreuzheben", "BACK", "OTHER", HINGE, "QUADS,GLUTES,HAMSTRINGS"),
    db("T-Bar_Row_with_Handle", "T-Bar Row", "T-Bar-Rudern", "BACK", "BARBELL", H_PULL, "BICEPS"),
    db("Dumbbell_Incline_Row", "Chest-Supported Dumbbell Row", "Kurzhantelrudern auf der Schrägbank", "BACK", "DUMBBELL", H_PULL, "BICEPS,SHOULDERS"),
    db("Close-Grip_Front_Lat_Pulldown", "Close-Grip Lat Pulldown", "Latzug eng", "BACK", "CABLE", V_PULL, "BICEPS"),
    db("Underhand_Cable_Pulldowns", "Reverse-Grip Lat Pulldown", "Latzug im Untergriff", "BACK", "CABLE", V_PULL, "BICEPS"),
    db("Chin-Up", "Chin-Up", "Klimmzug im Untergriff", "BACK", "BODYWEIGHT", V_PULL, "BICEPS"),
    own("assisted_pull_up", "Assisted Pull-Up", "Klimmzug mit Unterstützung", "BACK", "MACHINE", V_PULL, "BICEPS"),
    db("Inverted_Row", "Inverted Row", "Rudern im Hang", "BACK", "BODYWEIGHT", H_PULL, "BICEPS"),
    db("Barbell_Shrug", "Barbell Shrug", "Langhantel-Shrugs", "BACK", "BARBELL", ISO, "FOREARMS"),
    db("Dumbbell_Shrug", "Dumbbell Shrug", "Kurzhantel-Shrugs", "BACK", "DUMBBELL", ISO, "FOREARMS"),
    db("Good_Morning", "Good Morning", "Good Morning", "HAMSTRINGS", "BARBELL", HINGE, "BACK,GLUTES"),
    own("pendlay_row", "Pendlay Row", "Pendlay-Rudern", "BACK", "BARBELL", H_PULL, "BICEPS", "INTERMEDIATE"),

    # ---- Shoulders
    core("overhead_press"), core("db_shoulder_press"), core("machine_shoulder_press"), core("lateral_raise"),
    core("cable_lateral_raise"), core("reverse_pec_deck"), core("face_pull"),
    db("Seated_Dumbbell_Press", "Seated Dumbbell Shoulder Press", "Kurzhantel-Schulterdrücken sitzend", "SHOULDERS", "DUMBBELL", V_PUSH, "TRICEPS"),
    db("Arnold_Dumbbell_Press", "Arnold Press", "Arnold Press", "SHOULDERS", "DUMBBELL", V_PUSH, "TRICEPS"),
    db("Smith_Machine_Overhead_Shoulder_Press", "Smith Machine Shoulder Press", "Schulterdrücken an der Multipresse", "SHOULDERS", "SMITH", V_PUSH, "TRICEPS"),
    db("Push_Press", "Push Press", "Push Press", "SHOULDERS", "BARBELL", V_PUSH, "TRICEPS,QUADS"),
    own("landmine_press", "Landmine Press", "Landmine Press", "SHOULDERS", "BARBELL", V_PUSH, "CHEST,TRICEPS"),
    db("Front_Dumbbell_Raise", "Front Raise", "Frontheben", "SHOULDERS", "DUMBBELL", ISO),
    own("machine_lateral_raise", "Machine Lateral Raise", "Seitheben an der Maschine", "SHOULDERS", "MACHINE", ISO),
    db("Reverse_Flyes", "Rear Delt Fly", "Reverse Flys mit Kurzhanteln", "SHOULDERS", "DUMBBELL", ISO, "BACK"),
    db("Cable_Rear_Delt_Fly", "Cable Rear Delt Fly", "Reverse Flys am Kabel", "SHOULDERS", "CABLE", ISO, "BACK"),
    db("Upright_Cable_Row", "Cable Upright Row", "Aufrechtes Rudern am Kabel", "SHOULDERS", "CABLE", V_PULL, "BACK,BICEPS"),
    db("Upright_Barbell_Row", "Barbell Upright Row", "Aufrechtes Rudern mit der Langhantel", "SHOULDERS", "BARBELL", V_PULL, "BACK,BICEPS"),

    # ---- Biceps
    core("barbell_curl"), core("ez_curl"), core("db_curl"), core("hammer_curl"), core("cable_curl"),
    db("Incline_Dumbbell_Curl", "Incline Dumbbell Curl", "Kurzhantel-Curl auf der Schrägbank", "BICEPS", "DUMBBELL", ISO),
    db("Preacher_Curl", "Preacher Curl", "Scott-Curl", "BICEPS", "EZ_BAR", ISO),
    db("Machine_Preacher_Curls", "Machine Preacher Curl", "Scott-Curl an der Maschine", "BICEPS", "MACHINE", ISO),
    db("Concentration_Curls", "Concentration Curl", "Konzentrations-Curl", "BICEPS", "DUMBBELL", ISO),
    db("Spider_Curl", "Spider Curl", "Spider-Curl", "BICEPS", "EZ_BAR", ISO),
    db("Cable_Hammer_Curls_-_Rope_Attachment", "Rope Hammer Curl", "Hammer-Curl am Seil", "BICEPS", "CABLE", ISO, "FOREARMS"),
    db("Reverse_Barbell_Curl", "Reverse Curl", "Reverse-Curl", "FOREARMS", "BARBELL", ISO, "BICEPS"),

    # ---- Triceps
    core("triceps_pushdown"), core("overhead_triceps_extension"), core("skull_crusher"), core("close_grip_bench"),
    db("Triceps_Pushdown_-_Rope_Attachment", "Rope Pushdown", "Trizepsdrücken am Seil", "TRICEPS", "CABLE", ISO),
    db("Seated_Triceps_Press", "Dumbbell Overhead Triceps Extension", "Trizepsstrecken über Kopf mit Kurzhantel", "TRICEPS", "DUMBBELL", ISO),
    db("Tricep_Dumbbell_Kickback", "Triceps Kickback", "Trizeps-Kickback", "TRICEPS", "DUMBBELL", ISO),
    db("Bench_Dips", "Bench Dip", "Bank-Dips", "TRICEPS", "BODYWEIGHT", V_PUSH, "CHEST,SHOULDERS"),
    db("Dip_Machine", "Machine Dip", "Dip-Maschine", "TRICEPS", "MACHINE", V_PUSH, "CHEST"),

    # ---- Forearms and grip
    db("Palms-Up_Barbell_Wrist_Curl_Over_A_Bench", "Wrist Curl", "Handgelenk-Curl", "FOREARMS", "BARBELL", ISO),
    db("Palms-Down_Wrist_Curl_Over_A_Bench", "Reverse Wrist Curl", "Handgelenk-Curl im Obergriff", "FOREARMS", "BARBELL", ISO),
    own("dead_hang", "Dead Hang", "Passives Hängen", "FOREARMS", "BODYWEIGHT", CORE, "BACK,SHOULDERS"),

    # ---- Quads
    core("back_squat"), core("front_squat"), core("leg_press"), core("hack_squat"), core("smith_squat"), core("goblet_squat"),
    core("bulgarian_split_squat"), core("walking_lunge"), core("leg_extension"),
    db("Box_Squat", "Box Squat", "Box Squat", "QUADS", "BARBELL", SQUAT, "GLUTES,HAMSTRINGS"),
    own("pendulum_squat", "Pendulum Squat", "Pendel-Kniebeuge", "QUADS", "MACHINE", SQUAT, "GLUTES"),
    db("Bodyweight_Squat", "Air Squat", "Kniebeuge ohne Gewicht", "QUADS", "BODYWEIGHT", SQUAT, "GLUTES"),
    db("Barbell_Lunge", "Barbell Lunge", "Ausfallschritte mit Langhantel", "QUADS", "BARBELL", LUNGE, "GLUTES,HAMSTRINGS"),
    db("Dumbbell_Rear_Lunge", "Reverse Lunge", "Ausfallschritte rückwärts", "QUADS", "DUMBBELL", LUNGE, "GLUTES"),
    db("Dumbbell_Step_Ups", "Step-Up", "Step-ups", "QUADS", "DUMBBELL", LUNGE, "GLUTES"),
    own("wall_sit", "Wall Sit", "Wandsitzen", "QUADS", "BODYWEIGHT", SQUAT, "GLUTES"),
    db("Freehand_Jump_Squat", "Jump Squat", "Sprungkniebeuge", "QUADS", "BODYWEIGHT", SQUAT, "GLUTES,CALVES"),

    # ---- Hamstrings and glutes
    core("romanian_deadlift"), core("lying_leg_curl"), core("seated_leg_curl"), core("hip_thrust"), core("machine_hip_thrust"),
    core("hip_abduction"), core("hip_adduction"), core("kettlebell_swing"),
    db("Stiff-Legged_Dumbbell_Deadlift", "Dumbbell Romanian Deadlift", "Rumänisches Kreuzheben mit Kurzhanteln", "HAMSTRINGS", "DUMBBELL", HINGE, "GLUTES,BACK"),
    db("Stiff-Legged_Barbell_Deadlift", "Stiff-Leg Deadlift", "Kreuzheben mit gestreckten Beinen", "HAMSTRINGS", "BARBELL", HINGE, "GLUTES,BACK"),
    db("Kettlebell_One-Legged_Deadlift", "Single-Leg Romanian Deadlift", "Einbeiniges rumänisches Kreuzheben", "HAMSTRINGS", "KETTLEBELL", HINGE, "GLUTES"),
    db("Standing_Leg_Curl", "Standing Leg Curl", "Beinbeuger stehend", "HAMSTRINGS", "MACHINE", ISO),
    db("Natural_Glute_Ham_Raise", "Nordic Curl", "Nordic Curl", "HAMSTRINGS", "BODYWEIGHT", ISO, "GLUTES"),
    db("Glute_Ham_Raise", "Glute-Ham Raise", "Glute-Ham Raise", "HAMSTRINGS", "MACHINE", ISO, "GLUTES"),
    db("Barbell_Glute_Bridge", "Glute Bridge", "Glute Bridge", "GLUTES", "BARBELL", HINGE, "HAMSTRINGS"),
    db("One-Legged_Cable_Kickback", "Cable Glute Kickback", "Gesäß-Kickback am Kabel", "GLUTES", "CABLE", ISO, "HAMSTRINGS"),
    db("Cable_Hip_Adduction", "Cable Hip Adduction", "Adduktion am Kabel", "QUADS", "CABLE", ISO),

    # ---- Calves
    core("standing_calf_raise"), core("seated_calf_raise"),
    db("Calf_Press_On_The_Leg_Press_Machine", "Leg Press Calf Raise", "Wadendrücken an der Beinpresse", "CALVES", "MACHINE", ISO),
    db("Smith_Machine_Calf_Raise", "Smith Machine Calf Raise", "Wadenheben an der Multipresse", "CALVES", "SMITH", ISO),

    # ---- Core
    core("plank"), core("cable_crunch"), core("hanging_leg_raise"), core("ab_wheel"),
    own("toes_to_bar", "Toes to Bar", "Toes to Bar", "CORE", "BODYWEIGHT", CORE, "BACK,FOREARMS", "INTERMEDIATE"),
    own("hanging_knee_raise", "Hanging Knee Raise", "Knieheben hängend", "CORE", "BODYWEIGHT", CORE),
    db("Knee_Hip_Raise_On_Parallel_Bars", "Captain's Chair Knee Raise", "Knieheben am Dip-Barren", "CORE", "BODYWEIGHT", CORE),
    db("Side_Bridge", "Side Plank", "Seitstütz", "CORE", "BODYWEIGHT", CORE),
    db("Crunches", "Crunch", "Crunch", "CORE", "BODYWEIGHT", CORE),
    db("Sit-Up", "Sit-Up", "Sit-up", "CORE", "BODYWEIGHT", CORE),
    db("Decline_Crunch", "Decline Crunch", "Crunch auf der Negativbank", "CORE", "BODYWEIGHT", CORE),
    db("Air_Bike", "Bicycle Crunch", "Fahrrad-Crunch", "CORE", "BODYWEIGHT", CORE),
    db("Flat_Bench_Lying_Leg_Raise", "Lying Leg Raise", "Beinheben liegend", "CORE", "BODYWEIGHT", CORE),
    db("Jackknife_Sit-Up", "V-Up", "V-up", "CORE", "BODYWEIGHT", CORE),
    db("Russian_Twist", "Russian Twist", "Russian Twist", "CORE", "BODYWEIGHT", CORE),
    db("Dead_Bug", "Dead Bug", "Dead Bug", "CORE", "BODYWEIGHT", CORE),
    own("hollow_hold", "Hollow Hold", "Hollow Hold", "CORE", "BODYWEIGHT", CORE),
    db("Pallof_Press", "Pallof Press", "Pallof Press", "CORE", "CABLE", CORE),
    db("Standing_Cable_Wood_Chop", "Cable Woodchop", "Holzhacker am Kabel", "CORE", "CABLE", CORE, "SHOULDERS"),

    # ---- CrossFit and functional
    db("Power_Clean", "Power Clean", "Power Clean", "BACK", "BARBELL", HINGE, "QUADS,GLUTES,SHOULDERS"),
    own("thruster", "Thruster", "Thruster", "QUADS", "BARBELL", SQUAT, "SHOULDERS,GLUTES,TRICEPS", "INTERMEDIATE"),
    db("Front_Box_Jump", "Box Jump", "Box Jump", "QUADS", "OTHER", SQUAT, "GLUTES,CALVES"),
    db("Rope_Jumping", "Jump Rope", "Seilspringen", "CALVES", "OTHER", CORE, "QUADS"),
    own("double_under", "Double Under", "Double Under", "CALVES", "OTHER", CORE, "QUADS,SHOULDERS", "INTERMEDIATE"),
    db("Battling_Ropes", "Battle Ropes", "Battle Ropes", "SHOULDERS", "OTHER", CORE, "CORE,BACK"),
    db("Kettlebell_Thruster", "Kettlebell Thruster", "Kettlebell-Thruster", "QUADS", "KETTLEBELL", SQUAT, "SHOULDERS,GLUTES"),

    # ---- Hyrox stations and cardio
    core("burpee"), core("wall_ball"), core("sled_push"), core("sled_pull"), core("farmers_carry"), core("sandbag_lunge"),
    core("ski_erg"), core("mountain_climber"),
    own("burpee_broad_jump", "Burpee Broad Jump", "Burpee Broad Jump", "QUADS", "BODYWEIGHT", LUNGE, "CHEST,GLUTES,CORE", "INTERMEDIATE"),
    db("Rowing_Stationary", "Rowing Machine", "Rudergerät", "BACK", "MACHINE", H_PULL, "QUADS,BICEPS"),
    db("Running_Treadmill", "Treadmill Run", "Laufband", "QUADS", "MACHINE", LUNGE, "CALVES,HAMSTRINGS"),
    db("Walking_Treadmill", "Incline Walk", "Gehen auf dem Laufband", "QUADS", "MACHINE", LUNGE, "CALVES,GLUTES"),
    db("Bicycling_Stationary", "Stationary Bike", "Ergometer", "QUADS", "MACHINE", LUNGE, "HAMSTRINGS,CALVES"),
    own("assault_bike", "Assault Bike", "Air Bike", "QUADS", "MACHINE", LUNGE, "SHOULDERS,HAMSTRINGS"),
    db("Stairmaster", "Stair Climber", "Stepper", "QUADS", "MACHINE", LUNGE, "GLUTES,CALVES"),
]

# Instructions for exercises that are not in the database (own rows): (English, German).
OWN_INSTRUCTIONS = {
    "assisted_pull_up": (
        ["Set the counterweight: more weight means more help.", "Kneel or stand on the pad and take the handles a little wider than your shoulders.",
         "Pull your chest up towards the bar, elbows down and back.", "Lower yourself slowly until your arms are straight."],
        ["Stell das Gegengewicht ein: mehr Gewicht heißt mehr Unterstützung.", "Knie oder stell dich auf das Polster und greif die Griffe etwas breiter als schulterbreit.", "Zieh die Brust Richtung Stange, Ellbogen nach unten und hinten.", "Lass dich langsam ab, bis die Arme gestreckt sind."],
    ),
    "pendlay_row": (
        ["Stand over the bar, feet hip-width, and hinge until your back is almost parallel to the floor.", "Grip the bar a little wider than your shoulders, back flat.",
         "Row the bar explosively to your lower chest without lifting your torso.", "Put the bar back down on the floor after every rep."],
        ["Stell dich über die Stange, Füße hüftbreit, und beug dich vor, bis der Rücken fast parallel zum Boden ist.", "Greif die Stange etwas breiter als schulterbreit, Rücken gerade.", "Zieh die Stange explosiv zur unteren Brust, ohne den Oberkörper aufzurichten.", "Leg die Stange nach jeder Wiederholung auf dem Boden ab."],
    ),
    "landmine_press": (
        ["Put one end of a barbell in a landmine or a corner and hold the other end at shoulder height.", "Stand in a split stance, core tight.",
         "Press the bar up and forward until your arm is straight.", "Lower it back to your shoulder under control."],
        ["Steck ein Ende der Langhantel in eine Landmine oder Ecke und halt das andere auf Schulterhöhe.", "Stell dich in Schrittstellung, Rumpf fest.", "Drück die Stange nach oben vorne, bis der Arm gestreckt ist.", "Führ sie kontrolliert zur Schulter zurück."],
    ),
    "machine_lateral_raise": (
        ["Sit with your shoulders level with the machine's pivot and the pads on the outside of your arms.", "Raise your arms out to the side up to shoulder height.",
         "Pause briefly at the top.", "Lower slowly without letting the weight touch down."],
        ["Setz dich so, dass deine Schultern auf Höhe des Drehpunkts sind, Polster an der Außenseite der Arme.", "Heb die Arme seitlich bis auf Schulterhöhe.", "Halt oben kurz.", "Senk sie langsam, ohne das Gewicht abzusetzen."],
    ),
    "dead_hang": (
        ["Grab a pull-up bar with an overhand grip, hands shoulder-width.", "Let your body hang with straight arms, feet off the floor.",
         "Keep your shoulders slightly engaged and breathe calmly.", "Hold for time, then step down."],
        ["Greif eine Klimmzugstange im Obergriff, Hände schulterbreit.", "Häng mit gestreckten Armen, Füße vom Boden.", "Halt die Schultern leicht aktiv und atme ruhig.", "Halte auf Zeit, dann steig ab."],
    ),
    "pendulum_squat": (
        ["Stand on the platform with your back against the pad and shoulders under the pads.", "Release the safety and bend your knees, letting the sled swing down.",
         "Go as deep as you can with your heels down.", "Push through your whole foot back to the top."],
        ["Stell dich mit dem Rücken am Polster auf die Plattform, Schultern unter den Polstern.", "Lös die Sicherung und geh in die Knie, der Schlitten schwingt nach unten.", "Geh so tief wie möglich, die Fersen bleiben unten.", "Drück dich über den ganzen Fuß zurück nach oben."],
    ),
    "wall_sit": (
        ["Lean your back against a wall and walk your feet forward.", "Slide down until your thighs are parallel to the floor, knees over ankles.",
         "Keep your back flat against the wall.", "Hold for time."],
        ["Lehn dich mit dem Rücken an eine Wand und setz die Füße nach vorne.", "Rutsch hinunter, bis die Oberschenkel parallel zum Boden sind, Knie über den Knöcheln.", "Dein Rücken bleibt flach an der Wand.", "Halte auf Zeit."],
    ),
    "toes_to_bar": (
        ["Hang from a pull-up bar, hands shoulder-width.", "Brace your core and swing slightly to start the movement.",
         "Lift your legs and touch the bar with your toes, keeping your arms almost straight.", "Lower your legs under control and go straight into the next rep."],
        ["Häng dich an eine Klimmzugstange, Hände schulterbreit.", "Spann den Rumpf an und hol leicht Schwung.", "Heb die Beine und berühr mit den Zehen die Stange, Arme fast gestreckt.", "Senk die Beine kontrolliert und geh direkt in die nächste Wiederholung."],
    ),
    "hanging_knee_raise": (
        ["Hang from a pull-up bar with straight arms.", "Pull your knees up towards your chest, rolling your pelvis forward.",
         "Pause briefly at the top.", "Lower your legs slowly without swinging."],
        ["Häng dich mit gestreckten Armen an eine Klimmzugstange.", "Zieh die Knie zur Brust und roll das Becken nach vorne ein.", "Halt oben kurz.", "Senk die Beine langsam, ohne zu schwingen."],
    ),
    "hollow_hold": (
        ["Lie on your back with your arms overhead.", "Press your lower back into the floor and lift your shoulders and legs a little.",
         "Keep your arms and legs long and your belly tight.", "Hold for time; bend your knees to make it easier."],
        ["Leg dich auf den Rücken, Arme über dem Kopf.", "Drück den unteren Rücken in den Boden und heb Schultern und Beine leicht an.", "Halt Arme und Beine lang, Bauch fest.", "Halte auf Zeit; mit angewinkelten Knien wird es leichter."],
    ),
    "thruster": (
        ["Hold the bar in the front rack, hands just outside your shoulders.", "Squat down until your thighs are at least parallel.",
         "Drive up explosively and use the momentum to press the bar overhead.", "Bring the bar back to your shoulders and go into the next squat."],
        ["Halt die Stange vorne auf den Schultern, Hände knapp außerhalb der Schultern.", "Geh in die Kniebeuge, bis die Oberschenkel mindestens parallel sind.", "Drück dich explosiv hoch und nutz den Schwung, um die Stange über den Kopf zu drücken.", "Nimm die Stange zurück auf die Schultern und geh in die nächste Kniebeuge."],
    ),
    "double_under": (
        ["Hold the rope handles at hip height, elbows close to your body.", "Jump a little higher than for normal skipping.",
         "Spin the rope with your wrists so it passes under your feet twice per jump.", "Land softly on the balls of your feet."],
        ["Halt die Griffe auf Hüfthöhe, Ellbogen nah am Körper.", "Spring etwas höher als beim normalen Seilspringen.", "Dreh das Seil aus den Handgelenken, sodass es pro Sprung zweimal unter den Füßen durchläuft.", "Lande weich auf den Fußballen."],
    ),
    "burpee_broad_jump": (
        ["Drop into a burpee: chest to the floor.", "Push up and bring your feet under you.",
         "Jump forward as far as you can with both feet.", "Land softly and go straight into the next burpee."],
        ["Geh in einen Burpee: Brust zum Boden.", "Drück dich hoch und bring die Füße unter den Körper.", "Spring mit beiden Füßen so weit wie möglich nach vorne.", "Lande weich und geh direkt in den nächsten Burpee."],
    ),
    "assault_bike": (
        ["Adjust the seat so your leg is almost straight at the bottom.", "Push and pull the handles while you pedal.",
         "Keep an even rhythm, or sprint for intervals.", "Count calories, distance or time."],
        ["Stell den Sitz so ein, dass das Bein unten fast gestreckt ist.", "Drück und zieh die Griffe, während du trittst.", "Fahr gleichmäßig oder sprinte in Intervallen.", "Zähl Kalorien, Strecke oder Zeit."],
    ),
}
