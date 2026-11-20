# Knurl — working rules for Claude

Gym-tracking app (working title). Android (Kotlin, Jetpack Compose, Material 3), models and
calculations in Kotlin Multiplatform (`:shared`). Offline-first: all data on the device.
UI in German and English.

## How a change gets done
1. **Understand** — read the code that is there; match its style. Plan multi-step work.
2. **Build** — rules and calculations in `:shared`, screens in `:app`.
3. **Test** — `./gradlew testDebugUnitTest :shared:testAndroidHostTest`. Every calculation in
   `:shared` has a test. Then look at it on a device or the emulator.
4. **Everything that describes it** moves in the same change: DE + EN strings, README, docs.
5. Commit.

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
- **One card, one loading language:** `KnurlCard`, `BrandLoadingBox`, `BrandEmptyState`.
  No stock spinners.
- **Navigation through `Navigator`** (`go.to` / `go.back`); motion is set once in `ScreenMotion`.
  Screens add no transitions.
- **ViewModels via `knurlViewModel { ... }`**, dependencies from `AppContainer`.
- **All formatting in `Formatting`** (locale-aware numbers, durations, dates).
- **Exercise IDs are permanent.** Workouts reference them; never rename or reuse one.
- **Data shape changes:** bump `TrainingData.version` and migrate; never break existing files.

## Design
- Steel neutrals, chalk surfaces, cobalt as the brand colour. Plate colours (`Knurl.colors.plate()`)
  carry data only, never decoration.
- The mark is the monogram K (`KnurlMark`, `KnurlAppIcon` in `ui/brand`); its geometry is shared with
  `ic_launcher_foreground.xml` and `ic_notification.xml`. No knurl hatch pattern anywhere.
- Fonts: Archivo (display, numbers) and Inter (text) as variable fonts; styles only via `KnurlTypography`.
- Both themes are designed; every colour comes from `ui/theme`.

## Privacy (DSGVO)
- No internet permission in phase 1. Anything that collects data on its own (analytics, crash
  reporting, ads) stays OFF until the user consents in the app.
- New personal data needs export and deletion paths.

## Never
- Put secrets in git: keystore, `keystore.properties`, `local.properties`, `google-services.json`.
