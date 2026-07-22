package com.example.f1_kotlin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1_kotlin.data.repository.IF1Repository
import com.example.f1_kotlin.domain.AsyncValue
import com.example.f1_kotlin.domain.ErrorStrings
import com.example.f1_kotlin.domain.model.Race
import com.example.f1_kotlin.domain.toAppError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class RaceSearchUiState(
    val year: String = "",
    val round: String = "",
    val raceDisplay: String = "",
    val searchedRace: AsyncValue<Race?> = AsyncValue.Value(null),
    val fieldsInputted: Boolean = false,
    val errorMessage: String = "",
    val dataLoaded: Boolean = true,
)

/**
 * ViewModel экрана «Поиск гонки».
 *
 * Пользователь вводит год и номер раунда; по кнопке «Найти» — запрос без кэша.
 * Результат — одна гонка или сообщение «не найдено».
 */
@HiltViewModel
class RaceSearchViewModel @Inject constructor(
    private val repository: IF1Repository,
) : ViewModel() {
    private val loadJob = LoadJobHolder()

    private val _uiState = MutableStateFlow(RaceSearchUiState())
    val uiState: StateFlow<RaceSearchUiState> = _uiState.asStateFlow()

    fun onYearChanged(value: String) {
        _uiState.update {
            it.copy(year = value, round = "", raceDisplay = "")
        }
        checkFields()
    }

    fun onRacePicked(round: String, display: String) {
        _uiState.update { it.copy(round = round, raceDisplay = display) }
        checkFields()
    }

    fun checkFields() {
        _uiState.update {
            it.copy(fieldsInputted = it.year.length == 4 && it.round.isNotEmpty())
        }
    }

    suspend fun loadSeasonYears(): Result<List<String>> = repository.getSeasonYears()

    suspend fun loadSeasonRaces(year: String): Result<List<Race>> = repository.getSeasonRaces(year)

    fun loadRaceResults() {
        loadJob.launch(viewModelScope) {
            val year = _uiState.value.year
            val round = _uiState.value.round
            _uiState.update {
                it.copy(
                    dataLoaded = false,
                    errorMessage = "",
                    searchedRace = AsyncValue.Loading,
                )
            }
            repository.getRaceResults(year, round)
                .onSuccess { race ->
                    if (race != null) {
                        _uiState.update { it.copy(searchedRace = AsyncValue.Value(race)) }
                    } else {
                        _uiState.update {
                            it.copy(
                                searchedRace = AsyncValue.Value(null),
                                errorMessage = ErrorStrings.raceNotFound,
                            )
                        }
                    }
                }
                .onFailure { e ->
                    val err = e.toAppError()
                    _uiState.update {
                        it.copy(
                            searchedRace = err.toAsyncError(),
                            errorMessage = err.title,
                        )
                    }
                }
            _uiState.update { it.copy(dataLoaded = true) }
        }
    }
}
