package com.example.f1_kotlin.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.f1_kotlin.data.circuits.CircuitStats
import com.example.f1_kotlin.data.circuits.CircuitStatsRepository
import com.example.f1_kotlin.data.model.CircuitRaceWin
import com.example.f1_kotlin.data.repository.IF1Repository
import com.example.f1_kotlin.domain.AppError
import com.example.f1_kotlin.domain.AsyncValue
import com.example.f1_kotlin.domain.ErrorStrings
import com.example.f1_kotlin.domain.model.Circuit
import com.example.f1_kotlin.ui.navigation.CircuitDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class CircuitDetailUiState(
    val circuit: AsyncValue<Circuit> = AsyncValue.Loading,
    val winners: AsyncValue<List<CircuitRaceWin>> = AsyncValue.Loading,
    val stats: CircuitStats? = null,
    val error: AppError? = null,
    val isRefreshing: Boolean = false,
)

/** Карточка трассы: stats + winners через [IF1Repository]. */
@HiltViewModel
class CircuitDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: IF1Repository,
    private val circuitStatsRepository: CircuitStatsRepository,
) : ViewModel() {
    private val loadJob = LoadJobHolder()
    private val circuitId = savedStateHandle.toRoute<CircuitDetail>().circuitId

    private val _uiState = MutableStateFlow(CircuitDetailUiState())
    val uiState: StateFlow<CircuitDetailUiState> = _uiState.asStateFlow()

    init {
        loadAllData()
    }

    fun loadAllData() {
        loadJob.launch(viewModelScope) {
            loadInternal(softRefresh = false)
        }
    }

    /** Pull-to-refresh: keep Values while reloading. */
    fun refreshAll() {
        loadJob.launch(viewModelScope) {
            loadInternal(softRefresh = true)
        }
    }

    private suspend fun loadInternal(softRefresh: Boolean) {
        try {
            if (softRefresh) {
                _uiState.update {
                    it.copy(
                        isRefreshing = true,
                        error = null,
                        circuit = if (it.circuit is AsyncValue.Value) it.circuit else AsyncValue.Loading,
                        winners = if (it.winners is AsyncValue.Value) it.winners else AsyncValue.Loading,
                    )
                }
            } else {
                _uiState.update { it.copy(error = null) }
            }
            loadCircuit(softRefresh)
            loadStats()
            loadWinners(softRefresh)
        } finally {
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private suspend fun loadCircuit(softRefresh: Boolean) {
        if (!softRefresh) {
            repository.peekCircuitsCache()?.find { it.circuitId == circuitId }?.let { cached ->
                _uiState.update { it.copy(circuit = AsyncValue.Value(cached)) }
            } ?: run {
                _uiState.update { it.copy(circuit = AsyncValue.Loading) }
            }
        }

        repository.getCircuitById(circuitId).applyUnlessCached(
            current = _uiState.value.circuit,
            onSuccess = { found ->
                if (found != null) {
                    _uiState.update { it.copy(circuit = AsyncValue.Value(found)) }
                } else if (_uiState.value.circuit !is AsyncValue.Value) {
                    _uiState.update { it.copy(circuit = AsyncValue.Error(ErrorStrings.circuitNotFound)) }
                }
            },
            onFailure = { err ->
                if (_uiState.value.circuit !is AsyncValue.Value) {
                    _uiState.update { it.copy(circuit = err.toAsyncError()) }
                }
            },
        )
    }

    private suspend fun loadStats() {
        _uiState.update {
            it.copy(stats = runCatching { circuitStatsRepository.of(circuitId) }.getOrNull())
        }
    }

    private suspend fun loadWinners(softRefresh: Boolean) {
        if (!softRefresh || _uiState.value.winners !is AsyncValue.Value) {
            _uiState.update { it.copy(winners = AsyncValue.Loading) }
        }
        repository.getCircuitWinners(circuitId).applyUnlessCached(
            current = _uiState.value.winners,
            onSuccess = { _uiState.update { state -> state.copy(winners = AsyncValue.Value(it)) } },
            onFailure = { err ->
                if (_uiState.value.winners !is AsyncValue.Value) {
                    _uiState.update {
                        it.copy(
                            winners = err.toAsyncError(),
                            error = err,
                        )
                    }
                }
            },
        )
    }
}
