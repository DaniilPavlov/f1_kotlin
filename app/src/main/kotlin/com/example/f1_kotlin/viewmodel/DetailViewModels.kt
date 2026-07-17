package com.example.f1_kotlin.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1_kotlin.data.model.CircuitModel
import com.example.f1_kotlin.data.model.PitStopModel
import com.example.f1_kotlin.data.model.QualifyingResultModel
import com.example.f1_kotlin.data.model.RaceModel
import com.example.f1_kotlin.data.repository.F1Repository
import com.example.f1_kotlin.domain.AppException
import com.example.f1_kotlin.domain.AsyncValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class RaceInfoScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: F1Repository,
) : ViewModel() {
    private val loadJob = LoadJobHolder()
    private val season: String = savedStateHandle.get<String>("season").orEmpty()
    private val round: String = savedStateHandle.get<String>("round").orEmpty()

    private val _race = MutableStateFlow<AsyncValue<RaceModel>>(AsyncValue.Loading)
    val race: StateFlow<AsyncValue<RaceModel>> = _race.asStateFlow()

    private val _qualifying = MutableStateFlow<AsyncValue<List<QualifyingResultModel>>>(AsyncValue.Loading)
    val qualifying: StateFlow<AsyncValue<List<QualifyingResultModel>>> = _qualifying.asStateFlow()

    private val _pitStops = MutableStateFlow<AsyncValue<List<PitStopModel>>>(AsyncValue.Loading)
    val pitStops: StateFlow<AsyncValue<List<PitStopModel>>> = _pitStops.asStateFlow()

    private val _allDataLoaded = MutableStateFlow(false)
    val allDataLoaded: StateFlow<Boolean> = _allDataLoaded.asStateFlow()

    private val _error = MutableStateFlow<AppException?>(null)
    val error: StateFlow<AppException?> = _error.asStateFlow()

    init {
        loadAllData()
    }

    fun loadAllData() {
        loadJob.launch(viewModelScope) {
            _error.value = null
            _allDataLoaded.value = false
            _race.value = AsyncValue.Loading
            _qualifying.value = AsyncValue.Loading
            _pitStops.value = AsyncValue.Loading

            val raceResult = repository.getRaceResults(season, round)
            raceResult.onFailure { e ->
                val ex = e as AppException
                _race.value = AsyncValue.Error(ex.title, ex.subtitle)
                _error.value = ex
            }
            if (raceResult.isFailure) return@launch

            val loadedRace = raceResult.getOrNull()
            if (loadedRace == null) {
                val ex = AppException("Гонка не найдена")
                _race.value = AsyncValue.Error(ex.title, ex.subtitle)
                _error.value = ex
                return@launch
            }

            _race.value = AsyncValue.Value(loadedRace)
            _allDataLoaded.value = true
            loadExtraSections(loadedRace)
        }
    }

    private suspend fun loadExtraSections(race: RaceModel) {
        coroutineScope {
            val qualifyingDeferred = async { repository.getQualifyingResults(race.season, race.round) }
            val pitStopsDeferred = async { repository.getPitStopsWithDriverNames(race.season, race.round) }

            qualifyingDeferred.await().applyUnlessCached(
                current = _qualifying.value,
                onSuccess = { _qualifying.value = AsyncValue.Value(it) },
                onFailure = { ex ->
                    _qualifying.value = AsyncValue.Error(ex.title, ex.subtitle)
                    _error.value = ex
                },
            )

            pitStopsDeferred.await().applyUnlessCached(
                current = _pitStops.value,
                onSuccess = { _pitStops.value = AsyncValue.Value(it) },
                onFailure = { ex ->
                    _pitStops.value = AsyncValue.Error(ex.title, ex.subtitle)
                    _error.value = ex
                },
            )
        }

    }
}

@HiltViewModel
class CircuitDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: F1Repository,
) : ViewModel() {
    private val loadJob = LoadJobHolder()
    private val circuitId: String = savedStateHandle.get<String>("circuitId").orEmpty()

    private val _circuit = MutableStateFlow<AsyncValue<CircuitModel>>(AsyncValue.Loading)
    val circuit: StateFlow<AsyncValue<CircuitModel>> = _circuit.asStateFlow()

    init {
        loadCircuit()
    }

    fun loadCircuit() {
        loadJob.launch(viewModelScope) {
            repository.peekCircuitsCache()?.find { it.circuitId == circuitId }?.let {
                _circuit.value = AsyncValue.Value(it)
            } ?: run { _circuit.value = AsyncValue.Loading }

            repository.getCircuitById(circuitId).applyUnlessCached(
                current = _circuit.value,
                onSuccess = { found ->
                    if (found != null) {
                        _circuit.value = AsyncValue.Value(found)
                    } else {
                        _circuit.value = AsyncValue.Error("Трасса не найдена")
                    }
                },
                onFailure = { ex -> _circuit.value = AsyncValue.Error(ex.title, ex.subtitle) },
            )
        }
    }
}
