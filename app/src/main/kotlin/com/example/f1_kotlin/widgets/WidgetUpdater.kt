package com.example.f1_kotlin.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.f1_kotlin.MainActivity

object WidgetUpdater {
    fun updateProviders(context: Context, vararg providers: Class<*>) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        for (clazz in providers) {
            val component = ComponentName(appContext, clazz)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isEmpty()) continue
            val intent = Intent(appContext, clazz).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            appContext.sendBroadcast(intent)
        }
    }

    fun launchAppPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        return PendingIntent.getActivity(context, 0, intent, flags)
    }
}
