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
| UI | Jetpack Compose, Material 3, type-safe Navigation Compose (`kotlinx.serialization` routes) |
| Presentation | One ViewModel file per screen; `*UiState` + `AsyncValue` |
| Domain | Plain Kotlin models; `AppError` / `toAppError()`; `AppDataRefresh` |
| DI | Hilt; `IF1Repository` / `IEspnRepository` bound with `@Binds` |
| Network | Retrofit + OkHttp + Moshi DTOs; mappers DTO → domain |
| Images | Coil |
| Cache | Room (offline peek → refresh); ESPN in-memory TTL; soft `AppDataRefresh.clearAll` |
| Time | java.time |
| Map | OSMDroid + OSMBonusPack (Carto tiles) |
| Backend | Firebase (Analytics, Crashlytics, Remote Config), AppMetrica |

### Differences from f1_pet_project (Flutter)

| Flutter | Kotlin |
|---------|--------|
| MobX | ViewModel + StateFlow |
| Auto Route | Type-safe Navigation Compose |
| Dio | Retrofit + Moshi |
| Yandex MapKit | OSMDroid |
| Local cache | Room |
| Flutter widgets | Jetpack Compose |

## Architecture

- **Navigation** — `@Serializable` route objects/classes in `F1Routes.kt`; destinations use `toRoute<T>()` / `SavedStateHandle.toRoute`.
- **UiState** — each screen exposes a single `StateFlow<*UiState>`; loading/data/error via `AsyncValue`.
- **Errors** — repositories return `Result`; failures map to `AppError` (`toAppError`) for UI.
- **Repositories** — `IF1Repository` + `IEspnRepository` interfaces; concrete impls bound in Hilt (`RepositoryModule`).
- **ViewModels** — one file per screen under `viewmodel/` (no giant shared files).
- **Refresh** — `AppDataRefresh.clearAll()` soft-invalidates ESPN TTL + in-memory F1 caches (Room kept for offline); `refreshAll()` on main screens calls it before reload.
- **Firebase** — `google-services` plugin + `app/google-services.json` (gitignored); bootstrap in `F1Application`; Analytics/Crashlytics off in debug. Project: `f1-kotlin`.
- **AppMetrica** — bootstrap from `local.properties` (`appmetrica.apiKey`); empty key → skip.
- **Remote Config** — `min_app_version` (force update), `local_notifications_enabled` (reminder kill-switch).

## Structure

```
f1_kotlin/
├── app/
│   └── src/
│       ├── main/
│       │   ├── assets/      # circuit layouts + circuit_stats.json
│       │   ├── java/        # PendingIntent / BootCompleted helpers
│       │   └── kotlin/com/example/f1_kotlin/
│       │       ├── data/    # Jolpica + ESPN API, Room, Firebase, AppMetrica
│       │       ├── domain/
│       │       ├── di/
│       │       ├── ui/
│       │       ├── viewmodel/
│       │       └── util/
│       ├── test/            # JVM unit tests (MockK + coroutines-test)
│       └── androidTest/     # Compose UI tests (not in CI)
├── tool/ci/                 # google-services stub for CI / local without Firebase
└── .github/workflows/
```

## Requirements

- JDK **17+** (CI uses Temurin **21**; app `jvmTarget` 11)
- Android Studio / Android SDK
- minSdk 30, targetSdk 36

## Secrets

Not in git.

### Firebase (`f1-kotlin`)

