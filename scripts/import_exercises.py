"""
Builds Knurl's exercise catalog from free-exercise-db (github.com/yuhonas/free-exercise-db, Unlicense).

    python scripts/import_exercises.py <path-to-free-exercise-db-clone>

Writes
  app/src/main/assets/exercises.json   the catalog the app loads (shared: ExerciseCatalog.load)
  public/exercises/<id>/0.webp, 1.webp  the photos, served by Firebase Hosting

Our original 58 exercise IDs stay as they are (workouts reference them); they get photos and
instructions from their match in the database (OUR_IDS). Everything else gets a stable id derived
from the database id. German names: hand-written for our 58, built from word rules (GERMAN) for
the rest; review them in the generated file before release.
"""
import json
import os
import re
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
INCLUDE_CATEGORIES = {"strength", "plyometrics", "cardio"}

# Normal gym, fitness, bodybuilding, HIIT. Powerlifting specials, olympic lifts, strongman and
# expert-level exotics are left out on purpose: the app is for regular gym-goers.
EXCLUDE_NAMES = re.compile(
    r"chain|board press|band |bands|with band|partner|rickshaw|yoke|atlas|keg|tire|log lift|axle|circus|sling|deficit|pin |"
    r"rack pull|speed|isometric|anti-gravity|zercher|jefferson|turkish|windmill|bottoms|kneeling squat|one arm against|cuban|"
    r"bradford|gironda|svend|jm press|tate|pirate|see-saw|suitcase|sots|clean|snatch|jerk|hang |power |muscle up|dead bug|"
    r"scissor|flutter|plate twist|thruster with",
    re.I,
)
# Equipment "other"/none is only kept for typical gym, cardio, HIIT and Hyrox kit.
OTHER_OK = re.compile(r"sled|row|rope|bike|treadmill|elliptical|stair|box|sandbag|farmer|wall ball|ski|jump rope|trap bar|step|walking|carry|battle|push-up|dip|pull-up|chin", re.I)
PLYO_OK = re.compile(r"box jump|burpee|jump squat|broad jump|split jump|lunge jump|mountain climber|skater|high knee|tuck jump|frog|star jump|jumping jack|bench jump|knee tuck|lateral box|depth jump|freehand jump", re.I)


def wanted(ex):
    name, equipment, category = ex["name"], ex.get("equipment"), ex.get("category")
    if category not in INCLUDE_CATEGORIES or ex.get("level") == "expert" or EXCLUDE_NAMES.search(name):
        return False
    if equipment == "foam roll":
        return False
    if category == "plyometrics" and not PLYO_OK.search(name):
        return False
    if equipment in ("other", None) and category != "cardio" and not OTHER_OK.search(name):
        return False
    return True
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
    "plank": "Plank", "cable_crunch": "Cable_Crunch", "hanging_leg_raise": "Hanging_Leg_Raise", "ab_wheel": "Ab_Roller",
}

MUSCLES = {
    "quadriceps": "QUADS", "shoulders": "SHOULDERS", "abdominals": "CORE", "chest": "CHEST", "hamstrings": "HAMSTRINGS",
    "triceps": "TRICEPS", "biceps": "BICEPS", "lats": "BACK", "middle back": "BACK", "calves": "CALVES", "lower back": "BACK",
    "forearms": "FOREARMS", "glutes": "GLUTES", "traps": "BACK", "adductors": "QUADS", "neck": "SHOULDERS", "abductors": "GLUTES",
}

EQUIPMENT = {
    "barbell": "BARBELL", "dumbbell": "DUMBBELL", "body only": "BODYWEIGHT", None: "BODYWEIGHT", "cable": "CABLE",
    "machine": "MACHINE", "kettlebells": "KETTLEBELL", "bands": "BAND", "e-z curl bar": "EZ_BAR",
    "medicine ball": "OTHER", "exercise ball": "OTHER", "foam roll": "OTHER", "other": "OTHER",
}

LEVELS = {"beginner": "BEGINNER", "intermediate": "INTERMEDIATE", "expert": "EXPERT"}

