# Firebase: how Knurl works with it

Everything Firebase-side lives in this repo and is deployed from here.

| What | Where |
|---|---|
| Project / account | `.firebaserc` (`knurl-gym`), always `--account davidbuechel92@gmail.com` |
| Firestore rules / indexes | `firestore.rules`, `firestore.indexes.json` |
| Rules tests | `firestore-tests/*.test.mjs` (emulator, no credentials) |
| App config | `app/google-services.json`, git-ignored |

Region: **europe-west3** (Frankfurt). Plan: **Spark** (free). Auth, Firestore and receiving FCM
messages need nothing more. Cloud Functions or Cloud Storage would need Blaze.

## Data model

```
users/{uid}                      settings (map), schemaVersion, createdAt
users/{uid}/workouts/{id}        finished workouts (shared model Workout)
users/{uid}/routines/{id}        routines (Routine), ordered by position
users/{uid}/customExercises/{id} the user's own exercises (Exercise, isCustom = true)
users/{uid}/state/active         the running workout; deleted when it ends
users/{uid}/devices/{installId}  FCM token, platform, locale
```

Every document the app writes carries `updatedAt` = server time; the rules insist on it.
Documents are the shared `@Serializable` models mapped by `data/firebase/FirestoreJson.kt`, so the
models in `:shared` never see Firebase types.

## Accounts

Every install signs in anonymously on first start (needs the network once). Linking to Google or
e-mail keeps the same uid, so no data moves. Deleting the account deletes `users/{uid}` and all
subcollections from the app.

## Offline

Firestore's persistent cache is on by default. Reads come from the cache, writes are queued and
sent when the phone is online again. The running workout is applied in `TrainingRepository` first
and written behind; the store reports only confirmed or remote changes (`activeChanges`), so a
quick series of taps never jumps back.

## One-time setup on a machine

```bash
firebase login:add davidbuechel92@gmail.com
```

Get the app config (after cloning, or after adding a SHA fingerprint or sign-in provider):

```bash
firebase apps:sdkconfig ANDROID 1:746740081929:android:bb248f100e89af6e91dfd8 --account davidbuechel92@gmail.com --project knurl-gym --out app/google-services.json
```

Sign-in providers (Anonymous, E-mail/Password, Google) are switched on in the console:
Authentication → Sign-in method. The API route would upgrade the project to Identity Platform
(billing), so this one step stays manual.

## Daily use

```bash
cd firestore-tests && npm test        # rules tests in the emulator
firebase deploy --only firestore --account davidbuechel92@gmail.com --project knurl-gym
```

Deploy rules before app code that needs them.
