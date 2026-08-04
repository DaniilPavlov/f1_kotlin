package com.example.f1_kotlin.viewmodel

import com.example.f1_kotlin.data.model.H2hEntityCompareData
import com.example.f1_kotlin.domain.AppError
import com.example.f1_kotlin.domain.toAppError
import kotlinx.coroutines.CoroutineScope

data class H2hScopeState(
    val scopeMode: Int = 0,
    val useCurrentSeason: Boolean = true,
    val currentOnly: Boolean = true,
    val latestSeason: String = "",
    val pickedSeason: String = "",
) {
    val isSeasonScope: Boolean get() = scopeMode == 1
    val showYearPicker: Boolean get() = isSeasonScope && !useCurrentSeason

    val selectedSeason: String?
        get() {
            if (!isSeasonScope) return null
            return if (useCurrentSeason) {
                latestSeason.takeIf { it.isNotEmpty() }
            } else {
                pickedSeason.takeIf { it.length == 4 }
            }
        }

    /** both ids non-null, different, and season ok if season-scope */
    fun canCompare(idA: String?, idB: String?): Boolean =
        idA != null && idB != null && idA != idB &&
            (!isSeasonScope || selectedSeason != null)
}

/**
 * Общий скелет H2H-сравнения: A затем B последовательно (throttle Jolpica).
 *
 * GoF Behavioral Strategy — способ получения статистики выбирается на вызове:
 * [fetchA] / [fetchB] — взаимозаменяемые стратегии (драйверы / конструкторы / тесты).
 */
fun LoadJobHolder.launchH2hCompare(
    scope: CoroutineScope,
    canCompare: Boolean,
    fetchA: suspend () -> Result<H2hEntityCompareData>,
    fetchB: suspend () -> Result<H2hEntityCompareData>,
    onLoading: () -> Unit,
    onError: (AppError) -> Unit,
    onSuccess: suspend (H2hEntityCompareData, H2hEntityCompareData) -> Unit,
) {
    if (!canCompare) return
    launch(scope) {
        onLoading()
        val dataA = fetchA()
        dataA.exceptionOrNull()?.toAppError()?.let {
            onError(it)
            return@launch
        }
        val dataB = fetchB()
        dataB.exceptionOrNull()?.toAppError()?.let {
            onError(it)
            return@launch
        }
        onSuccess(dataA.getOrThrow(), dataB.getOrThrow())
    }
}
