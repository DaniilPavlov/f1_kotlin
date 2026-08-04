package com.example.f1_kotlin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1_kotlin.data.repository.IF1Repository
import com.example.f1_kotlin.domain.AsyncValue
import com.example.f1_kotlin.domain.model.Driver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class H2hDriversUiState(
    val scope: H2hScopeState = H2hScopeState(),
    val driverA: Driver? = null,
    val driverB: Driver? = null,
    val comparison: AsyncValue<H2hDriverCompareResult?> = AsyncValue.Value(null),
) {
    val scopeMode get() = scope.scopeMode
    val useCurrentSeason get() = scope.useCurrentSeason
    val currentOnly get() = scope.currentOnly
    val latestSeason get() = scope.latestSeason
    val pickedSeason get() = scope.pickedSeason
    val isSeasonScope get() = scope.isSeasonScope
    val showYearPicker get() = scope.showYearPicker
    val selectedSeason get() = scope.selectedSeason
    val canCompare get() = scope.canCompare(driverA?.driverId, driverB?.driverId)
}

/** H2H пилотов: выбор пары, scope сезона, timeline очков. */
@HiltViewModel
class H2hDriversViewModel @Inject constructor(
    private val repository: IF1Repository,
) : ViewModel() {
    private val loadJob = LoadJobHolder()

    private val _uiState = MutableStateFlow(H2hDriversUiState())
    val uiState: StateFlow<H2hDriversUiState> = _uiState.asStateFlow()

    val isSeasonScope: Boolean get() = _uiState.value.isSeasonScope
    val showYearPicker: Boolean get() = _uiState.value.showYearPicker
    val selectedSeason: String? get() = _uiState.value.selectedSeason
    val canCompare: Boolean get() = _uiState.value.canCompare

    init {
        viewModelScope.launch {
            repository.getSeasonYears().onSuccess { years ->
                if (years.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            scope = it.scope.copy(
                                latestSeason = years.first(),
                                pickedSeason = years.first(),
                            ),
                        )
                    }
                }
            }
        }
    }

    fun setScopeMode(mode: Int) {
        if (_uiState.value.scopeMode == mode) return
        _uiState.update {
            it.copy(scope = it.scope.copy(scopeMode = mode), comparison = AsyncValue.Value(null))
        }
    }

    fun setUseCurrentSeason(value: Boolean) {
        if (_uiState.value.useCurrentSeason == value) return
        _uiState.update {
            it.copy(
                scope = it.scope.copy(useCurrentSeason = value),
                comparison = AsyncValue.Value(null),
            )
        }
    }

    fun setCurrentOnly(value: Boolean) {
        if (_uiState.value.currentOnly == value) return
        _uiState.update {
            it.copy(
                scope = it.scope.copy(currentOnly = value),
                driverA = null,
                driverB = null,
                comparison = AsyncValue.Value(null),
            )
        }
    }

    fun onSeasonPicked(year: String) {
        _uiState.update {
            it.copy(
                scope = it.scope.copy(pickedSeason = year),
                comparison = AsyncValue.Value(null),
            )
        }
    }

    fun setDriverA(driver: Driver) {
        _uiState.update { it.copy(driverA = driver, comparison = AsyncValue.Value(null)) }
    }

    fun setDriverB(driver: Driver) {
        _uiState.update { it.copy(driverB = driver, comparison = AsyncValue.Value(null)) }
    }

    suspend fun loadSeasonYears(): Result<List<String>> = repository.getSeasonYears()

    suspend fun loadDriversForPicker(): Result<List<Driver>> =
        if (_uiState.value.currentOnly) repository.getCurrentDrivers() else repository.getAllDrivers()

    fun compare() {
        val state = _uiState.value
        val a = state.driverA ?: return
        val b = state.driverB ?: return
        val season = state.selectedSeason
        loadJob.launchH2hCompare(
            scope = viewModelScope,
            canCompare = canCompare,
            fetchA = { repository.getDriverH2hCompareData(a.driverId, season) },
            fetchB = { repository.getDriverH2hCompareData(b.driverId, season) },
            onLoading = { _uiState.update { it.copy(comparison = AsyncValue.Loading) } },
            onError = { err -> _uiState.update { it.copy(comparison = err.toAsyncError()) } },
            onSuccess = { dataA, dataB ->
                val timeline = H2hPointsTimeline.fromScores(dataA.scores, dataB.scores, season)
                val constructorIdA = repository.currentConstructorsForDriver(a.driverId)
                    .firstOrNull()?.constructorId
                val constructorIdB = repository.currentConstructorsForDriver(b.driverId)
                    .firstOrNull()?.constructorId
                _uiState.update {
                    it.copy(
                        comparison = AsyncValue.Value(
                            H2hDriverCompareResult(
                                driverA = a,
                                driverB = b,
                                statsA = dataA.stats,
                                statsB = dataB.stats,
                                season = season,
                                timeline = timeline,
                                constructorIdA = constructorIdA,
                                constructorIdB = constructorIdB,
                            ),
                        ),
                    )
                }
            },
        )
    }
}
