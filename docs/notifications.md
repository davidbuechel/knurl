# Notifications

Every kind has its own Android channel (`core/notifications/Notifications.kt`, enum `Channel`) and
its own switch in Settings (`NotificationSettings`, stored in the account). The permission
(Android 13+) is asked the first time a workout runs, and again whenever a notification is
switched on in Settings.

| Channel id | What | Source |
|---|---|---|
| `rest` | "Rest is over" while Knurl is in the background | local alarm, `RestTimer` → `RestOverReceiver` |
| `workout` | ongoing: workout name, sets done, live clock (elapsed / rest countdown) | `WorkoutNotifier` |
| `reminders` | training day reminder on chosen days and time, skipped if already trained | local alarm, `ReminderReceiver` |
| `streak` | Saturday 17:00, when one more workout keeps the weekly streak | local alarm |
| `weekly` | Sunday 19:00, workouts, volume, records of the week | local alarm |
| `account` | account and security messages | push (FCM) |
| `news` | product news, off by default | push (FCM) |

Local alarms are inexact except the rest alarm, which is exact when the person allowed exact
alarms (Settings shows the link when needed). `BootReceiver` schedules reminders again after a
restart; `ReminderScheduler` reschedules whenever the notification settings change.

## Push messages (FCM)

Each phone writes its token to `users/{uid}/devices/{installId}` (`PushRegistration`), removed on
sign-out. Send data messages so the app picks the channel and honours the person's switches:

```json
{ "data": { "channel": "account", "title": "…", "body": "…" } }
```

Messages without `channel` land in `news`. A `notification` payload also works (title/body), but
then Android shows it itself while the app is in the background, ignoring the switches.

Sending from a server needs Cloud Functions (Blaze plan); for now test messages go out from the
Firebase console (Messaging → test message to the device token).

## Taps

Workout notifications open the running workout (`Notifications.TARGET_WORKOUT`, handled in
`KnurlNavHost`). Others open the app.
