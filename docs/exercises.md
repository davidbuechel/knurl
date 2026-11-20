# Exercise catalog

About 740 exercises: our own 58 core exercises (`shared/.../catalog/ExerciseCatalog.kt`, `core`) plus
strength, powerlifting, olympic, strongman and plyometric exercises from
[free-exercise-db](https://github.com/yuhonas/free-exercise-db) (Unlicense, public domain).
Stretching and cardio are left out.

| What | Where |
|---|---|
| Catalog the app loads | `app/src/main/assets/exercises.json` (committed) |
| Photos | `public/exercises/{id}/0.webp` (start) and `1.webp` (end), 480 px, served by Firebase Hosting at `https://knurl-gym.web.app/exercises/...`, not committed |
| Import | `scripts/import_exercises.py` |

## Regenerate

Needs Python 3 with Pillow.

```bash
git clone --depth 1 https://github.com/yuhonas/free-exercise-db.git ../free-exercise-db
python scripts/import_exercises.py ../free-exercise-db
firebase deploy --only hosting --account davidbuechel92@gmail.com --project knurl-gym
```

The script keeps existing photos, so a re-run only converts new ones.

## IDs and names

- Our 58 core IDs never change (workouts reference them). `OUR_IDS` in the script maps each to its
  database entry for photos and instructions; `machine_hip_thrust` has no match and no photo.
- Imported exercises get `slug(databaseId)` as id, e.g. `barbell_glute_bridge`. Never rename one.
- German names: hand-written for the core 58; built from word rules (`GERMAN`) for the rest. They
  read reasonably but not always naturally ("Reißen Kreuzheben") and need a review pass before release.
- Instructions are English for now (`instructionsEn`); `instructionsDe` is shown when present, and the
  app says when it falls back to English.

## Search

`shared/.../search/ExerciseSearch.kt`: both languages, umlauts and `ae/oe/ue` spellings meet,
gym shorthand (BD, RDL, OHP, KH, LH …), muscle words (Bizeps, Brust …), one typo per word (two from
eight letters). Every query word must match. Tested in `ExerciseSearchTest`.
