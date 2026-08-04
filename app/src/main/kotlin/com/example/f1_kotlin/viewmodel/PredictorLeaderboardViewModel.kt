package com.example.f1_kotlin.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.f1_kotlin.data.repository.IPredictorLeaderboardRepository
import com.example.f1_kotlin.domain.AppError
import com.example.f1_kotlin.domain.predictor.PredictorLeaderboardEntry
import com.example.f1_kotlin.domain.predictor.PredictorLeaderboardProfile
import com.example.f1_kotlin.domain.predictor.PredictorLeaderboardResult
import com.example.f1_kotlin.domain.predictor.PredictorNickname
import com.example.f1_kotlin.domain.toAppError
import com.example.f1_kotlin.ui.navigation.PredictorLeaderboard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PredictorLeaderboardUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: AppError? = null,
    val formErrorKey: String? = null,
    val profile: PredictorLeaderboardProfile = PredictorLeaderboardProfile(),
    val entries: List<PredictorLeaderboardEntry> = emptyList(),
    val nicknameDraft: String = "",
    val optInAgreed: Boolean = false,
    val year: String = "",
    val myPoints: Int = 0,
)

/** Экран лидерборда: join/leave/никнейм и ранжированный список. */
@HiltViewModel
class PredictorLeaderboardViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val leaderboardRepository: IPredictorLeaderboardRepository,
) : ViewModel() {
    private val args = savedStateHandle.toRoute<PredictorLeaderboard>()

    private val loadJob = LoadJobHolder()
    private val _uiState = MutableStateFlow(
        PredictorLeaderboardUiState(year = args.year, myPoints = args.myPoints),
    )
    val uiState: StateFlow<PredictorLeaderboardUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        loadJob.launch(viewModelScope) {
            _uiState.update { it.copy(isLoading = true, error = null, formErrorKey = null) }
            try {
                val profile = leaderboardRepository.loadProfile()
                val entries = leaderboardRepository.loadLeaderboard(args.year)
                val ranked = entries.mapIndexed { index, e -> e.withRank(index + 1) }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        profile = profile,
                        entries = ranked,
                        nicknameDraft = profile.nickname.orEmpty(),
                        optInAgreed = profile.leaderboardOptIn,
                        error = null,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.toAppError())
                }
            }
        }
    }

    fun setNicknameDraft(value: String) {
        _uiState.update { it.copy(nicknameDraft = value, formErrorKey = null) }
    }

    fun setOptInAgreed(value: Boolean) {
        _uiState.update { it.copy(optInAgreed = value, formErrorKey = null) }
    }

    fun join() {
        val state = _uiState.value
        if (!state.optInAgreed) {
            _uiState.update { it.copy(formErrorKey = "predictorLeaderboardOptInRequired") }
            return
        }
        PredictorNickname.validate(state.nicknameDraft)?.let { key ->
            _uiState.update { it.copy(formErrorKey = key) }
            return
        }
        runSave {
            when (
                val result = leaderboardRepository.join(
                    nickname = state.nicknameDraft,
                    year = state.year,
                    totalPoints = state.myPoints,
                )
            ) {
                PredictorLeaderboardResult.Ok -> load()
                is PredictorLeaderboardResult.Fail ->
                    _uiState.update { it.copy(formErrorKey = result.errorKey) }
            }
        }
    }

    fun saveNickname() {
        val state = _uiState.value
        PredictorNickname.validate(state.nicknameDraft)?.let { key ->
            _uiState.update { it.copy(formErrorKey = key) }
            return
        }
        runSave {
            when (
                val result = leaderboardRepository.updateNickname(
                    nickname = state.nicknameDraft,
                    year = state.year,
                )
            ) {
                PredictorLeaderboardResult.Ok -> load()
                is PredictorLeaderboardResult.Fail ->
                    _uiState.update { it.copy(formErrorKey = result.errorKey) }
            }
        }
    }

    fun leave(onLeft: () -> Unit = {}) {
        val state = _uiState.value
        runSave {
            when (val result = leaderboardRepository.leave(state.year)) {
                PredictorLeaderboardResult.Ok -> {
                    load()
                    onLeft()
                }
                is PredictorLeaderboardResult.Fail ->
                    _uiState.update { it.copy(formErrorKey = result.errorKey) }
            }
        }
    }

    fun myEntry(state: PredictorLeaderboardUiState = _uiState.value): PredictorLeaderboardEntry? {
        val uid = leaderboardRepository.currentUid ?: return null
        return state.entries.firstOrNull { it.uid == uid }
    }

    fun showJoinForm(state: PredictorLeaderboardUiState = _uiState.value): Boolean =
        !state.profile.canShowOnLeaderboard

    private fun runSave(block: suspend () -> Unit) {
        loadJob.launch(viewModelScope) {
            _uiState.update { it.copy(isSaving = true, formErrorKey = null) }
            try {
                block()
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}
