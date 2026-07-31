package com.example.f1_kotlin.viewmodel

import com.example.f1_kotlin.data.model.H2hStats
import com.example.f1_kotlin.domain.model.Constructor
import com.example.f1_kotlin.domain.model.Driver

data class H2hDriverCompareResult(
    val driverA: Driver,
    val driverB: Driver,
    val statsA: H2hStats,
    val statsB: H2hStats,
    val season: String?,
    val timeline: H2hPointsTimeline = H2hPointsTimeline(emptyList()),
)

data class H2hConstructorCompareResult(
    val constructorA: Constructor,
    val constructorB: Constructor,
    val statsA: H2hStats,
    val statsB: H2hStats,
    val season: String?,
    val timeline: H2hPointsTimeline = H2hPointsTimeline(emptyList()),
)