# Word rules for German names, longest phrases first. Values: (german, role) with role
# "core" (the movement), "equip" (goes in front, joined with a hyphen) or "mod" (goes after a comma).
GERMAN = {
    "romanian deadlift": ("Rumänisches Kreuzheben", "core"), "stiff-legged deadlift": ("Kreuzheben mit gestreckten Beinen", "core"),
    "sumo deadlift": ("Sumo-Kreuzheben", "core"), "deadlift": ("Kreuzheben", "core"),
    "incline bench press": ("Schrägbankdrücken", "core"), "decline bench press": ("Negativ-Bankdrücken", "core"),
    "bench press": ("Bankdrücken", "core"), "floor press": ("Bodendrücken", "core"), "military press": ("Military Press", "core"),
    "shoulder press": ("Schulterdrücken", "core"), "overhead press": ("Überkopfdrücken", "core"), "push press": ("Push Press", "core"),
    "leg press": ("Beinpresse", "core"), "calf press": ("Wadendrücken", "core"), "chest press": ("Brustpresse", "core"),
    "calf raises": ("Wadenheben", "core"), "calf raise": ("Wadenheben", "core"), "lateral raise": ("Seitheben", "core"),
    "front raise": ("Frontheben", "core"), "leg raise": ("Beinheben", "core"), "rear delt": ("hintere Schulter", "mod"),
    "front squat": ("Frontkniebeuge", "core"), "split squat": ("Split Squat", "core"), "hack squat": ("Hackenschmidt", "core"),
    "full squat": ("Kniebeuge", "core"), "squats": ("Kniebeugen", "core"), "squat": ("Kniebeuge", "core"),
    "lunges": ("Ausfallschritte", "core"), "lunge": ("Ausfallschritt", "core"), "step-up": ("Step-up", "core"),
    "upright row": ("Aufrechtes Rudern", "core"), "rows": ("Rudern", "core"), "row": ("Rudern", "core"),
    "lat pulldown": ("Latzug", "core"), "pulldown": ("Zug nach unten", "core"), "pullups": ("Klimmzüge", "core"),
    "pull-up": ("Klimmzug", "core"), "chin-up": ("Klimmzug im Untergriff", "core"),
    "leg curls": ("Beinbeuger", "core"), "leg curl": ("Beinbeuger", "core"), "leg extensions": ("Beinstrecker", "core"),
    "leg extension": ("Beinstrecker", "core"), "triceps extension": ("Trizepsstrecken", "core"), "extensions": ("Strecken", "core"),
    "extension": ("Strecken", "core"), "skullcrusher": ("French Press", "core"), "kickback": ("Kickback", "core"),
    "curls": ("Curls", "core"), "curl": ("Curl", "core"), "flyes": ("Fliegende", "core"), "fly": ("Fliegende", "core"),
    "crossover": ("Crossover", "core"), "pushdown": ("Trizepsdrücken", "core"), "dips": ("Dips", "core"),
    "pushups": ("Liegestütze", "core"), "push-up": ("Liegestütz", "core"), "crunches": ("Crunches", "core"), "crunch": ("Crunch", "core"),
    "sit-up": ("Sit-up", "core"), "plank": ("Unterarmstütz", "core"), "shrugs": ("Shrugs", "core"), "shrug": ("Shrug", "core"),
    "hip thrust": ("Hip Thrust", "core"), "glute bridge": ("Glute Bridge", "core"), "good morning": ("Good Morning", "core"),
    "power clean": ("Power Clean", "core"), "hang clean": ("Hang Clean", "core"), "clean": ("Umsetzen", "core"),
    "snatch": ("Reißen", "core"), "jerk": ("Ausstoßen", "core"), "swings": ("Swings", "core"), "swing": ("Swing", "core"),
    "press": ("Drücken", "core"), "raise": ("Heben", "core"), "twist": ("Drehung", "core"), "jump": ("Sprung", "core"),
    "smith machine": ("Multipresse", "equip"), "barbell": ("Langhantel", "equip"), "dumbbells": ("Kurzhantel", "equip"),
    "dumbbell": ("Kurzhantel", "equip"), "cable": ("Kabel", "equip"), "machine": ("Maschine", "equip"),
    "kettlebell": ("Kettlebell", "equip"), "band": ("Band", "equip"), "ez-bar": ("SZ", "equip"), "e-z bar": ("SZ", "equip"),
    "lever": ("Hebel", "equip"), "leverage": ("Hebel", "equip"), "medicine ball": ("Medizinball", "equip"),
    "one-arm": ("einarmig", "mod"), "one arm": ("einarmig", "mod"), "single-arm": ("einarmig", "mod"), "single arm": ("einarmig", "mod"),
    "one-leg": ("einbeinig", "mod"), "one leg": ("einbeinig", "mod"), "single-leg": ("einbeinig", "mod"), "single leg": ("einbeinig", "mod"),
    "seated": ("sitzend", "mod"), "standing": ("stehend", "mod"), "lying": ("liegend", "mod"), "kneeling": ("kniend", "mod"),
    "hanging": ("hängend", "mod"), "incline": ("Schrägbank", "mod"), "decline": ("Negativbank", "mod"),
    "close-grip": ("enger Griff", "mod"), "wide-grip": ("breiter Griff", "mod"), "reverse grip": ("Untergriff", "mod"),
    "reverse": ("umgekehrt", "mod"), "alternating": ("abwechselnd", "mod"), "alternate": ("abwechselnd", "mod"),
    "bent over": ("vorgebeugt", "mod"), "bent-over": ("vorgebeugt", "mod"), "rope": ("mit Seil", "mod"), "weighted": ("mit Gewicht", "mod"),
    "overhead": ("über Kopf", "mod"), "behind the neck": ("hinter dem Nacken", "mod"), "narrow stance": ("enger Stand", "mod"),
    "wide stance": ("breiter Stand", "mod"), "neutral grip": ("Neutralgriff", "mod"), "side": ("seitlich", "mod"),
}
STOP = {"with", "on", "the", "a", "to", "and", "-", "of", "for", "in", "attachment", "version", "medium", "grip"}


