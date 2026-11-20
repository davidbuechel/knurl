# Testing

| Layer | Command | What it covers |
|---|---|---|
| Shared rules and calculations | `./gradlew :shared:testAndroidHostTest` | 1RM, volume, hard sets, progression, plates, records, weeks, workout planning |
| App | `./gradlew testDebugUnitTest` | `TrainingRepository` against `InMemoryTrainingStore`, formatting |
| Firestore rules | `cd firestore-tests && npm test` | ownership and document shapes under `users/{uid}` |
| On the device | `./gradlew installDebug` | see below |

`InMemoryTrainingStore` (app/src/test) behaves like Firestore's local cache: writes show up at
once, and our own writes to the running workout are not echoed back.

## On the device or emulator

After a change, look at it in the app. For data changes check at least:
- first start signs in anonymously and shows the starter routines
- a workout can be logged with the network off and appears after going online
- reinstalling and signing in with the same account brings the data back (once linking exists)
