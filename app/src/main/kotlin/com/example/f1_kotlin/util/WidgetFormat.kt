package com.example.f1_kotlin.util

/** Чистые хелперы для виджетов (без Context) — удобно юнит-тестировать. */
object WidgetFormat {
    fun shortRaceName(raceName: String): String {
        val name = raceName.trim()
        val suffix = " Grand Prix"
        return if (name.endsWith(suffix) && name.length > suffix.length) {
            name.substring(0, name.length - suffix.length)
        } else {
            name
        }
    }

    fun driverLabel(code: String?, familyName: String): String {
        val c = code?.trim().orEmpty()
        return if (c.isNotEmpty() && !c.equals("none", ignoreCase = true)) c else familyName.uppercase()
    }
}
