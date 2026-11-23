# Exercise catalog

About 160 exercises a normal gym, a CrossFit box or a Hyrox class actually uses, picked by hand in
`scripts/exercise_list.py` (the only place that decides what the app offers). German names are
written by hand. Photos and English instructions come from
[free-exercise-db](https://github.com/yuhonas/free-exercise-db) (Unlicense, public domain) where it
has the exercise; the rest (Toes to Bar, Thruster, Hyrox stations …) have our own instructions in
both languages and show an icon tile instead of a photo.

| What | Where |
|---|---|
| The list | `scripts/exercise_list.py` (`core`, `db`, `own` rows; `OWN_INSTRUCTIONS`) |
| Catalog the app loads | `app/src/main/assets/exercises.json` (committed, generated) |
| Instructions | Every offered exercise has English and German. German: `scripts/instructions-de.json`; both languages: `OWN_INSTRUCTIONS` (own rows, and database rows whose text is missing or unusable); English for core exercises without a database text: `INSTRUCTIONS_EN` |
| Photos | `public/exercises/{id}/0.webp` (start) and `1.webp` (end), 480 px, served by Firebase Hosting at `https://knurl-gym.web.app/exercises/...`, not committed |
| Import | `scripts/import_exercises.py` |

## Regenerate

Needs Python 3 with Pillow.

```bash
git clone --depth 1 https://github.com/yuhonas/free-exercise-db.git ../free-exercise-db
python scripts/import_exercises.py ../free-exercise-db
firebase deploy --only hosting --account davidbuechel92@gmail.com --project knurl-gym
```

The script keeps existing photos, so a re-run only converts new ones. It prints which offered
exercises have no photo.

## IDs, names, retired exercises

- IDs never change: workouts and routines reference them. Core IDs come from the Kotlin `core`
  list; database rows get `slug(databaseId)` (so ids from earlier imports stay); own rows name theirs.
- Names may be improved any time. English names are Title Case.
- An exercise that leaves the list stays in `exercises.json` as `archived` (names, muscles, photo
  count only), so old workouts keep their names. `RetiredExercises` offers it again to whoever
  still uses it (history, a routine, the running workout); for everyone else it is gone from the
  library and search.

## Search

`shared/.../search/ExerciseSearch.kt`: both languages, umlauts and `ae/oe/ue` spellings meet,
gym shorthand (BD, RDL, OHP, KH, LH …), muscle words (Bizeps, Brust …), one typo per word (two from
eight letters). Every query word must match. Tested in `ExerciseSearchTest`.

## Own exercises

People add their own from Training → Exercises ("+ Custom") or from the picker when a search finds
nothing. One name for both languages, main muscle, up to three more, equipment, own cues and an
optional photo (gallery or camera, cropped to a 480 px square WebP on the device, stored in
`exercisePhotos`). Ids start with `custom_`. Deleting one that the history or a plan uses only
archives it (`CustomExercises.isUsed`); archived ones never show in the library or search.