GERMAN.update({
    "stiff-legged": ("gestreckte Beine", "mod"), "stiff leg": ("gestreckte Beine", "mod"), "trap bar": ("Trap-Bar", "equip"),
    "two-arm": ("beidarmig", "mod"), "two arm": ("beidarmig", "mod"), "two-dumbbell": ("zwei Kurzhanteln", "mod"),
    "two kettlebells": ("zwei Kettlebells", "mod"), "rear": ("rückwärts", "mod"), "chains": ("Ketten", "rest"),
    "bands": ("Bändern", "rest"), "ball": ("Ball", "rest"), "bench": ("Bank", "rest"), "floor": ("Boden", "rest"),
    "from deficit": ("aus dem Defizit", "mod"), "box": ("Box", "rest"), "landmine": ("Landmine", "rest"),
})


def german_name(english):
    """
    A German name only when every word is known and there is exactly one movement; otherwise the
    English name (gym people know those, and a half-translated name reads worse than either).
    English word order is kept, "with X" becomes "mit X".
    """
    text = re.sub(r"[()]", " ", english.lower())
    text = re.sub(r"\s+", " ", text).strip()
    before, core, after, equip, mods = [], [], [], [], []
    keys = sorted(GERMAN, key=len, reverse=True)
    pending_with = False
    while text:
        for key in keys:
            if text.startswith(key) and (len(text) == len(key) or not text[len(key)].isalnum()):
                word, role = GERMAN[key]
                text = text[len(key):].strip()
                break
        else:
            token, _, text = text.partition(" ")
            if token == "with":
                pending_with = True
                continue
            if token in STOP:
                continue
            return english  # an unknown word: keep the English name
        if role == "core":
            core.append(word)
        elif role == "equip":
            equip.append(word)
        elif role == "mod":
            mods.append(word)
        else:
            target = after if core else before
            target.append(("mit " if pending_with else "") + word)
            pending_with = False
    if len(core) != 1:
        return english  # no movement, or two of them ("Snatch Deadlift"): English reads better
    if len(set(mods)) > 2:
        return english  # three or more add-ons make a German name longer and clumsier than the English one
    name = " ".join(before + core + after)
    if equip:
        name = "-".join(dict.fromkeys(equip)) + "-" + name
    if mods:
        name += ", " + ", ".join(dict.fromkeys(mods))
    return name[0].upper() + name[1:]


