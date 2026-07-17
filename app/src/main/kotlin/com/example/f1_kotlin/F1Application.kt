package com.example.f1_kotlin

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Глобальный класс приложения, указанный в AndroidManifest.
 *
 * [@HiltAndroidApp] запускает кодогенерацию Dagger/Hilt: создаётся граф зависимостей
 * (SingletonComponent), куда попадают [com.example.f1_kotlin.di.NetworkModule],
 * [com.example.f1_kotlin.di.DatabaseModule] и все классы с [@Inject].
 *
 * Без этого класса Hilt не сможет внедрять [com.example.f1_kotlin.data.repository.F1Repository]
 * во ViewModel'и.
 */
@HiltAndroidApp
class F1Application : Application()
