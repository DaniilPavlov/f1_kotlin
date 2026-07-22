package com.example.f1_kotlin.data.circuits

import com.squareup.moshi.JsonClass

/** Технические характеристики трассы (curated, не Jolpica). */
@JsonClass(generateAdapter = true)
data class CircuitStats(
    val lengthKm: Double,
    val laps: Int,
    val turns: Int,
    val topSpeedKmh: Double,
    val elevationM: Double,
) {
    val lengthLabel: String get() = "${trim(lengthKm)} KM"
    val lapsLabel: String get() = laps.toString()
    val turnsLabel: String get() = turns.toString()
    val topSpeedLabel: String get() = trim(topSpeedKmh)
    val elevationLabel: String get() = trim(elevationM)

    companion object {
        private fun trim(value: Double): String =
            if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
    }
}
