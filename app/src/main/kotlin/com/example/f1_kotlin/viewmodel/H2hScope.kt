package com.example.f1_kotlin.viewmodel

import com.example.f1_kotlin.data.model.H2hStats
import com.example.f1_kotlin.domain.AppError
import com.example.f1_kotlin.domain.toAppError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

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

fun LoadJobHolder.launchH2hCompare(
    scope: CoroutineScope,
    canCompare: Boolean,
    fetchA: suspend () -> Result<H2hStats>,
    fetchB: suspend () -> Result<H2hStats>,
    onLoading: () -> Unit,
    onError: (AppError) -> Unit,
    onSuccess: (H2hStats, H2hStats) -> Unit,
) {
    if (!canCompare) return
    launch(scope) {
        onLoading()
        coroutineScope {
            val statsADeferred = async { fetchA() }
            val statsBDeferred = async { fetchB() }
            val statsA = statsADeferred.await()
            val statsB = statsBDeferred.await()
            val leftErr = statsA.exceptionOrNull()?.toAppError()
            if (leftErr != null) {
                onError(leftErr)
                return@coroutineScope
            }
            val rightErr = statsB.exceptionOrNull()?.toAppError()
            if (rightErr != null) {
                onError(rightErr)
                return@coroutineScope
            }
            onSuccess(statsA.getOrThrow(), statsB.getOrThrow())
        }
    }
}
