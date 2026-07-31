package com.example.f1_kotlin.di

import com.example.f1_kotlin.data.analytics.AnalyticsGateway
import com.example.f1_kotlin.data.analytics.AppAnalyticsGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {
    @Binds
    @Singleton
    abstract fun bindAnalyticsGateway(impl: AppAnalyticsGateway): AnalyticsGateway
}
