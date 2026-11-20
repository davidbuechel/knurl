# Exercise catalog

About 500 exercises for regular gym-goers: our own 66 core exercises (`shared/.../catalog/ExerciseCatalog.kt`,
`core`, including HIIT/Hyrox stations such as burpees, wall balls, sled push/pull, farmer's carry, SkiErg) plus
strength, cardio and classic plyometric exercises from
[free-exercise-db](https://github.com/yuhonas/free-exercise-db) (Unlicense, public domain). Left out on purpose:
powerlifting specials, olympic lifts, strongman, expert-level exotics, stretching (`wanted()` in the script).

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
- German names: hand-written for the core list; for imports built from word rules (`GERMAN`), but only
  when every word is known, there is exactly one movement and at most two add-ons. Otherwise the English
  name is used in German too (gym people know them).
- Instructions: German for all core exercises (`scripts/instructions-de.json`, merged by the script),
  English for the rest; the app says when it falls back to English. HIIT/Hyrox core exercises have no photo yet.

## Search

`shared/.../search/ExerciseSearch.kt`: both languages, umlauts and `ae/oe/ue` spellings meet,
gym shorthand (BD, RDL, OHP, KH, LH …), muscle words (Bizeps, Brust …), one typo per word (two from
eight letters). Every query word must match. Tested in `ExerciseSearchTest`.
