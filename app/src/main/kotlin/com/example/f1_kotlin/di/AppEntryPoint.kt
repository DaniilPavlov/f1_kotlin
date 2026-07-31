package com.example.f1_kotlin.di

import com.example.f1_kotlin.data.analytics.AnalyticsGateway
import com.example.f1_kotlin.data.deeplink.DeepLinkBus
import com.example.f1_kotlin.domain.live.LiveWeekendController
import com.example.f1_kotlin.widgets.AppWidgetSyncService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppEntryPoint {
    fun deepLinkBus(): DeepLinkBus
    fun liveWeekendController(): LiveWeekendController
    fun analyticsGateway(): AnalyticsGateway
    fun appWidgetSyncService(): AppWidgetSyncService
}
