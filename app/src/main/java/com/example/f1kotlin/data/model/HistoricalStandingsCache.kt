package com.example.f1kotlin.data.model

/** DTO для кэша «Зал славы» — таблицы пилотов и конструкторов за год. */
data class HistoricalStandingsCache(
    val drivers: List<DriverStandingsModel>,
    val constructors: List<ConstructorStandingsModel>,
)
