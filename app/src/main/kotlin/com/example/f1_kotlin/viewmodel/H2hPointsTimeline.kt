package com.example.f1_kotlin.viewmodel

data class H2hRoundScore(
    val season: String,
    val round: String,
    val raceName: String,
    val points: Double,
) {
    val key: String get() = "$season-$round"
    val roundNumber: Int get() = round.toIntOrNull() ?: 0
}

data class H2hTimelinePoint(
    val season: String,
    val round: String,
    val label: String,
    val raceName: String,
    val cumulativeA: Double,
    val cumulativeB: Double,
    val roundPointsA: Double,
    val roundPointsB: Double,
)

data class H2hPointsTimeline(
    val points: List<H2hTimelinePoint>,
) {
    val isEmpty: Boolean get() = points.isEmpty()
    val maxCumulative: Double
        get() = points.maxOfOrNull { maxOf(it.cumulativeA, it.cumulativeB) } ?: 0.0

    companion object {
        fun fromScores(
            scoresA: List<H2hRoundScore>,
            scoresB: List<H2hRoundScore>,
            seasonScope: String? = null,
        ): H2hPointsTimeline {
            val mapA = scoresA.associateBy { it.key }
            val mapB = scoresB.associateBy { it.key }
            val keys = (mapA.keys + mapB.keys).sortedWith { a, b ->
                val sa = mapA[a] ?: mapB[a]!!
                val sb = mapA[b] ?: mapB[b]!!
                val seasonCmp = sa.season.compareTo(sb.season)
                if (seasonCmp != 0) seasonCmp else sa.roundNumber.compareTo(sb.roundNumber)
            }
            val singleSeason = !seasonScope.isNullOrBlank()
            var cumA = 0.0
            var cumB = 0.0
            val points = keys.map { key ->
                val a = mapA[key]
                val b = mapB[key]
                val sample = a ?: b!!
                val roundA = a?.points ?: 0.0
                val roundB = b?.points ?: 0.0
                cumA += roundA
                cumB += roundB
                H2hTimelinePoint(
                    season = sample.season,
                    round = sample.round,
                    label = if (singleSeason) sample.round else "${sample.season} · R${sample.round}",
                    raceName = a?.raceName ?: b?.raceName.orEmpty(),
                    cumulativeA = cumA,
                    cumulativeB = cumB,
                    roundPointsA = roundA,
                    roundPointsB = roundB,
                )
            }
            return H2hPointsTimeline(points)
        }
    }
}
