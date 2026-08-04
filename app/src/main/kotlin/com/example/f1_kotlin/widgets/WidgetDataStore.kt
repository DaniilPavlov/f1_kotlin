package com.example.f1_kotlin.widgets

import android.content.Context

/** Ключи SharedPreferences для next-GP и top-3 standings виджетов. */
object WidgetDataStore {
    const val PREFS_NAME = "f1_app_widgets"

    const val NEXT_GP_RACE_NAME = "next_gp_race_name"
    const val NEXT_GP_CIRCUIT = "next_gp_circuit"
    const val NEXT_GP_TARGET_MS = "next_gp_target_ms"
    const val NEXT_GP_HAS_DATA = "next_gp_has_data"

    const val STANDINGS_SEASON = "standings_season"
    const val STANDINGS_ROUND = "standings_round"
    const val STANDINGS_HAS_DATA = "standings_has_data"

    fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun driverCode(index: Int) = "standings_d${index}_code"
    fun driverPoints(index: Int) = "standings_d${index}_points"

    fun save(context: Context, data: Map<String, Any?>) {
        val editor = prefs(context).edit()
        for ((key, value) in data) {
            when (value) {
                null -> editor.remove(key)
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Double -> editor.putString(key, value.toString())
                is String -> editor.putString(key, value)
                else -> editor.putString(key, value.toString())
            }
        }
        editor.apply()
    }
}
