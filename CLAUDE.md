# Knurl — working rules for Claude

Gym-tracking app (working title). Android (Kotlin, Jetpack Compose, Material 3), models and
calculations in Kotlin Multiplatform (`:shared`). Firebase backend (Auth, Firestore) in
`europe-west3`, project `knurl-gym`; works offline through Firestore's cache. UI in German and English.

## How a change gets done
1. **Understand** — read the code that is there; match its style. Plan multi-step work.
2. **Build** — rules and calculations in `:shared`, screens in `:app`.
3. **Test** — see `docs/testing.md`: `./gradlew testDebugUnitTest :shared:testAndroidHostTest`,
   and `cd firestore-tests && npm test` when rules change. Every calculation in `:shared` has a
   test. Then look at it on a device or the emulator.
4. **Deploy rules before the app code that needs them** (`docs/firebase.md`).
5. **Everything that describes it** moves in the same change: DE + EN strings, README, docs.
6. Commit.

## Standalone project
- Knurl is completely independent. **Never bring code, config, text, accounts or references
  from any other project into this repo.** Write everything here from scratch.

## Git
- Commit to `main`. No branches or PRs unless asked.
- No `Co-Authored-By` trailer in commit messages.
- Stage files by explicit path; never `git add -A` / `git add .`.
- Private GitHub account; remotes use the SSH alias `github-private`.

## App rules of thumb
- **Rules, models and calculations live in `:shared`.** No Android or Java APIs there; time is
  `kotlin.time` and `kotlinx-datetime`. The app converts only at its edges (formatting).
- **Weights are stored in kilograms**, always. Convert only for display.
- **Records are derived, never stored by hand** (`History.records`).
- **One screen, one implementation.** Vary a screen with a parameter (e.g. `ExercisesScreen`
  with `pickForWorkout`), never copy it. Shared pieces live in `feature/common` or `ui/brand`.
- **One set of building blocks** (`feature/common/DesignSystem.kt`): `SectionHeader` above every
  group; rows in a `ListGroup` (`ListRow`, `SwitchRow`, `ValueRow`, `SegmentedRow`; long lazy lists
  use `Modifier.groupedItem`); three numbers in a `KpiRow`; `KnurlCard` only for charts and forms.
  Surfaces have no border. Colour: primary is the accent, green only for done, red (tertiary) only
  for records, purple only for supersets. Loading via `BrandLoadingBox`/`BrandBusyOverlay`, empty
  states via `BrandEmptyState`; no stock spinners.
- **Texts:** every count goes through `<plurals>`. German says "Training(s)" for workouts
  ("Einheit" means kg/lb). English exercise names are in Title Case.
- **Navigation through `Navigator`** (`go.to` / `go.back`); motion is set once in `ScreenMotion`
  (a stack: slide over, the screen below drifts and darkens; the back gesture shrinks the screen
  and follows the finger). Destinations use `screen<Route>(nav) { go, entry -> }`, never
  `composable`; screens add no transitions. Each destination's `Navigator` only acts while its
  screen is settled on top, so navigation from an effect waits for RESUMED (`withResumed`).
- **Back never loses work.** A `BackHandler` closes what is open first (number pad, sheet) and
  guards unsaved edits, and is only enabled while there is something to guard, so the back
  gesture animates normally otherwise. Back on a tab other than Today returns to Today.
- **Numbers read the local way:** `Formatting.weight/tonnage/percent/length`, units joined with
  `Formatting.NBSP`; the decimal key and typed values use `Formatting.decimalSeparator()`.
  Screens add no transitions.
- **Notifications:** every kind is a `Channel` with a switch in `NotificationSettings`; add both,
  plus a row in `docs/notifications.md`. The rest timer is app-wide (`RestTimer`), never screen state.
- **Health Connect** (`core/health/HealthSync.kt`): one switch per kind of data, off until turned on and
  only on while granted; heart rate never leaves the phone. New data kinds need a manifest permission,
  a row in `docs/health-connect.md` and the privacy policy (`scripts/legal.py`).
- **Derived, not stored:** records, achievements, kcal and month reviews are computed from the history in
  `:shared` (`History`, `Achievements`, `Calories`, `MonthRecaps`); never write them to Firestore.
- **ViewModels via `knurlViewModel { ... }`**, dependencies from `AppContainer`.
- **All formatting in `Formatting`** (locale-aware numbers, durations, dates). Weights on screen go
  through `weightText` / `setText` / `tonnageText` (feature/common/Units.kt), which follow the chosen
  kg/lb unit (`LocalWeightUnit`); typed weights are converted back with `Formatting.fromUnit`.
- **Two kinds of settings:** account-wide in `Settings` (Firestore, follows the person), device-only in
  `AppPreferences` (theme, language via AppCompat, screen on, rest feedback, crash-report consent).
- **Exercise IDs are permanent.** Workouts reference them; never rename or reuse one. The catalog is
  generated (`docs/exercises.md`); edit `ExerciseCatalog.core` or the import script, not the JSON.
- **Data access only through `TrainingRepository`**; it talks to a `TrainingStore`
  (`FirestoreTrainingStore` in the app, `InMemoryTrainingStore` in tests). Screens never touch Firebase.
- **The running workout is local-first**: applied in the repository, written behind. Never feed
  `activeChanges` echoes back over local edits.
- **Data shape changes:** keep old documents readable (defaults for new fields), update
  `firestore.rules` and its tests in the same change.

## Backend rules of thumb
- Every document lives under `users/{uid}`; only the owner reads or writes it. New collections need
  a rule, a shape check and tests in `firestore-tests/`.
- Every write carries `updatedAt` = server time.
- Firebase commands always pass `--account davidbuechel92@gmail.com --project knurl-gym`.
- Stay on the Spark plan unless a feature truly needs Blaze; say so before adding Functions or Storage.

## Design
- Steel neutrals, chalk surfaces, cobalt as the brand colour. Plate colours (`Knurl.colors.plate()`)
  carry data only, never decoration.
- The mark is three plates rising on a bar end (`KnurlMark`, `KnurlAppIcon` in `ui/brand`); geometry shared
  with `ic_launcher_foreground.xml` and `ic_notification.xml`.
- **One loading language, built from the mark:** `KnurlLoader` (plates load one by one), `KnurlProgress`
  (determinate), `BrandLoadingBox`, `BrandBusyOverlay`, `BrandButtonLabel`. No stock spinners or progress bars.
- **Building blocks** in `feature/common/DesignSystem.kt`: `ListGroup`/`ListRow`, `SectionHeader`, `KpiRow`,
  `SegmentedTabs`, `HeroCard`, `WeekDots`, `Pill`. Surfaces have no border; no card inside a card.
- Fonts: Archivo (display, numbers) and Inter (text) as variable fonts; styles only via `KnurlTypography`.
- Both themes are designed; every colour comes from `ui/theme`.

## Privacy (DSGVO)
- Anything that collects data on its own (analytics, crash reporting, ads) stays OFF until the
  user consents in the app.
- New personal data needs export and deletion paths.

## Never
- Put secrets in git: keystore, `keystore.properties`, `local.properties`, `google-services.json`.
- Create accounts or type passwords in the live project; test with anonymous accounts or the emulator.
