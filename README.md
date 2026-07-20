# F1 Kotlin

Native Android app with Formula 1 stats  
(standings, results, calendar, hall of fame, circuits).

Data — [Jolpica F1 API](https://github.com/jolpica/jolpica-f1) (Ergast-compatible).

Same idea, other stacks:

- [f1_pet_project](https://github.com/DaniilPavlov/f1_pet_project) — Flutter (Android / iOS)
- [f1_kmp](https://github.com/DaniilPavlov/f1_kmp) — Kotlin Multiplatform (Android / iOS), ported from this repo

## Stack

| Layer | Tech |
|------|------------|
| UI | Jetpack Compose, Material 3, Navigation Compose |
| DI | Hilt |
| Network | Retrofit + OkHttp + Moshi |
| Cache | Room (offline peek → refresh) |
| Time | java.time |
| Map | OSMDroid + OSMBonusPack |

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
│       ├── main/kotlin/com/example/f1_kotlin/
│       │   ├── data/        # API, Room, repository, models
│       │   ├── domain/      # ApiCallHandler, AsyncValue
│       │   ├── di/          # Hilt modules
│       │   ├── ui/          # screens, components, map, theme
│       │   ├── viewmodel/
│       │   └── util/
│       └── test/            # unit tests
└── gradle/
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
git tag v1.1.0
git push origin v1.1.0
```

For release APK signing (optional) — `ANDROID_KEYSTORE_*` secrets in GitHub Actions.

## Offline

The app reads the local Room cache first (peek), then refreshes from the network.  
If the network is unavailable but cache exists, the UI keeps the last known data.

## Features

- **Home** — current season driver and constructor standings  
- **Results** — latest race, search by season and race (pickers), detail card (race, sprint, qualifying, pit stops)  
- **Calendar** — season schedule with weekend sessions (practice, qualifying, sprint, sprint qualifying, race)  
- **Hall of fame** — final driver and constructor tables for a selected year (season picker from Jolpica)  
- **Circuits** — list and OSMDroid map with pins/clusters, circuit card, Wikipedia link, and race winners history  
- **Driver card** — full screen with passport data and career stats (races, wins, podiums, poles, teams) from Jolpica endpoints  
- **Constructor card** — nationality, Wikipedia link, career stats, and drivers list  
- **Localization** — Russian and English, toggle in the app bar without restarting the app  
- **Reminders** — local notifications 30 minutes before a session (up to 10 upcoming kept in the OS; window refreshes when the app opens)  
- **Schedule cache** — shared cache for the calendar and reminders  
- **Offline** — Room cache with instant peek and network refresh  
