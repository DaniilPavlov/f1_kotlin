package com.example.f1_kotlin.data.deeplink

import android.net.Uri

object F1PetDeepLinks {
    const val SCHEME = "f1pet"

    fun driver(driverId: String): Uri =
        Uri.Builder().scheme(SCHEME).authority("driver").appendPath(driverId).build()

    fun constructor(constructorId: String): Uri =
        Uri.Builder().scheme(SCHEME).authority("constructor").appendPath(constructorId).build()

    fun circuit(circuitId: String): Uri =
        Uri.Builder().scheme(SCHEME).authority("circuit").appendPath(circuitId).build()

    fun raceLive(): Uri =
        Uri.Builder().scheme(SCHEME).authority("race").appendPath("live").build()

    fun race(season: String, round: String): Uri =
        Uri.Builder().scheme(SCHEME).authority("race").appendPath(season).appendPath(round).build()
}

sealed class DeepLinkTarget {
    data class Driver(val driverId: String) : DeepLinkTarget()
    data class Constructor(val constructorId: String) : DeepLinkTarget()
    data class Circuit(val circuitId: String) : DeepLinkTarget()
    data object RaceLive : DeepLinkTarget()
    data class Race(val season: String, val round: String) : DeepLinkTarget()
}

fun Uri.toDeepLinkTarget(): DeepLinkTarget? {
    if (scheme != F1PetDeepLinks.SCHEME) return null
    return when (host) {
        "driver" -> pathSegments.firstOrNull()?.takeIf { it.isNotBlank() }?.let { DeepLinkTarget.Driver(it) }
        "constructor" -> pathSegments.firstOrNull()?.takeIf { it.isNotBlank() }
            ?.let { DeepLinkTarget.Constructor(it) }
        "circuit" -> pathSegments.firstOrNull()?.takeIf { it.isNotBlank() }?.let { DeepLinkTarget.Circuit(it) }
        "race" -> {
            val segments = pathSegments
            when {
                segments.firstOrNull() == "live" -> DeepLinkTarget.RaceLive
                segments.size >= 2 -> DeepLinkTarget.Race(segments[0], segments[1])
                else -> null
            }
        }
        else -> null
    }
}
