package com.example.f1_kotlin.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.f1_kotlin.data.repository.IPredictorRepository
import com.example.f1_kotlin.domain.AppError
import com.example.f1_kotlin.domain.predictor.PredictorSeason
import com.example.f1_kotlin.domain.toAppError
import com.example.f1_kotlin.ui.navigation.PredictorSeasonHistory
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PredictorSeasonHistoryUiState(
    val isLoading: Boolean = true,
    val error: AppError? = null,
    val season: PredictorSeason? = null,
)

/** История одного сезона предиктора из Firestore store. */
@HiltViewModel
class PredictorSeasonHistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val predictorRepository: IPredictorRepository,
) : ViewModel() {
    private val args = savedStateHandle.toRoute<PredictorSeasonHistory>()
    val year: String = args.year

    private val loadJob = LoadJobHolder()
    private val _uiState = MutableStateFlow(PredictorSeasonHistoryUiState())
    val uiState: StateFlow<PredictorSeasonHistoryUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        loadJob.launch(viewModelScope) {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val store = predictorRepository.load()
                val season = store.season(year) ?: PredictorSeason(year = year)
                _uiState.update { it.copy(isLoading = false, season = season, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.toAppError()) }
            }
        }
    }
}
