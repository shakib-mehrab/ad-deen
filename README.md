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
  <img width="470" height="1032" alt="home-day" src="https://github.com/user-attachments/assets/ddc80738-cc23-412d-bb5c-b6b0266294e8" />
    <img width="470" height="1032" alt="home-night" src="https://github.com/user-attachments/assets/e9977978-341d-4d3f-aea8-219420f20294" />
  <img width="470" height="1032" alt="wakt-day" src="https://github.com/user-attachments/assets/2fda62e0-a534-498f-959a-390b1e6ccdd6" />
 <img width="470" height="1032" alt="tracker-day" src="https://github.com/user-attachments/assets/5abce572-9475-44d3-93cf-27b02e9470ef" />
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
- A **start alarm** and a **15-minute end warning** for every waqt, each with its own on/off switch, notification channel and sound.
- **Istigfar and Durud reminders** run on their own schedules. When they coincide, Durud follows Istigfar 30 seconds later. Rotating Arabic, English and Bangla phrases. **Quiet hours** apply to these reminders only, never to prayer alarms.
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
- Monthly calendar, 7-day score chart with Quran markers, current and best **streaks**, and **milestones** 


### Azkhar, tasbih and more
- Morning, evening, sleep and waking azkhar with Arabic, transliteration and translations, plus tap-to-count with haptics.
- Custom zikr counters (tasbih) and the **99 Names of Allah**.
- Adjustable Arabic font size.
- Quran browser with quran.com, quran.gov.bd and QuraanShareef, and you can **add your own websites**. We encourages reading from a physical Mushaf.

### Account, sync and backup
- Optional email sign-in with **real-time Firestore sync** across devices. Works offline and syncs when back online; the newest edit wins.
- **Backup files:** share to Google Drive, save to storage, restore any time.

### Updates
- **In-app updates:** new versions published on GitHub Releases are offered inside the app (More → Check for updates, or automatically). Each download is verified (checksum and signing key) before Android's installer opens.


---

## Screenshots

| Onboarding | Home | Home (night) | Wakt and alarms |
| :---: | :---: | :---: | :---: |
<img width="470" height="1032" alt="onboarding-day" src="https://github.com/user-attachments/assets/e90f0a17-ddbb-4688-8052-1e49030bb064" /> | <img width="470" height="1032" alt="home-day" src="https://github.com/user-attachments/assets/d628d3e8-9b93-4b43-84e1-430acb65e1b1" /> | <img src="docs/screenshots/home-night.png" width="180" /> | <img width="470" height="1032" alt="wakt-day" src="https://github.com/user-attachments/assets/c511e905-f3fc-4bd8-aeb6-a44d0e378d65" />

| Wakt (night) | Amal tracker | Azkhar | More |
| :---: | :---: | :---: | :---: |
|<img width="470" height="1032" alt="wakt-night" src="https://github.com/user-attachments/assets/712b4da8-591a-4d3d-b2df-443955c50059" /> | <img width="470" height="1032" alt="tracker-day" src="https://github.com/user-attachments/assets/b8bef7a2-08fc-4f24-bbe4-c507c124a956" /> | <img width="470" height="1032" alt="azkhar-day" src="https://github.com/user-attachments/assets/f99f00fc-27e8-4db8-9dbb-0977007a9e3e" /> | <img width="470" height="1032" alt="more-day" src="https://github.com/user-attachments/assets/24d6f7b0-a4c2-4f65-84bb-c56ae9c93071" /> |

| Settings | Backup and sync | Account | Sounds and reading |
| :---: | :---: | :---: | :---: |
| <img width="470" height="1032" alt="more-settings-day" src="https://github.com/user-attachments/assets/6a05147e-0ccf-47f1-8468-d59dec1237e3" /> | <img width="470" height="1032" alt="more-backup-day" src="https://github.com/user-attachments/assets/fd9f0e3f-fdc9-417d-ad4b-b54bc9f37f39" /> | <img width="470" height="1032" alt="more-auth-day" src="https://github.com/user-attachments/assets/2cf1457e-e034-4d48-a605-a60ede5c0f68" /> |<img width="470" height="1032" alt="more-custom-sounds-day" src="https://github.com/user-attachments/assets/cd0a68d7-c5d4-4341-9d1f-f5ce2d7e10af" /> |

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



## Privacy

Prayer times are calculated entirely on the device. Location is used only to calculate prayer times and is
stored in your settings. If you sign in, your settings and amal records sync to your own private Firestore
space, and **More → Delete account** removes them. The app has no ads and no analytics.

## Credits and licences

- Fonts: [Poppins](https://github.com/itfoundry/Poppins) and Hind Siliguri by Indian Type Foundry, both under the SIL Open Font License 1.1 (licence texts in `app/src/main/assets/licenses/`).
- Icons: Material Symbols (Apache 2.0).
- Hadith quoted in the app: Sahih al-Bukhari 6464.
