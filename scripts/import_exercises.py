"""
Builds Knurl's exercise catalog from free-exercise-db (github.com/yuhonas/free-exercise-db, Unlicense).

    python scripts/import_exercises.py <path-to-free-exercise-db-clone>

Writes
  app/src/main/assets/exercises.json   the catalog the app loads (shared: ExerciseCatalog.load)
  public/exercises/<id>/0.webp, 1.webp  the photos, served by Firebase Hosting

Which exercises the app offers is decided by scripts/exercise_list.py (hand-picked, German names
written by hand). Core exercise IDs come from the Kotlin catalog and get photos and instructions
from their match in the database (OUR_IDS); database rows get slug(db id) as id. Exercises that
leave the list stay in the file as hidden so the history keeps their names.
"""
import json
import os
import re
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
IMAGE_WIDTH = 480
IMAGE_QUALITY = 70

# our id -> free-exercise-db id
OUR_IDS = {
    "bench_press": "Barbell_Bench_Press_-_Medium_Grip", "incline_bench_press": "Barbell_Incline_Bench_Press_-_Medium_Grip",
    "db_bench_press": "Dumbbell_Bench_Press", "db_incline_press": "Incline_Dumbbell_Press", "machine_chest_press": "Machine_Bench_Press",
    "pec_deck": "Butterfly", "cable_fly": "Cable_Crossover", "dips": "Dips_-_Chest_Version", "push_up": "Pushups",
    "smith_bench_press": "Smith_Machine_Bench_Press", "deadlift": "Barbell_Deadlift", "barbell_row": "Bent_Over_Barbell_Row",
    "db_row": "One-Arm_Dumbbell_Row", "seated_cable_row": "Seated_Cable_Rows", "machine_row": "Leverage_Iso_Row",
    "lat_pulldown": "Wide-Grip_Lat_Pulldown", "pull_up": "Pullups", "straight_arm_pulldown": "Straight-Arm_Pulldown",
    "back_extension": "Hyperextensions_Back_Extensions", "overhead_press": "Standing_Military_Press",
    "db_shoulder_press": "Dumbbell_Shoulder_Press", "machine_shoulder_press": "Machine_Shoulder_Military_Press",
    "lateral_raise": "Side_Lateral_Raise", "cable_lateral_raise": "Cable_Seated_Lateral_Raise", "reverse_pec_deck": "Reverse_Machine_Flyes",
    "face_pull": "Face_Pull", "barbell_curl": "Barbell_Curl", "ez_curl": "EZ-Bar_Curl", "db_curl": "Dumbbell_Bicep_Curl",
    "hammer_curl": "Hammer_Curls", "cable_curl": "Standing_Biceps_Cable_Curl", "triceps_pushdown": "Triceps_Pushdown",
    "overhead_triceps_extension": "Cable_Rope_Overhead_Triceps_Extension", "skull_crusher": "EZ-Bar_Skullcrusher",
    "close_grip_bench": "Close-Grip_Barbell_Bench_Press", "back_squat": "Barbell_Full_Squat", "front_squat": "Front_Barbell_Squat",
    "leg_press": "Leg_Press", "hack_squat": "Hack_Squat", "smith_squat": "Smith_Machine_Squat", "goblet_squat": "Goblet_Squat",
    "bulgarian_split_squat": "Split_Squat_with_Dumbbells", "walking_lunge": "Dumbbell_Lunges", "leg_extension": "Leg_Extensions",
    "romanian_deadlift": "Romanian_Deadlift", "lying_leg_curl": "Lying_Leg_Curls", "seated_leg_curl": "Seated_Leg_Curl",
    "hip_thrust": "Barbell_Hip_Thrust", "hip_abduction": "Thigh_Abductor", "hip_adduction": "Thigh_Adductor",
    "kettlebell_swing": "One-Arm_Kettlebell_Swings", "standing_calf_raise": "Standing_Calf_Raises", "seated_calf_raise": "Seated_Calf_Raise",
    "mountain_climber": "Mountain_Climbers", "farmers_carry": "Farmers_Walk", "sled_push": "Sled_Push",
    "plank": "Plank", "cable_crunch": "Cable_Crunch", "hanging_leg_raise": "Hanging_Leg_Raise", "ab_wheel": "Ab_Roller",
}

LEVELS = {"beginner": "BEGINNER", "intermediate": "INTERMEDIATE", "expert": "EXPERT"}

def slug(db_id):
    return re.sub(r"_+", "_", re.sub(r"[^a-z0-9]+", "_", db_id.lower())).strip("_")


def convert_images(src_root, db_ex, target_id, out_root):
    folder = os.path.join(out_root, target_id)
    os.makedirs(folder, exist_ok=True)
    for i, rel in enumerate(db_ex.get("images", [])[:2]):
        target = os.path.join(folder, "%d.webp" % i)
        if os.path.exists(target):
            continue
        with Image.open(os.path.join(src_root, "exercises", rel)) as img:
            img = img.convert("RGB")
            height = round(img.height * IMAGE_WIDTH / img.width)
            img.resize((IMAGE_WIDTH, height), Image.LANCZOS).save(target, "WEBP", quality=IMAGE_QUALITY, method=6)
    return min(2, len(db_ex.get("images", [])))


