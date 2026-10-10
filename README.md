<div align="center">

<img src="assets/logo/ad-deen-logo-1024.png" width="112" alt="Ad-Deen logo" />

# Ad-Deen

**An authentic, calm Islamic companion for Android: accurate prayer times, alarms for every waqt,
a daily amal tracker, azkhar and tasbih, in English and বাংলা.**

![Android](https://img.shields.io/badge/Android-7.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-Auth%20%2B%20Firestore-FFCA28?logo=firebase&logoColor=black)

</div>

<p align="center">
  <img src="docs/screenshots/home-day.png" width="200" alt="Home (day)" />
  <img src="docs/screenshots/home-night.png" width="200" alt="Home (night)" />
  <img src="docs/screenshots/wakt-day.png" width="200" alt="Wakt and alarms" />
  <img src="docs/screenshots/tracker-day.png" width="200" alt="Amal tracker" />
</p>

---

## Features

### Prayer times
- High-precision astronomical calculation (solar declination, equation of time, refraction).
- Methods: **Islamic Foundation Bangladesh**, Karachi (Hanafi), Muslim World League, Egyptian, Umm al-Qura. Asr uses the Hanafi rule.
- **Seven windows:** Tahajjud, Fajr, Duha/Ishraq, Dhuhr, Asr, Maghrib, Isha, plus sunrise, sunset and Islamic midnight.
- **Forbidden (makruh) times:** sunrise, zawal and sunset, calculated with the same engine and location as everything else.
- Location: all **64 districts of Bangladesh**, or GPS.
- Hijri date with a ±2-day moon-sighting adjustment.

### Alarms and reminders
- A **start alarm** and a **15-minute end warning** for every waqt, each with its own on/off switch, notification channel and sound. Duha never shares Dhuhr's sound.
- Turning an alarm off cancels every scheduled copy of it, and it stays off after a reboot, a time-zone change or an app update.
- Prayer alarms use Android's alarm-clock mode, so they ring on time even in deep sleep.
- **Istigfar and Durud reminders** run on their own schedules (every 15 minutes to 3 hours, aligned to the clock). When they coincide, Durud follows Istigfar 30 seconds later. Rotating Arabic, English and Bangla phrases. **Quiet hours** apply to these reminders only, never to prayer alarms.
- Morning and evening azkhar reminders.
- **Custom sounds:** pick any audio file for any waqt or reminder, with preview and reset.

### Daily amal tracker (100 points)
| Item | Points |
| :--- | :--- |
| 5 fard prayers: Jamat 12 / Alone 7 / Qaza 0 / Missed 0 | 60 |
| Witr 10 · Tahajjud 10 · Duha 5 | 25 |
| Quran reading | 5 |
| Azkhar: Wake, Morning, Evening, Sleep, Zikr (2 each) | 10 |

- **Perfect Day** at 100 points. Tap a selected status again to clear it, and every change shows a 5-second **Undo**.
- Monthly calendar, 7-day score chart with Quran markers, current and best **streaks**, and **milestones** (first Perfect Day; 3-, 7- and 30-day streaks; 7 days all in Jamat; Quran on 7 days).
- Today and past days are editable; future days are locked. Days recorded before Quran tracking keep their original results.

### Azkhar, tasbih and more
- Morning, evening, sleep and waking azkhar with Arabic, transliteration and translations, plus tap-to-count with haptics.
- Custom zikr counters (tasbih) and the **99 Names of Allah**.
- Adjustable Arabic font size.
- Quran browser with quran.com, quran.gov.bd and QuraanShareef, and you can **add your own websites**. The app gently encourages reading from a physical Mushaf.

### Account, sync and backup
- Optional email sign-in with **real-time Firestore sync** across devices. Works offline and syncs when back online; the newest edit wins.
- Each user can only read and write their own data (`users/{uid}/…` security rules).
- **Backup files:** share to Google Drive, save to storage, restore any time.

### Updates
- **In-app updates:** new versions published on GitHub Releases are offered inside the app (More → Check for updates, or automatically). Each download is verified (checksum and signing key) before Android's installer opens.

### Design
- A soft, warm interface: paper and amber by day, **warm espresso night mode** that fades in automatically at Maghrib (or follows the phone's dark mode).
- Hero clock over a pale mosque skyline, with the sun or moon moving on its real path across the sky.
- Guided **onboarding**: language, location and alarm permissions, each with an explanation.
- Fully bilingual, with Poppins (English) and Hind Siliguri (Bangla) typography. Layouts are tested at 320dp width and 1.3x font size.

---

## Screenshots

| Onboarding | Home | Home (night) | Wakt and alarms |
| :---: | :---: | :---: | :---: |
| <img src="docs/screenshots/onboarding-day.png" width="180" /> | <img src="docs/screenshots/home-day.png" width="180" /> | <img src="docs/screenshots/home-night.png" width="180" /> | <img src="docs/screenshots/wakt-day.png" width="180" /> |

| Wakt (night) | Amal tracker | Azkhar | More |
| :---: | :---: | :---: | :---: |
| <img src="docs/screenshots/wakt-night.png" width="180" /> | <img src="docs/screenshots/tracker-day.png" width="180" /> | <img src="docs/screenshots/azkhar-day.png" width="180" /> | <img src="docs/screenshots/more-day.png" width="180" /> |

| Settings | Backup and sync | Account | Sounds and reading |
| :---: | :---: | :---: | :---: |
| <img src="docs/screenshots/more-settings-day.png" width="180" /> | <img src="docs/screenshots/more-backup-day.png" width="180" /> | <img src="docs/screenshots/more-auth-day.png" width="180" /> | <img src="docs/screenshots/more-custom-sounds-day.png" width="180" /> |

<sub>Screenshots are rendered from the app's real Compose code (`ScreenshotTest`) with sample data.</sub>

---

## Tech stack

| Layer | Technology |
| :--- | :--- |
| Language and UI | Kotlin 2.2, Jetpack Compose, Material 3, Navigation Compose |
| Architecture | MVVM: `MainViewModel`, a singleton `AppRepository`, Kotlin Flows |
| Local data | Room (days, zikr items, settings) and SharedPreferences (UI state) |
| Cloud | Firebase Authentication (email/password), Cloud Firestore with offline cache |
| Scheduling | `AlarmManager` (alarm-clock and exact alarms), `BroadcastReceiver`s, a media-playback foreground service |
| Location | Google Play Services fused location |
| Build | Gradle 9.3 (Kotlin DSL), AGP 9.1, version catalog, R8 shrinking for release |
| Tests | JUnit and Robolectric: scoring, alarm scheduling, layouts, logo rendering, screenshots |

## Project structure

```
app/src/main/java/com/example/
├── MainActivity.kt            app shell, onboarding gate, auto-hiding bottom bar
├── alarm/                     AlarmScheduler, NotificationChannels, SoundHelper
├── data/                      AppRepository (Room + Firestore), Mappers, BackupManager, UiPrefs
├── model/                     prayer and tracker models, scoring rules
├── prayer/                    PrayerCalculator (astronomy)
├── receiver/                  AlarmReceiver, BootReceiver
├── service/                   PrayerAudioService (alarm sound)
└── ui/                        screens, components (design system), theme (palette, type)
app/src/main/res/raw/          one sound per waqt plus Istigfar and Durud
assets/logo/                   master logo (SVG) and store icons
docs/                          RELEASE, UPDATES, SOUNDS, LOGO, FONTS, screenshots
scripts/                       icon generator, live-reload dev script
```

## Getting started

**Requirements:** JDK 17, Android SDK (platform 36.1, build-tools 36), and an Android phone or emulator (Android 7.0+).

```bash
.\gradlew.bat installDebug
```

- **Live reload while developing (VS Code):** run the task *Ad-Deen: Live Watch*. It rebuilds and reinstalls on every save (`scripts/dev-watch.ps1`).
- **Run the tests:** `.\gradlew.bat testDebugUnitTest`
- **Build a release:** `pwsh -File scripts/release.ps1 -Notes "..."` (see [docs/RELEASE.md](docs/RELEASE.md)).
- **Download the app:** [latest release](https://github.com/shakib-mehrab/ad-deen/releases/latest).

## Documentation

| Doc | What it covers |
| :--- | :--- |
| [docs/RELEASE.md](docs/RELEASE.md) | Signing, building the APK/AAB, sharing it, and the Play Store checklist |
| [docs/UPDATES.md](docs/UPDATES.md) | Shipping updates: the in-app GitHub updater, the release script, Remote Config, Play |
| [docs/SOUNDS.md](docs/SOUNDS.md) | Waqt → channel → sound mapping, and how to replace sounds |
| [docs/LOGO.md](docs/LOGO.md) | Master logo, regenerating every icon |
| [docs/FONTS.md](docs/FONTS.md) | Bundled fonts and their licences |

## Known limitations

- GPS locations outside Bangladesh use the nearest Bangladeshi district.
- Room uses destructive migration. Add a proper migration before changing the database schema, or local history is lost on update.
- Sign-in is email/password only (no Google Sign-In yet).

## Privacy

Prayer times are calculated entirely on the device. Location is used only to calculate prayer times and is
stored in your settings. If you sign in, your settings and amal records sync to your own private Firestore
space, and **More → Delete account** removes them. The app has no ads and no analytics.

## Credits and licences

- Fonts: [Poppins](https://github.com/itfoundry/Poppins) and Hind Siliguri by Indian Type Foundry, both under the SIL Open Font License 1.1 (licence texts in `app/src/main/assets/licenses/`).
- Icons: Material Symbols (Apache 2.0).
- Hadith quoted in the app: Sahih al-Bukhari 6464.