def pattern(ex, primary):
    name = ex["name"].lower()
    if ex.get("mechanic") == "isolation":
        return "ISOLATION"
    if primary == "CORE":
        return "CORE"
    if "lunge" in name or "split squat" in name or "step" in name:
        return "LUNGE"
    if primary == "QUADS":
        return "SQUAT"
    if primary in ("HAMSTRINGS", "GLUTES") or "deadlift" in name or "good morning" in name:
        return "HINGE"
    if ex.get("force") == "push":
        return "VERTICAL_PUSH" if primary == "SHOULDERS" or "overhead" in name or "military" in name else "HORIZONTAL_PUSH"
    if ex.get("force") == "pull":
        return "VERTICAL_PULL" if any(w in name for w in ("pulldown", "pull-up", "pullup", "chin")) else "HORIZONTAL_PULL"
    return "ISOLATION"


def equipment(ex):
    if "smith" in ex["name"].lower():
        return "SMITH"
    return EQUIPMENT.get(ex.get("equipment"), "OTHER")


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
    db = {ex["id"]: ex for ex in json.load(open(os.path.join(src_root, "dist", "exercises.json"), encoding="utf-8"))}
    ours = own_exercises()
    print("own exercises:", len(ours))
    out_images = os.path.join(ROOT, "public", "exercises")
    catalog = []
    matched = set()

    for our_id, own in ours.items():
        db_id = OUR_IDS.get(our_id)
        ex = db.get(db_id) if db_id else None
        entry = dict(own)
        if ex:
            matched.add(db_id)
            entry["level"] = LEVELS.get(ex.get("level"), "BEGINNER")
            entry["instructionsEn"] = ex.get("instructions", [])
            entry["images"] = convert_images(src_root, ex, our_id, out_images)
        catalog.append(entry)

    for db_id, ex in sorted(db.items()):
        if db_id in matched or not wanted(ex) or not ex.get("primaryMuscles"):
            continue
        primary = MUSCLES.get(ex["primaryMuscles"][0])
        if primary is None:
            continue
        our_id = slug(db_id)
        secondary = list(dict.fromkeys(m for m in (MUSCLES.get(x) for x in ex.get("secondaryMuscles", [])) if m and m != primary))
        catalog.append({
            "id": our_id,
            "nameDe": german_name(ex["name"]),
            "nameEn": ex["name"],
            "primary": primary,
            "secondary": secondary,
            "equipment": equipment(ex),
            "pattern": pattern(ex, primary),
            "level": LEVELS.get(ex.get("level"), "BEGINNER"),
            "instructionsEn": ex.get("instructions", []),
            "images": convert_images(src_root, ex, our_id, out_images),
        })

    # Hand-written German instructions (scripts/instructions-de.json), keyed by our id.
    instructions_de = json.load(open(os.path.join(ROOT, "scripts", "instructions-de.json"), encoding="utf-8"))
    for entry in catalog:
        if entry["id"] in instructions_de:
            entry["instructionsDe"] = instructions_de[entry["id"]]

    # Photos of exercises no longer in the catalog are removed from the hosting folder.
    keep = {e["id"] for e in catalog}
    if os.path.isdir(out_images):
        import shutil
        for folder in os.listdir(out_images):
            if folder not in keep:
                shutil.rmtree(os.path.join(out_images, folder))

    out = os.path.join(ROOT, "app", "src", "main", "assets", "exercises.json")
    os.makedirs(os.path.dirname(out), exist_ok=True)
    with open(out, "w", encoding="utf-8") as f:
        json.dump({"version": 1, "source": "free-exercise-db (Unlicense)", "exercises": catalog}, f, ensure_ascii=False, separators=(",", ":"))
    print("exercises:", len(catalog), "with images:", sum(1 for e in catalog if e.get("images")),
          "German names:", sum(1 for e in catalog if e["nameDe"] != e["nameEn"]),
          "German instructions:", sum(1 for e in catalog if e.get("instructionsDe")))


if __name__ == "__main__":
    main(sys.argv[1])
