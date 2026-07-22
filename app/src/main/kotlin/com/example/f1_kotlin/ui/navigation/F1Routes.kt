package com.example.f1_kotlin.ui.navigation

import kotlinx.serialization.Serializable

@Serializable data object Home
@Serializable data object Results
@Serializable data object Schedule
@Serializable data object News
@Serializable data object Circuits
@Serializable data object RaceSearch
@Serializable data object HallOfFame
@Serializable data object H2hDrivers
@Serializable data object H2hConstructors
@Serializable data object FinishStatus
@Serializable data class RaceInfo(val season: String, val round: String)
@Serializable data class CircuitDetail(val circuitId: String)
@Serializable data class DriverDetail(val driverId: String)
@Serializable data class ConstructorDetail(val constructorId: String)
