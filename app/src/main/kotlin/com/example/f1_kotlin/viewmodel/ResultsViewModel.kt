package com.example.f1_kotlin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1_kotlin.data.api.EspnApiService
import com.example.f1_kotlin.data.model.EspnScoreboardEvent
import com.example.f1_kotlin.domain.model.Race
import com.example.f1_kotlin.data.repository.IEspnRepository
import com.example.f1_kotlin.data.repository.IF1Repository
import com.example.f1_kotlin.domain.AppDataRefresh
import com.example.f1_kotlin.domain.AsyncValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResultsUiState(
    val lastRace: AsyncValue<Race> = AsyncValue.Loading,
    val scoreboard: AsyncValue<EspnScoreboardEvent?> = AsyncValue.Loading,
    val isRefreshing: Boolean = false,
)

/**
 * ViewModel «Результаты» — последняя гонка + ESPN weekend scoreboard.
 * [refreshAll] чистит кэши через [AppDataRefresh] и грузит заново (ErrorBody retry).
 */
@HiltViewModel
class ResultsViewModel @Inject constructor(
    private val repository: IF1Repository,
    private val espnRepository: IEspnRepository,
    private val appDataRefresh: AppDataRefresh,
) : ViewModel() {
    private val loadJob = LoadJobHolder()
    private var pollJob: Job? = null

    private val _uiState = MutableStateFlow(ResultsUiState())
    val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

    init {
        loadAllData()
    }

    fun loadAllData() {
        loadJob.launch(viewModelScope) {
            loadInternal(clearCaches = false)
        }
    }

    /** ErrorBody: сброс кэшей, затем сеть. */
    fun refreshAll() {
        loadJob.launch(viewModelScope) {
            loadInternal(clearCaches = true)
        }
    }

    fun loadScoreboard(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            loadScoreboardInternal(forceRefresh)
        }
    }

    private suspend fun loadInternal(clearCaches: Boolean) = coroutineScope {
        try {
            if (clearCaches) {
                appDataRefresh.clearAll()
                _uiState.update {
                    it.copy(
                        isRefreshing = true,
                        lastRace = if (it.lastRace is AsyncValue.Value) it.lastRace else AsyncValue.Loading,
                        scoreboard = if (it.scoreboard is AsyncValue.Value) {
                            it.scoreboard
                        } else {
                            AsyncValue.Loading
                        },
                    )
                }
            }
            val raceDeferred = async { loadLastRaceInternal(skipPeek = clearCaches) }
            val scoreboardDeferred = async { loadScoreboardInternal(forceRefresh = clearCaches) }
            raceDeferred.await()
            scoreboardDeferred.await()
        } finally {
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private suspend fun loadLastRaceInternal(skipPeek: Boolean = false) {
        if (!skipPeek) {
            repository.peekLastRaceCache()?.let { race ->
                _uiState.update { it.copy(lastRace = AsyncValue.Value(race)) }
            } ?: run {
                _uiState.update { it.copy(lastRace = AsyncValue.Loading) }
            }
        }

        repository.getLastRace().applyUnlessCached(
            current = _uiState.value.lastRace,
            onSuccess = { race ->
                _uiState.update { it.copy(lastRace = AsyncValue.Value(race)) }
            },
            onFailure = { err ->
                _uiState.update { it.copy(lastRace = err.toAsyncError()) }
            },
        )
    }

    /** ESPN scoreboard: cache first; network failure never breaks Results (hide silently). */
    private suspend fun loadScoreboardInternal(forceRefresh: Boolean) {
        if (!forceRefresh) {
            if (espnRepository.isScoreboardFresh) {
                _uiState.update {
                    it.copy(scoreboard = AsyncValue.Value(espnRepository.peekScoreboard))
                }
                syncLivePolling()
                return
            }
            espnRepository.peekScoreboard?.let { event ->
                _uiState.update { it.copy(scoreboard = AsyncValue.Value(event)) }
            } ?: run {
                if (_uiState.value.scoreboard !is AsyncValue.Value) {
                    _uiState.update { it.copy(scoreboard = AsyncValue.Loading) }
                }
            }
        } else if (_uiState.value.scoreboard !is AsyncValue.Value) {
            _uiState.update { it.copy(scoreboard = AsyncValue.Loading) }
        }

        espnRepository.getScoreboardEvent(forceRefresh = forceRefresh).fold(
            onSuccess = { event ->
                _uiState.update { it.copy(scoreboard = AsyncValue.Value(event)) }
            },
            onFailure = {
                if (_uiState.value.scoreboard !is AsyncValue.Value) {
                    _uiState.update { it.copy(scoreboard = AsyncValue.Value(null)) }
                }
            },
        )
        syncLivePolling()
    }

    private fun syncLivePolling() {
        val live = _uiState.value.scoreboard.getOrNull()?.isLive == true
        if (live) startLivePolling() else stopLivePolling()
    }

    private fun startLivePolling() {
        if (pollJob?.isActive == true) return
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(EspnApiService.SCOREBOARD_POLL_INTERVAL_MS)
                if (_uiState.value.scoreboard.getOrNull()?.isLive != true) {
                    stopLivePolling()
                    return@launch
                }
                loadScoreboardInternal(forceRefresh = true)
            }
        }
    }

    private fun stopLivePolling() {
        pollJob?.cancel()
        pollJob = null
    }

    override fun onCleared() {
        stopLivePolling()
        super.onCleared()
    }
}
