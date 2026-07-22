# F1 Kotlin

Native Android app with Formula 1 stats  
(standings, results, calendar, news, circuits).

Data:
- [Jolpica F1 API](https://github.com/jolpica/jolpica-f1) (Ergast-compatible) — schedule, results, standings
- [ESPN](https://site.api.espn.com/) — news, weekend scoreboard, driver photos

Same idea, other stacks:

- [f1_pet_project](https://github.com/DaniilPavlov/f1_pet_project) — Flutter (Android / iOS / Web)
- [f1_kmp](https://github.com/DaniilPavlov/f1_kmp) — Kotlin Multiplatform (Android / iOS)

## Stack

| Layer | Tech |
|------|------------|
| UI | Jetpack Compose, Material 3, Navigation Compose |
| DI | Hilt |
| Network | Retrofit + OkHttp + Moshi (Jolpica + ESPN clients) |
| Images | Coil |
| Cache | Room (offline peek → refresh); ESPN in-memory TTL |
| Time | java.time |
| Map | OSMDroid + OSMBonusPack (Carto tiles) |

### Differences from f1_pet_project (Flutter)

| Flutter | Kotlin |
|---------|--------|
| MobX | ViewModel + StateFlow |
| Auto Route | Navigation Compose |
| Dio | Retrofit + Moshi |
| Yandex MapKit | OSMDroid |
| Local cache | Room |
| Flutter widgets | Jetpack Compose |

## Structure

```
f1_kotlin/
├── app/
│   └── src/
│       ├── main/
│       │   ├── assets/      # circuit layouts + circuit_stats.json
│       │   ├── java/        # PendingIntent / BootCompleted helpers
│       │   └── kotlin/com/example/f1_kotlin/
│       │       ├── data/    # Jolpica + ESPN API, Room, career, circuits
│       │       ├── domain/
│       │       ├── di/
│       │       ├── ui/
│       │       ├── viewmodel/
│       │       └── util/
│       └── test/
└── .github/workflows/
```

## Requirements

- JDK 11+
- Android Studio / Android SDK
- minSdk 30, targetSdk 36

## Run

```bash
./gradlew :app:assembleDebug
# install on device/emulator:
./gradlew :app:installDebug
```

In Android Studio: Run → **app** configuration.

## Tests

```bash
./gradlew :app:testDebugUnitTest
```

## CI / CD

[![CI](https://github.com/DaniilPavlov/f1_kotlin/actions/workflows/ci.yml/badge.svg)](https://github.com/DaniilPavlov/f1_kotlin/actions/workflows/ci.yml)

| Workflow | When | What it does |
|----------|-------|------------|
| `ci.yml` | push / PR to `master` | build debug APK, unit tests |
| `release.yml` | tag `v*` or manual | Android APK (+ GitHub Release) |

Release:

```bash
# version in app/build.gradle.kts must match the tag
git tag v1.2.0
git push origin v1.2.0
```

For release APK signing (optional) — `ANDROID_KEYSTORE_*` secrets in GitHub Actions.

## Offline

The app reads the local Room cache first (peek), then refreshes from the network.  
If the network is unavailable but cache exists, the UI keeps the last known data.  
ESPN news/scoreboard use a short in-memory TTL (no Room).

## Features

- **Home** — current season driver and constructor standings  
- **Results** — weekend scoreboard (ESPN, live poll), latest race, race search, hall of fame, H2H (drivers / constructors), finish statuses  
- **Calendar** — monthly calendar with session times; on empty days shows next GP card (layout + countdown); local reminders 30 min before  
- **News** — F1 headlines from ESPN  
- **Circuits** — list and map with pins/clusters, track layouts, length/laps/turns/speed/elevation, Wikipedia, winners history  
- **Driver / Constructor cards** — ESPN photos/news, career stats with tappable wins / podiums / poles lists  
- **Localization** — Russian and English, toggle in the app bar without restarting the app  
- **Reminders** — local notifications 30 minutes before a session (up to 10 upcoming kept in the OS)  
- **Offline** — Room cache with instant peek and network refresh  
- **Share** — career stats and race results as PNG via the system share sheet  
- **Shimmer skeletons** — loading placeholders for main screens (like Flutter)  
- **Country flags** — nationality / country as emoji in tables, career cards, circuits, scoreboard  
