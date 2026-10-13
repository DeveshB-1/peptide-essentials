# Play Store submission checklist

Everything in this folder is ready to paste/upload into Play Console.

| Play Console field | Use |
| --- | --- |
| App bundle | `DoseLog.aab` from the latest GitHub release |
| App name | `listing/title.txt` |
| Short description | `listing/short_description.txt` |
| Full description | `listing/full_description.txt` |
| Release notes | `listing/release_notes.txt` |
| App icon (512×512) | `icon-512.png` |
| Feature graphic (1024×500) | `feature-graphic.png` |
| Phone screenshots | Take at least 2 on your own phone (Today, History, Medications, site picker) |
| Privacy policy URL | https://deveshb-1.github.io/dose-log/privacy.html |
| Category | **Health & Fitness** (recommended; *Medical* also fits, but invites stricter review) |
| Contains ads | No |
| In-app purchases | No |

## App content answers

- **Data safety:** No data collected, no data shared. The app has no INTERNET permission; everything is stored
  locally. "Is data encrypted in transit?" → not applicable (nothing is transmitted). "Can users request data
  deletion?" → users delete data themselves in the app or by uninstalling.
- **Health apps declaration:** Declare the app as a health app with the feature *Medication and treatment
  management* (personal medication log and reminders). State that it is a **personal tracker, not a medical
  device**: it does not diagnose, treat or recommend doses, and it shows a "not medical advice" notice on first
  launch and in About. It is not regulated as a medical device and makes no clinical claims.
- **Content rating (IARC questionnaire):** category *Reference, News, or Educational* / utility. Violence: no.
  Sexual content: no. Language: no. Controlled substances: no references or promotion (users type their own
  medication names). Gambling: no. User-generated content shared with others: no. Users can interact: no.
  Shares location: no. Digital purchases: no. Expected rating: Everyone / PEGI 3.
- **Target audience:** 18+ (avoids Families policy requirements; the app is for adults managing their own
  prescriptions).
- **Ads:** No. **Government / financial / news app:** No.
- **Permissions:** `POST_NOTIFICATIONS` (dose reminders) and `RECEIVE_BOOT_COMPLETED` (restore reminders after
  restart). No exact-alarm permission, so no exact-alarm declaration is needed.

## Signing

Use Play App Signing. Either upload `~/.android/dose-log-release.jks` (alias `doselog`) via Google's PEPK tool
so Play builds and GitHub-release APKs share a signature, or let Google generate the app signing key and keep
this keystore as the **upload key** (sideloaded installs then need uninstalling once before installing from
Play). Keep the keystore and its password backed up somewhere safe — they are not in this repository.

## New personal developer accounts

Google requires a **closed test with at least 12 testers opted in for 14 continuous days** before you can apply
for production access.
