# F1 Kotlin

Android-приложение со статистикой Formula 1  
(турнирные таблицы, результаты, календарь, зал славы, трассы).

Порт Flutter-проекта **[f1_pet_project](https://github.com/DaniilPavlov/f1_pet_project)** на нативный Kotlin + Jetpack Compose.  
Данные — [Jolpica F1 API](https://github.com/jolpica/jolpica-f1) (совместим с Ergast).

## Стек

| Слой | Технологии |
|------|------------|
| UI | Jetpack Compose, Material 3, Navigation Compose |
| DI | Hilt |
| Сеть | Retrofit + OkHttp + Moshi |
| Кэш | Room (offline peek → refresh) |
| Время | java.time |
| Карта | OSMDroid + OSMBonusPack |

### Отличия от f1_pet_project (Flutter)

- MobX → ViewModel + StateFlow  
- Auto Route → Navigation Compose  
- Dio → Retrofit + Moshi  
- Yandex MapKit → OSMDroid  
- Локальный кэш → Room  
- Flutter widgets → Jetpack Compose  

## Структура

```
f1_kotlin/
├── app/
│   └── src/
│       ├── main/kotlin/com/example/f1_kotlin/
│       │   ├── data/        # API, Room, repository, models
│       │   ├── domain/      # ApiCallHandler, AsyncValue
│       │   ├── di/          # Hilt-модули
│       │   ├── ui/          # экраны, компоненты, карта, тема
│       │   ├── viewmodel/
│       │   └── util/
│       └── test/            # unit-тесты
└── gradle/
```

## Требования

- JDK 11+
- Android Studio / Android SDK
- minSdk 30, targetSdk 36

## Запуск

```bash
./gradlew :app:assembleDebug
# установка на устройство/эмулятор:
./gradlew :app:installDebug
```

В Android Studio: Run → конфигурация **app**.

## Тесты

```bash
./gradlew :app:testDebugUnitTest
```

## Offline

Приложение сначала читает локальный Room-кэш (peek), затем обновляет данные с сети.  
Если сеть недоступна, а кэш есть — UI остаётся с последними данными.
