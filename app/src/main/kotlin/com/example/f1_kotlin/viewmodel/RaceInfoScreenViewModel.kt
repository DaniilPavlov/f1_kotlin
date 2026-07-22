package com.example.f1_kotlin.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.f1_kotlin.data.repository.IF1Repository
import com.example.f1_kotlin.domain.AppError
import com.example.f1_kotlin.domain.AsyncValue
import com.example.f1_kotlin.domain.ErrorStrings
import com.example.f1_kotlin.domain.model.PitStop
import com.example.f1_kotlin.domain.model.QualifyingResult
import com.example.f1_kotlin.domain.model.Race
import com.example.f1_kotlin.domain.model.RaceResult
import com.example.f1_kotlin.domain.toAppError
import com.example.f1_kotlin.ui.navigation.RaceInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class RaceInfoUiState(
    val race: AsyncValue<Race> = AsyncValue.Loading,
    val qualifying: AsyncValue<List<QualifyingResult>> = AsyncValue.Loading,
    val pitStops: AsyncValue<List<PitStop>> = AsyncValue.Loading,
    val sprint: AsyncValue<List<RaceResult>> = AsyncValue.Loading,
    val error: AppError? = null,
)

@HiltViewModel
class RaceInfoScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: IF1Repository,
) : ViewModel() {
    private val loadJob = LoadJobHolder()
    private val args = savedStateHandle.toRoute<RaceInfo>()
    private val season = args.season
    private val round = args.round

    private val _uiState = MutableStateFlow(RaceInfoUiState())
    val uiState: StateFlow<RaceInfoUiState> = _uiState.asStateFlow()

    init {
        loadAllData()
    }

    fun loadAllData() {
        loadJob.launch(viewModelScope) {
            _uiState.update {
                it.copy(
                    error = null,
                    race = AsyncValue.Loading,
                    qualifying = AsyncValue.Loading,
                    pitStops = AsyncValue.Loading,
                    sprint = AsyncValue.Loading,
                )
            }

            val raceResult = repository.getRaceResults(season, round)
            raceResult.onFailure { e ->
                val err = e.toAppError()
                _uiState.update {
                    it.copy(
                        race = err.toAsyncError(),
                        error = err,
                    )
                }
            }
            if (raceResult.isFailure) return@launch

            val loadedRace = raceResult.getOrNull()
            if (loadedRace == null) {
                val err = AppError(ErrorStrings.raceNotFound)
                _uiState.update {
                    it.copy(
                        race = err.toAsyncError(),
                        error = err,
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(race = AsyncValue.Value(loadedRace)) }
            loadExtraSections(loadedRace)
        }
    }

    private suspend fun loadExtraSections(race: Race) {
        coroutineScope {
            val qualifyingDeferred = async { repository.getQualifyingResults(race.season, race.round) }
            val pitStopsDeferred = async { repository.getPitStopsWithDriverNames(race.season, race.round) }
            val sprintDeferred = async { repository.getSprintResults(race.season, race.round) }

            qualifyingDeferred.await().applyUnlessCached(
                current = _uiState.value.qualifying,
                onSuccess = { _uiState.update { state -> state.copy(qualifying = AsyncValue.Value(it)) } },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            qualifying = err.toAsyncError(),
                            error = err,
                        )
                    }
                },
            )

            pitStopsDeferred.await().applyUnlessCached(
                current = _uiState.value.pitStops,
                onSuccess = { _uiState.update { state -> state.copy(pitStops = AsyncValue.Value(it)) } },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            pitStops = err.toAsyncError(),
                            error = err,
                        )
                    }
                },
            )

            sprintDeferred.await().applyUnlessCached(
                current = _uiState.value.sprint,
                onSuccess = { _uiState.update { state -> state.copy(sprint = AsyncValue.Value(it)) } },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            sprint = err.toAsyncError(),
                            error = err,
                        )
                    }
                },
            )
        }
    }
}