def own_exercises():
    """Our core exercises, read from the Kotlin catalog (the single source for them)."""
    path = os.path.join(ROOT, "shared", "src", "commonMain", "kotlin", "de", "knurl", "domain", "catalog", "ExerciseCatalog.kt")
    pattern_ = re.compile(r'ex\("([a-z_]+)", "([^"]+)", "([^"]+)", (\w+), (\w+), (\w+)((?:, \w+)*)\)')
    alias = {"CORE_PATTERN": "CORE"}
    result = {}
    for m in pattern_.finditer(open(path, encoding="utf-8").read()):
        result[m.group(1)] = {
            "id": m.group(1), "nameDe": m.group(2), "nameEn": m.group(3), "primary": m.group(4),
            "secondary": [s.strip() for s in m.group(7).split(",") if s.strip()],
            "equipment": m.group(5), "pattern": alias.get(m.group(6), m.group(6)),
        }
    return result


def main(src_root):
    """
    Builds the catalog from scripts/exercise_list.py. Exercises that were in the catalog before but
    are no longer on the list stay in the file as hidden ("archived"): the history and routines
    still show their names, the library and search no longer offer them.
    """
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    from exercise_list import EXERCISES, OWN_INSTRUCTIONS, INSTRUCTIONS_EN

    db = {ex["id"]: ex for ex in json.load(open(os.path.join(src_root, "dist", "exercises.json"), encoding="utf-8"))}
    ours = own_exercises()
    out_images = os.path.join(ROOT, "public", "exercises")
    out = os.path.join(ROOT, "app", "src", "main", "assets", "exercises.json")
    previous = {e["id"]: e for e in json.load(open(out, encoding="utf-8"))["exercises"]} if os.path.exists(out) else {}
    catalog = []

    for row in EXERCISES:
        if row["kind"] == "core":
            entry = dict(ours[row["id"]])
            ex = db.get(OUR_IDS.get(row["id"], ""))
        elif row["kind"] == "db":
            ex = db[row["db"]]
            entry = {k: row[k] for k in ("nameEn", "nameDe", "primary", "secondary", "equipment", "pattern")}
            entry["id"] = slug(row["db"])
        else:
            ex = None
            entry = {k: row[k] for k in ("id", "nameEn", "nameDe", "primary", "secondary", "equipment", "pattern", "level")}
            entry["instructionsEn"], entry["instructionsDe"] = OWN_INSTRUCTIONS[row["id"]]
        if ex:
            entry["level"] = LEVELS.get(ex.get("level"), "BEGINNER")
            # "expert" in the database often just means unusual; nothing on our list is expert-only.
            if entry["level"] == "EXPERT":
                entry["level"] = "INTERMEDIATE"
            entry["instructionsEn"] = ex.get("instructions", [])
            entry["images"] = convert_images(src_root, ex, entry["id"], out_images)
            # Where the database's own text is missing or not useful, ours replaces it (both languages).
            if entry["id"] in OWN_INSTRUCTIONS:
                entry["instructionsEn"], entry["instructionsDe"] = OWN_INSTRUCTIONS[entry["id"]]
        if not entry.get("instructionsEn") and entry["id"] in INSTRUCTIONS_EN:
            entry["instructionsEn"] = INSTRUCTIONS_EN[entry["id"]]
        catalog.append(entry)

    ids = [e["id"] for e in catalog]
    duplicates = {i for i in ids if ids.count(i) > 1}
    assert not duplicates, duplicates

    # Hand-written German instructions (scripts/instructions-de.json), keyed by our id.
    instructions_de = json.load(open(os.path.join(ROOT, "scripts", "instructions-de.json"), encoding="utf-8"))
    for entry in catalog:
        if entry["id"] in instructions_de:
            entry["instructionsDe"] = instructions_de[entry["id"]]

    # Removed from the list: kept hidden, slim (names, muscles, photo count) so old workouts read well.
    current = set(ids)
    retired = []
    for old_id, old in sorted(previous.items()):
        if old_id in current:
            continue
        keep = {k: old[k] for k in ("id", "nameEn", "nameDe", "primary", "secondary", "equipment", "pattern") if k in old}
        keep["images"] = old.get("images", 0)
        keep["archived"] = True
        retired.append(keep)

    with open(out, "w", encoding="utf-8") as f:
        json.dump({"version": 2, "source": "Knurl list; photos and English instructions partly from free-exercise-db (Unlicense)",
                   "exercises": catalog + retired}, f, ensure_ascii=False, separators=(",", ":"))
    print("offered:", len(catalog), "hidden:", len(retired), "with photos:", sum(1 for e in catalog if e.get("images")),
          "German instructions:", sum(1 for e in catalog if e.get("instructionsDe")))
    missing = [e["id"] for e in catalog if not e.get("images")]
    print("without photo:", ", ".join(missing))


if __name__ == "__main__":
    main(sys.argv[1])