1. [Firebase Console](https://console.firebase.google.com/project/f1-kotlin/overview) → Project settings → Your apps  
2. Add Android app with package **`com.example.f1_kotlin`** (if not added yet)  
3. Download `google-services.json` → put at **`app/google-services.json`** (gitignored)  
4. Enable **Analytics**, **Crashlytics**, **Remote Config** in the console  

Without a real file, Gradle copies `tool/ci/google-services.stub.json` so CI/local still builds.

Remote Config keys: `local_notifications_enabled` (bool), `min_app_version` (string semver).

### AppMetrica

Optional in `local.properties`:

```properties
appmetrica.apiKey=...
```

## Run

```bash
./gradlew :app:assembleDebug
# install on device/emulator:
./gradlew :app:installDebug
```

In Android Studio: Run → **app** configuration.

## Tests

**Unit (JVM)** — MockK + `kotlinx-coroutines-test`; run on CI:

```bash
./gradlew :app:testDebugUnitTest
```

Covered areas include:

- ViewModels: Home, Results (incl. ESPN hide-on-error), Schedule, Race search, H2H drivers, Finish status, Race info, Circuit detail, News
- Domain / util: `ApiCallHandler`, `AppVersion`
- Data: career loader, Jolpica mappers

**Compose UI (`androidTest`)** — content composables (`HomeScreenContent`, `ResultsScreenContent`) without Hilt. Not run in CI (needs device/emulator):

```bash
./gradlew :app:connectedDebugAndroidTest
```

## CI / CD

[![CI](https://github.com/DaniilPavlov/f1_kotlin/actions/workflows/ci.yml/badge.svg)](https://github.com/DaniilPavlov/f1_kotlin/actions/workflows/ci.yml)

| Workflow | When | What it does |
|----------|-------|------------|
| `ci.yml` | push / PR to `master` | Firebase stub, detekt, debug APK, unit tests |
| `release.yml` | tag `v*` or manual | Android APK (+ GitHub Release) |

```bash
./gradlew :app:detekt :app:testDebugUnitTest
```

Release:

```bash
# version in app/build.gradle.kts must match the tag
git tag v1.5.0
git push origin v1.5.0
```

Release secrets (GitHub → Settings → Secrets):

| Secret | Purpose |
|--------|---------|
| `GOOGLE_SERVICES_JSON` | Full `app/google-services.json` body (else CI stub) |
| `APPMETRICA_API_KEY` | AppMetrica API key (else skipped) |
| `ANDROID_KEYSTORE_BASE64` | Base64 of `upload-keystore.jks` |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | Key alias (e.g. `upload`) |
| `ANDROID_KEY_PASSWORD` | Key password |

Without `ANDROID_KEYSTORE_*`, the release APK is built with **debug** signing.

```bash
keytool -genkey -v -keystore upload-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
base64 -i upload-keystore.jks | pbcopy   # → ANDROID_KEYSTORE_BASE64
```

## Offline

The app reads the local Room cache first (peek), then refreshes from the network.  
If the network is unavailable but cache exists, the UI keeps the last known data.  
ESPN news/scoreboard use a short in-memory TTL (no Room); scoreboard network failures hide the block instead of breaking Results.  
Forced reload (`refreshAll`) soft-invalidates ESPN + in-memory caches via `AppDataRefresh` (Room kept as offline fallback).

## Features

- **Home** — current season driver and constructor standings  
- **Results** — weekend scoreboard (ESPN, live poll), latest race, race search, hall of fame, H2H (drivers / constructors), finish statuses  
- **Calendar** — monthly calendar with session times; on empty days shows next GP card (layout + countdown); local reminders 30 min before  
- **News** — F1 headlines from ESPN  
- **Circuits** — list and map with pins/clusters, track layouts, length/laps/turns/speed/elevation, Wikipedia, winners history  
- **Driver / Constructor cards** — ESPN photos/news, career stats with tappable wins / podiums / poles lists  
- **Localization** — Russian and English, toggle in the app bar without restarting the app  
- **Reminders** — local notifications 30 minutes before a session (up to 10 upcoming; Remote Config can disable)  
- **Force update** — blocking screen when below Remote Config `min_app_version`  
- **Offline** — Room cache with instant peek and network refresh  
- **Share** — career stats and race results as PNG via the system share sheet  
- **Shimmer skeletons** — loading placeholders for main screens (like Flutter)  
- **Country flags** — nationality / country as emoji in tables, career cards, circuits, scoreboard  
