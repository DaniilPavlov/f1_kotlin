package com.example.f1_kotlin.data.circuits

/**
 * Локальные схемы трасс: `assets/circuits/{circuitId}.png`.
 */
object CircuitLayoutAssets {
    private val knownIds = setOf(
        "albert_park",
        "americas",
        "bahrain",
        "baku",
        "catalunya",
        "hungaroring",
        "imola",
        "interlagos",
        "jeddah",
        "losail",
        "marina_bay",
        "miami",
        "monaco",
        "monza",
        "red_bull_ring",
        "rodriguez",
        "shanghai",
        "silverstone",
        "spa",
        "suzuka",
        "vegas",
        "villeneuve",
        "yas_marina",
        "zandvoort",
    )

    /** Путь в AssetManager к PNG-схеме или `null`, если ассета нет. */
    fun assetPath(circuitId: String): String? {
        val id = circuitId.trim().lowercase()
        if (id !in knownIds) return null
        return "circuits/$id.png"
    }

    fun hasLayout(circuitId: String): Boolean = assetPath(circuitId) != null
}
