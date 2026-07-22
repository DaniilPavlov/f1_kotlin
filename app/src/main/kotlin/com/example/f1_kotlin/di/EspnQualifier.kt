package com.example.f1_kotlin.di

import javax.inject.Qualifier

/** Qualifier for the dedicated ESPN OkHttp / Retrofit stack. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class EspnClient
