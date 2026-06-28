package com.example.f1kotlin.data.model

/**
 * DTO для сохранения таблицы пилотов в Room вместе с номером сезона и раунда.
 * Нужен экрану «Главная» для строк «Сезон / Раунд» при offline-режиме.
 */
data class DriverStandingsCache(
    val drivers: List<DriverStandingsModel>,
    val season: String,
    val round: String,
)
