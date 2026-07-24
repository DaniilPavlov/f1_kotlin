package com.example.f1_kotlin.di

import com.example.f1_kotlin.BuildConfig
import com.example.f1_kotlin.data.api.EspnApiService
import com.example.f1_kotlin.data.api.F1ApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt-модуль сетевого слоя (Dagger [@Module]).
 *
 * [@InstallIn(SingletonComponent::class)] — зависимости живут всё время работы приложения.
 * [@Provides] — Hilt знает, *как* создать OkHttp / Retrofit / Moshi, когда их запросит Repository.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    // HTTPS напрямую — HTTP даёт 301 redirect и лишний round-trip на каждый запрос
    private const val BASE_URL = "https://api.jolpi.ca/ergast/f1/"

    /** Moshi парсит JSON ответов API в data class'ы. */
    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    /**
     * GoF Creational Factory Method — создание продукта ([OkHttpClient]) делегировано
     * [provideOkHttpClient] / [provideEspnOkHttpClient], скрывая таймауты и interceptors.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        // GoF Creational Builder — пошаговая сборка (таймауты → interceptors → build).
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            // GoF Structural Decorator — заголовки «навешиваются» на OkHttp через interceptor,
            // не меняя API клиента.
            // GoF Behavioral Chain of Responsibility — либо модифицируем запрос, либо chain.proceed().
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("system", "android")
                    .addHeader("version", "1.0")
                    .addHeader("build-number", "1")
                    .addHeader("device-id", "deviceID")
                    .build()
                chain.proceed(request)
            }
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                },
            )
        }
        return builder.build()
    }

    /** Separate OkHttp for ESPN (no Ergast app headers; longer timeouts for news). */
    @Provides
    @Singleton
    @EspnClient
    fun provideEspnOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(40, TimeUnit.SECONDS)
            .writeTimeout(40, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                },
            )
        }
        return builder.build()
    }

    /** Retrofit превращает [F1ApiService] в реальные HTTP GET-запросы. */
    @Provides
    @Singleton
    fun provideF1ApiService(okHttpClient: OkHttpClient, moshi: Moshi): F1ApiService =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(F1ApiService::class.java)

    /** Dedicated Retrofit for unofficial ESPN Site API. */
    @Provides
    @Singleton
    fun provideEspnApiService(@EspnClient okHttpClient: OkHttpClient, moshi: Moshi): EspnApiService =
        Retrofit.Builder()
            .baseUrl(EspnApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(EspnApiService::class.java)
}
