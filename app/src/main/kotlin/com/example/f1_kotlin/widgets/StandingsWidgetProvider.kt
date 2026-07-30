package com.example.f1_kotlin.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.widget.RemoteViews
import com.example.f1_kotlin.R

class StandingsWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val data = WidgetDataStore.prefs(context)
        appWidgetIds.forEach { widgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_standings).apply {
                setOnClickPendingIntent(
                    R.id.widget_standings_root,
                    WidgetUpdater.launchAppPendingIntent(context),
                )
                val hasData = data.getBoolean(WidgetDataStore.STANDINGS_HAS_DATA, false)
                if (!hasData) {
                    setViewVisibility(R.id.widget_standings_content, View.GONE)
                    setViewVisibility(R.id.widget_standings_empty, View.VISIBLE)
                    setTextViewText(R.id.widget_standings_empty, context.getString(R.string.widget_open_app))
                    return@apply
                }
                setViewVisibility(R.id.widget_standings_content, View.VISIBLE)
                setViewVisibility(R.id.widget_standings_empty, View.GONE)
                val season = data.getString(WidgetDataStore.STANDINGS_SEASON, "") ?: ""
                val round = data.getString(WidgetDataStore.STANDINGS_ROUND, "") ?: ""
                val subtitle = when {
                    season.isNotBlank() && round.isNotBlank() ->
                        context.getString(R.string.widget_standings_subtitle, season, round)
                    season.isNotBlank() -> season
                    else -> context.getString(R.string.widget_standings_title)
                }
                setTextViewText(R.id.widget_standings_subtitle, subtitle)
                bindRow(this, 1, data, R.id.widget_standings_d1_code, R.id.widget_standings_d1_pts)
                bindRow(this, 2, data, R.id.widget_standings_d2_code, R.id.widget_standings_d2_pts)
                bindRow(this, 3, data, R.id.widget_standings_d3_code, R.id.widget_standings_d3_pts)
            }
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    private fun bindRow(
        views: RemoteViews,
        index: Int,
        data: SharedPreferences,
        codeId: Int,
        ptsId: Int,
    ) {
        val code = data.getString(WidgetDataStore.driverCode(index), "") ?: ""
        val pts = data.getString(WidgetDataStore.driverPoints(index), "") ?: ""
        views.setTextViewText(codeId, if (code.isBlank()) "—" else code)
        views.setTextViewText(ptsId, pts)
    }
}
