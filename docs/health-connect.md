# Health Connect

Android's shared health store. Watches and other apps write there (Fitbit, Samsung Health, Pixel
Watch; Garmin Connect one way: heart rate, weight, workouts). Knurl talks only to Health Connect,
never to a vendor directly. Code: `core/health/HealthSync.kt`, settings: `feature/settings/HealthSettings.kt`.

| Switch (Settings → Connections) | Permissions | What happens |
|---|---|---|
| Share workouts | WRITE_EXERCISE, WRITE_TOTAL_CALORIES_BURNED | After a workout is finished or edited: an `ExerciseSessionRecord` (strength training, or HIIT when most exercises are conditioning stations) and the estimated kcal. Client record ids are the workout id (`…-kcal` for energy), so edits replace and deletes remove them. |
| Heart rate from your watch | READ_HEART_RATE | Average and peak during the workout, shown in the summary. With year of birth and sex (optional, Settings) kcal come from Keytel's formula instead of MET. Heart rate stays on the phone. |
| Sync body weight | READ_WEIGHT, WRITE_WEIGHT | Weights logged in Knurl go out; weights from other apps come in on every app start (Health Connect allows reading the last 30 days), stored as `hc-<record id>` so they never duplicate. |

Every switch is off until turned on, and only stays on while Health Connect grants its permissions
(checked on every resume). `HealthPrivacyActivity` answers Health Connect's "why" link with the
privacy policy (section 7).

Below Android 14 Health Connect is an app; Settings offers to install it. On devices without it the
section says so.

## Play Store

Health permissions need the Health apps declaration in the Play Console. Text for it:

> Knurl is a strength training log. With the person's consent it writes finished workouts (time,
> type, estimated energy) to Health Connect, reads heart rate during a workout to show average and
> peak and to estimate energy, and syncs body weight with scales and watches. Health data is used
> only for these features, never for advertising, and is not sold or shared. Heart rate is not
> uploaded. Each kind of data has its own switch and can be turned off in the app or in Health Connect.

Permissions to list: `WRITE_EXERCISE`, `WRITE_TOTAL_CALORIES_BURNED`, `READ_HEART_RATE`, `READ_WEIGHT`, `WRITE_WEIGHT`.
Privacy policy: https://knurl-gym.web.app/datenschutz.html (English: /en/privacy.html).

## Testing

Health Connect Toolbox (Google) can insert heart rate and weight samples for a time window. Then
finish a workout in that window and check the summary; the session appears in Health Connect →
Data and access → Activity.
