package com.example.f1_kotlin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1_kotlin.data.repository.IF1Repository
import com.example.f1_kotlin.domain.AsyncValue
import com.example.f1_kotlin.domain.model.Constructor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class H2hConstructorsUiState(
    val scope: H2hScopeState = H2hScopeState(),
    val constructorA: Constructor? = null,
    val constructorB: Constructor? = null,
    val comparison: AsyncValue<H2hConstructorCompareResult?> = AsyncValue.Value(null),
) {
    val scopeMode get() = scope.scopeMode
    val useCurrentSeason get() = scope.useCurrentSeason
    val currentOnly get() = scope.currentOnly
    val latestSeason get() = scope.latestSeason
    val pickedSeason get() = scope.pickedSeason
    val isSeasonScope get() = scope.isSeasonScope
    val showYearPicker get() = scope.showYearPicker
    val selectedSeason get() = scope.selectedSeason
    val canCompare get() = scope.canCompare(constructorA?.constructorId, constructorB?.constructorId)
}

@HiltViewModel
class H2hConstructorsViewModel @Inject constructor(
    private val repository: IF1Repository,
) : ViewModel() {
    private val loadJob = LoadJobHolder()

    private val _uiState = MutableStateFlow(H2hConstructorsUiState())
    val uiState: StateFlow<H2hConstructorsUiState> = _uiState.asStateFlow()

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
                constructorA = null,
                constructorB = null,
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

    fun setConstructorA(constructor: Constructor) {
        _uiState.update { it.copy(constructorA = constructor, comparison = AsyncValue.Value(null)) }
    }

    fun setConstructorB(constructor: Constructor) {
        _uiState.update { it.copy(constructorB = constructor, comparison = AsyncValue.Value(null)) }
    }

    suspend fun loadSeasonYears(): Result<List<String>> = repository.getSeasonYears()

    suspend fun loadConstructorsForPicker(): Result<List<Constructor>> =
        if (_uiState.value.currentOnly) {
            repository.getCurrentConstructorsList()
        } else {
            repository.getAllConstructors()
        }

    fun compare() {
        val state = _uiState.value
        val a = state.constructorA ?: return
        val b = state.constructorB ?: return
        val season = state.selectedSeason
        loadJob.launchH2hCompare(
            scope = viewModelScope,
            canCompare = canCompare,
            fetchA = { repository.getConstructorH2hStats(a.constructorId, season) },
            fetchB = { repository.getConstructorH2hStats(b.constructorId, season) },
            onLoading = { _uiState.update { it.copy(comparison = AsyncValue.Loading) } },
            onError = { err -> _uiState.update { it.copy(comparison = err.toAsyncError()) } },
            onSuccess = { statsA, statsB ->
                _uiState.update {
                    it.copy(
                        comparison = AsyncValue.Value(
                            H2hConstructorCompareResult(a, b, statsA, statsB, season),
                        ),
                    )
                }
            },
        )
    }
}
