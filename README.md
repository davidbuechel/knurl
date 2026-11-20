# Knurl

Gym-Tracking für Android: Übungen loggen, Gewichte pro Übung verfolgen, Fortschritt verstehen.
Kotlin + Jetpack Compose, Modelle und Rechnungen in Kotlin Multiplatform (`:shared`) für eine spätere iOS-App.

> „Knurl“ ist der Working Title. Zeichen: drei aufsteigende Hantelscheiben auf einem Stangenende; dieselbe Form ist die Ladeanimation.

## Funktionen
- **Erster Start:** Onboarding mit Ziel, Erfahrung, Trainingstagen und Einheit; daraus Startpläne und Wochenziel.
- **Heute:** nächster Plan der Reihe nach, laufendes Training fortsetzen, Woche auf einen Blick mit Volumen-Trend,
  größte Steigerungen der letzten vier Wochen, letzte Rekorde.
- **Training:** Pläne anlegen und bearbeiten (Übungen, Reihenfolge, Sätze, Wdh.-Bereich, Pause, Supersätze),
  duplizieren, verschieben, löschen mit Rückgängig. Freies Training oder jedes vergangene Training als Plan speichern.
  Vergangene Trainings bearbeiten.
- **Live-Workout:** eigene Zahlentastatur, automatisch weiter zum nächsten Satz, Supersätze im Wechsel, Aufwärm- und
  Drop-Sätze, RIR optional, Pausenleiste, „Zuletzt“-Werte und Progressionsvorschlag.
- **Zusammenfassung:** Rekord-Moment, Steigerung pro Übung gegenüber dem letzten Mal, Volumen-Vergleich, als Plan übernehmen.
- **Übungen:** rund 500 Übungen für normale Gym-Gänger (inkl. HIIT/Hyrox) mit Fotos und Anleitung, Suche in beiden
  Sprachen mit Kürzeln, Muskelnamen und Tippfehler-Toleranz, Filter, Favoriten, „Zuletzt trainiert“.
  **Eigene Übungen** mit Muskeln, Gerät, Hinweisen und eigenem Foto (Galerie oder Kamera).
  Detail: Bilder, e1RM-Verlauf (3M/6M/1J/Alles), Bestwerte pro Wiederholungszahl, Scheibenrechner. Details: `docs/exercises.md`.
- **Statistik:** Woche/Monat/Jahr mit Vergleich zum Vorzeitraum bis zum selben Tag, Muskel-Heatmap (vorn/hinten),
  harte Sätze pro Muskel mit Zielbereich 10–20, größte Steigerungen, Volumen-Verlauf, Wochenserie;
  Körpergewicht und Trainingskalender als eigene Seiten.
- **Erfolge:** 31 Medaillen (Bronze/Silber/Gold) für Trainings, Serien, Volumen, Kraft-Meilensteine bei Bankdrücken,
  Kniebeuge und Kreuzheben, Rekorde und mehr; abgeleitet aus dem Verlauf, nie gespeichert. Wochenziel als Ring auf „Heute“.
- **Monatsrückblick:** „Dein Oktober“ mit Volumen, Zeit, Rekorden, Lieblingsübung, größtem Schritt, Muskelkarte und
  Freischaltungen, als Bild teilbar; in der ersten Woche eines Monats auf „Heute“, sonst über den Kalender.
- **Profil und Konto:** anonymer Start, Konto sichern mit Google oder E-Mail (Daten bleiben), Anmelden, Abmelden,
  Konto löschen, Export als JSON und CSV.
- **Einstellungen:** Design (System/Hell/Dunkel), Sprache (System/Deutsch/Englisch), kg/lb, Stange, vorhandene
  Scheiben, Standardpause, Wochenziel, Bildschirm anlassen, Pausen-Vibration/-Ton, Benachrichtigungen einzeln,
  Absturzberichte (aus bis eingeschaltet), Impressum/Datenschutz (aus `scripts/legal.py`, auch unter knurl-gym.web.app/impressum.html und /datenschutz.html).
- **Benachrichtigungen:** Pausen-Alarm im Hintergrund, laufendes Training auf dem Sperrbildschirm (mit Countdown),
  Trainingserinnerung an gewählten Tagen, Serien-Erinnerung, Wochenrückblick, Push (FCM) für Konto und News.
  Details: `docs/notifications.md`.

Daten liegen in Firebase (Projekt `knurl-gym`, Frankfurt) unter dem eigenen Konto. Jede Installation startet
mit einem anonymen Konto; Training funktioniert dank Firestore-Cache auch offline. Details: `docs/firebase.md`.

## Fahrplan
1. Loggen, offline, Konto, Benachrichtigungen ✓
2. Redesign, Onboarding, Supersätze ✓
3. Eigene Übungen ✓, Statistik ✓, Erfolge und Monatsrückblick ✓, kcal-Schätzung, Health Connect ← *hier*
4. Wear OS, iOS über `:shared`

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
