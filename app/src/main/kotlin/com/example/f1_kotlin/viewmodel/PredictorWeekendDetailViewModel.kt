package com.example.f1_kotlin.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.f1_kotlin.data.repository.IF1Repository
import com.example.f1_kotlin.data.repository.IPredictorRepository
import com.example.f1_kotlin.domain.AppError
import com.example.f1_kotlin.domain.model.Driver
import com.example.f1_kotlin.domain.predictor.PredictorGridKind
import com.example.f1_kotlin.domain.predictor.PredictorScoreService
import com.example.f1_kotlin.domain.predictor.PredictorSessionCompare
import com.example.f1_kotlin.domain.predictor.PredictorWeekendPrediction
import com.example.f1_kotlin.domain.toAppError
import com.example.f1_kotlin.ui.navigation.PredictorWeekendDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PredictorWeekendDetailUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: AppError? = null,
    val weekend: PredictorWeekendPrediction? = null,
    val driversById: Map<String, Driver> = emptyMap(),
    val selectedSession: PredictorGridKind = PredictorGridKind.Qualifying,
    val qualifyingCompare: PredictorSessionCompare? = null,
    val raceCompare: PredictorSessionCompare? = null,
)

@HiltViewModel
class PredictorWeekendDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val f1Repository: IF1Repository,
    private val predictorRepository: IPredictorRepository,
) : ViewModel() {
    private val args = savedStateHandle.toRoute<PredictorWeekendDetail>()
    val season: String = args.season
    val round: String = args.round
    val raceName: String = args.raceName

    private val loadJob = LoadJobHolder()
    private val _uiState = MutableStateFlow(PredictorWeekendDetailUiState())
    val uiState: StateFlow<PredictorWeekendDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        loadJob.launch(viewModelScope) { loadInternal(clearCaches = false) }
    }

    fun refreshAll() {
        loadJob.launch(viewModelScope) { loadInternal(clearCaches = true) }
    }

    fun selectSession(kind: PredictorGridKind) {
        _uiState.update { it.copy(selectedSession = kind) }
    }

    fun activeCompare(state: PredictorWeekendDetailUiState = _uiState.value): PredictorSessionCompare? =
        if (state.selectedSession == PredictorGridKind.Qualifying) {
            state.qualifyingCompare
        } else {
            state.raceCompare
        }

    private suspend fun loadInternal(clearCaches: Boolean) {
        _uiState.update {
            it.copy(
                isLoading = !clearCaches && it.weekend == null,
                isRefreshing = clearCaches || it.weekend != null,
                error = null,
            )
        }
        try {
            val store = predictorRepository.load()
            val weekend = store.weekend(season, round)
                ?: PredictorWeekendPrediction(round = round, raceName = raceName)

            val drivers = f1Repository.getCurrentDrivers().getOrNull().orEmpty()
                .associateBy { it.driverId }

            var qualiActual = weekend.actualQualifyingOrder
            var raceActual = weekend.actualRaceOrder

            if (qualiActual == null) {
                f1Repository.getQualifyingResults(season, round).getOrNull()?.let {
                    qualiActual = PredictorScoreService.qualifyingActualOrder(it)
                }
            }
            if (raceActual == null) {
                f1Repository.getRaceResults(season, round).getOrNull()?.results?.let {
                    raceActual = PredictorScoreService.raceActualOrder(it)
                }
            }

            val qualiCompare = if (weekend.qualifyingOrder.isNotEmpty() || !qualiActual.isNullOrEmpty()) {
                PredictorSessionCompare.fromOrders(weekend.qualifyingOrder, qualiActual.orEmpty())
            } else {
                null
            }
            val raceCompare = if (weekend.raceOrder.isNotEmpty() || !raceActual.isNullOrEmpty()) {
                PredictorSessionCompare.fromOrders(weekend.raceOrder, raceActual.orEmpty())
            } else {
                null
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    weekend = weekend,
                    driversById = drivers,
                    qualifyingCompare = qualiCompare,
                    raceCompare = raceCompare,
                    error = null,
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(isLoading = false, isRefreshing = false, error = e.toAppError())
            }
        }
    }
}
