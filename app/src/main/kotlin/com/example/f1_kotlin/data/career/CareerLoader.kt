package com.example.f1_kotlin.data.career

import com.example.f1_kotlin.data.api.F1ApiService
import com.example.f1_kotlin.data.model.CareerStats
import com.example.f1_kotlin.data.model.ConstructorModel
import com.example.f1_kotlin.data.model.DriverModel
import com.example.f1_kotlin.data.model.MrDataTotalModel
import kotlinx.coroutines.delay

/**
 * Загрузка карьерной статистики через Jolpica (≤4 req/s → пауза 500 ms между запросами).
 */
object CareerLoader {
    private const val MIN_GAP_MS = 500L

    suspend fun loadDriverCareer(
        api: F1ApiService,
        driverId: String,
        current: List<ConstructorModel> = emptyList(),
    ): CareerStats<ConstructorModel> {
        val responses = getThrottled(
            api,
            listOf(
                "drivers/$driverId/results",
                "drivers/$driverId/results/1",
                "drivers/$driverId/results/2",
                "drivers/$driverId/results/3",
                "drivers/$driverId/qualifying/1",
                "drivers/$driverId/constructors",
            ),
        )
        val wins = totalOf(responses[1])
        val second = totalOf(responses[2])
        val third = totalOf(responses[3])
        return CareerStats(
            races = totalOf(responses[0]),
            wins = wins,
            podiums = wins + second + third,
            poles = totalOf(responses[4]),
            current = current,
            related = responses[5].constructorTable?.constructors.orEmpty(),
        )
    }

    suspend fun loadConstructorCareer(
        api: F1ApiService,
        constructorId: String,
        current: List<DriverModel> = emptyList(),
    ): CareerStats<DriverModel> {
        val responses = getThrottled(
            api,
            listOf(
                "constructors/$constructorId/results",
                "constructors/$constructorId/results/1",
                "constructors/$constructorId/results/2",
                "constructors/$constructorId/results/3",
                "constructors/$constructorId/qualifying/1",
                "constructors/$constructorId/drivers",
            ),
        )
        val wins = totalOf(responses[1])
        val second = totalOf(responses[2])
        val third = totalOf(responses[3])
        return CareerStats(
            races = totalOf(responses[0]),
            wins = wins,
            podiums = wins + second + third,
            poles = totalOf(responses[4]),
            current = current,
            related = responses[5].driverTable?.drivers.orEmpty(),
        )
    }

    private suspend fun getThrottled(api: F1ApiService, paths: List<String>): List<MrDataTotalModel> {
        val responses = mutableListOf<MrDataTotalModel>()
        var lastStart = 0L
        for (path in paths) {
            if (lastStart > 0) {
                val elapsed = System.currentTimeMillis() - lastStart
                if (elapsed < MIN_GAP_MS) delay(MIN_GAP_MS - elapsed)
            }
            lastStart = System.currentTimeMillis()
            responses.add(api.getMrDataTotal(path).mrData)
        }
        return responses
    }

    private fun totalOf(data: MrDataTotalModel): Int =
        data.total?.toIntOrNull() ?: 0
}
