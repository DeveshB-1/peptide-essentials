# Dose Log

A neutral, offline Android app for keeping track of medication you've been prescribed — weekly pens or vials,
insulin, B12, hormone or fertility injections, daily tablets, anything. You name your own medications and
enter doses exactly as prescribed; the app reminds you and keeps the log.

> **Not medical advice.** Dose Log is a personal log, not a medical device. It doesn't recommend medications,
> doses, schedules or injection sites. Always follow your prescriber's or pharmacist's instructions.

## Features

- **Medications** – name, dose amount and unit (mg, mcg, mL, units, IU), schedule (daily, specific weekdays or
  every N days), time of day, start date, notes, colour tag, optional injection-site tracking, per-medication
  reminders, and pause/resume.
- **Today** – missed doses (last 7 days), doses due today and what's coming up. Log a dose as *Taken* (time,
  amount and site editable) or *Skip* it, with undo.
- **Injection site rotation** – abdomen, thigh, upper arm and buttock, left/right, picked on a front/back body
  diagram or with chips. The least recently used site for that medication is pre-selected.
- **History** – grouped by date, newest first, filterable by medication; tap an entry to edit or delete it.
- **Reminders** – a notification at each dose time with a *Mark taken* action that logs the dose without
  opening the app. Uses inexact `setAndAllowWhileIdle` alarms (no exact-alarm permission), rescheduled after
  reboot and on time or time-zone changes. Notification permission is requested on Android 13+ only when you
  first save a medication with reminders on.
- **Export & backup** – share full history as CSV, and save/restore a JSON backup through the system file
  picker (no storage permissions).
- Material 3 with dynamic colour on Android 12+ (teal/green fallback), light and dark themes, edge-to-edge,
  TalkBack labels.

## Privacy

No accounts, ads, analytics or network access — the app doesn't request the `INTERNET` permission. Data is
stored as JSON in app-private storage and excluded from cloud backup. See the
[privacy policy](https://deveshb-1.github.io/dose-log/privacy.html).

## Build

Requirements: JDK 17+ (21 recommended) and the Android SDK (compileSdk 36).

```sh
echo "sdk.dir=$HOME/Android/Sdk" > local.properties
./gradlew testReleaseUnitTest assembleRelease bundleRelease
```

Unit tests cover schedule maths (daily / weekdays / every N days across month ends, leap days and DST
transitions), the Today planner and site rotation.

### Release signing

Release builds are signed from `keystore.properties` in the project root (git-ignored):

```properties
storeFile=/path/to/release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

or, in CI, from the `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD` environment variables.
Without either, release builds fall back to the debug key.

GitHub Actions builds the APK and App Bundle on every push; pushing a `v*` tag publishes a release with
`DoseLog.apk` and `DoseLog.aab`. The workflow expects the secrets `KEYSTORE_B64` (base64 keystore),
`KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD`.

## Project layout

```
app/src/main/java/io/github/deveshb1/doselog/
  data/        models, JSON codec, repository (StateFlow + atomic file)
  schedule/    schedule maths, Today planner, site rotation
  reminders/   alarm scheduling, notification, receivers
  export/      CSV export and JSON backup
  ui/          Compose screens, components and theme
```
