# Knurl

Gym-Tracking für Android: Übungen loggen, Gewichte pro Übung verfolgen, Fortschritt verstehen.
Kotlin + Jetpack Compose, Modelle und Rechnungen in Kotlin Multiplatform (`:shared`) für eine spätere iOS-App.

> „Knurl“ ist der Working Title. Zeichen: ein geometrisches K, dessen oberer Arm in einer Hantelscheibe endet.

## Phase 1 (aktuell)
- **Heute:** nächster Plan der Reihe nach, laufendes Training fortsetzen, Woche auf einen Blick, letzte Rekorde.
- **Training:** Startpläne (Push, Pull, Beine, Ganzkörper), freies Training, Verlauf mit Löschen.
- **Live-Workout:** Sätze mit Gewicht/Wdh.-Steppern (Antippen zum Eintippen), Aufwärm- und Drop-Sätze,
  Abhaken mit Haptik, Pausentimer, „Zuletzt“-Werte, Progressionsvorschlag. Gewichtsänderungen laufen an
  offene Folgesätze weiter.
- **Übungen:** 58 Übungen mit Muskel und Gerät, Suche und Muskelfilter. Detail: geschätztes 1RM mit Verlauf,
  Bestwerte, Scheibenrechner, alle Einheiten.
- **Fortschritt:** Einheiten, Volumen, Rekorde der Woche, harte Sätze pro Muskel mit Zielbereich 10–20,
  Volumen der letzten 8 Wochen, Wochenserie.
- **Profil:** Stangengewicht, Standardpause, Wochenziel.

Daten liegen in Firebase (Projekt `knurl-gym`, Frankfurt) unter dem eigenen Konto. Jede Installation startet
mit einem anonymen Konto; Training funktioniert dank Firestore-Cache auch offline. Details: `docs/firebase.md`.

## Fahrplan
1. Loggen, offline ← *hier*
2. Statistik vertiefen: eigene Pläne bearbeiten, eigene Übungen, Körpermaße, CSV-Export, kg/lb
3. Optionales Konto mit Backup, Health Connect, Wear OS, iOS über `:shared`

## Setup
- Ordner in Android Studio öffnen, oder bauen mit `./gradlew assembleDebug`.
- `local.properties` (SDK-Pfad) legt Android Studio selbst an; sie wird nicht committet.
- `app/google-services.json` ist nicht im Repo; holen wie in `docs/firebase.md` beschrieben.
- Tests: `./gradlew testDebugUnitTest :shared:testAndroidHostTest` und `cd firestore-tests && npm test`
  (siehe `docs/testing.md`)

## Aufbau
```
app/      Android-App (Compose, Material 3)
  data/       TrainingRepository (Quelle der Wahrheit), TrainingStore-Interface, firebase/ (Auth, Firestore)
  feature/    today, training, workout, exercises, progress, settings, common
  ui/         theme (Farben, Typo, Shapes), brand (Zeichen, App-Icon, Lade-/Leerzustände), navigation
shared/   Kotlin Multiplatform
  domain/model     Exercise, Workout, SetEntry, Routine
  domain/catalog   Übungskatalog (DE/EN), Startpläne
  domain/calc      OneRepMax, Volume, HardSets, Progression, PlateCalculator, History, Weeks
  domain/          WorkoutPlanning (Training aus Plan bauen, Sätze bearbeiten, beenden)
```

## Design
Stahl-Neutrale, Kreide-Flächen, Kobalt (#2448E8) als Markenfarbe. Die Farben der Wettkampfscheiben
(25 rot, 20 blau, 15 gelb, 10 grün, 5 grau) sind die Datenpalette. Hell- und Dunkelmodus folgen dem System.
Schriften: Archivo (Display und Zahlen, leicht breit) und Inter (Text), beide als variable Fonts unter
`res/font`, Lizenzen (OFL) in `licenses/`.

## Stack
AGP 9.5 (built-in Kotlin), Kotlin 2.4, Gradle 9.8, Compose BOM 2026.08, Material 3, Navigation Compose
(typsichere Routen), kotlinx-serialization, kotlinx-datetime, Firebase (Auth, Firestore). Package `de.knurl` · minSdk 29 · targetSdk 36 ·
compileSdk 37. UI auf Deutsch und Englisch.
