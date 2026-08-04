package com.example.f1_kotlin.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Цвета команд (constructors)
 * Ключ — Jolpica `constructorId`; неизвестные id получают стабильный fallback по хэшу.
 */
object ConstructorColors {
    private val knownByConstructorId = mapOf(
        "ferrari" to Color(0xFFA51010),
        "mercedes" to Color(0xFF006F62),
        "red_bull" to Color(0xFF1E2E5A),
        "rb" to Color(0xFF6B9AC4),
        "racing_bulls" to Color(0xFF6B9AC4),
        "mclaren" to Color(0xFFFF8700),
        "audi" to Color(0xFFE85A5A),
        "sauber" to Color(0xFFE85A5A),
        "kick_sauber" to Color(0xFFE85A5A),
        "cadillac" to Color(0xFF8A8D8F),
        "haas" to Color(0xFF2B2B2B),
        "aston_martin" to Color(0xFF229971),
        "alpine" to Color(0xFFFF69B4),
        "williams" to Color(0xFF00A0DE),
    )

    private val fallbackSwatches = listOf(
        Color(0xFF3671C6),
        Color(0xFF27F4D2),
        Color(0xFF52E252),
        Color(0xFFFE5888),
        Color(0xFF64C4FF),
        Color(0xFFA19D94),
        Color(0xFF9B59B6),
        Color(0xFFF1C40F),
    )

    fun forConstructorId(constructorId: String): Color {
        val key = constructorId.trim().lowercase()
        knownByConstructorId[key]?.let { return it }
        var hash = 0
        for (c in key) {
            hash = (hash * 31 + c.code) and 0x7fffffff
        }
        return fallbackSwatches[hash % fallbackSwatches.size]
    }
}
