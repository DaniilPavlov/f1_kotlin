package com.example.f1_kotlin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1_kotlin.domain.model.ConstructorStanding
import com.example.f1_kotlin.domain.model.DriverStanding
import com.example.f1_kotlin.data.repository.IF1Repository
import com.example.f1_kotlin.domain.AppError
import com.example.f1_kotlin.domain.AsyncValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HallOfFameUiState(
    val drivers: AsyncValue<List<DriverStanding>> = AsyncValue.Loading,
    val constructors: AsyncValue<List<ConstructorStanding>> = AsyncValue.Loading,
    val year: String = "",
    val fieldsInputted: Boolean = false,
    val activeTable: Int = 0,
    val error: AppError? = null,
)

/** ViewModel «Зал славы» — peek-кэш по году + refresh, [LoadJobHolder]. */
@HiltViewModel
class HallOfFameViewModel @Inject constructor(
    private val repository: IF1Repository,
) : ViewModel() {
    private val loadJob = LoadJobHolder()

    private val _uiState = MutableStateFlow(HallOfFameUiState())
    val uiState: StateFlow<HallOfFameUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getSeasonYears().onSuccess { years ->
                if (_uiState.value.year.isEmpty() && years.isNotEmpty()) {
                    _uiState.update { it.copy(year = years.first()) }
                    checkFields()
                    loadAllData()
                }
            }
        }
    }

    fun onYearChanged(value: String) {
        _uiState.update { it.copy(year = value) }
        checkFields()
        if (_uiState.value.fieldsInputted) {
            loadAllData()
        }
    }

    fun checkFields() {
        val year = _uiState.value.year
        _uiState.update {
            it.copy(fieldsInputted = year.length == 4 && year.isNotEmpty())
        }
    }

    fun changeActiveTable(index: Int) {
        _uiState.update { it.copy(activeTable = index) }
    }

    suspend fun loadSeasonYears(): Result<List<String>> = repository.getSeasonYears()

    fun loadAllData() {
        if (!_uiState.value.fieldsInputted) return
        loadJob.launch(viewModelScope) {
            _uiState.update { it.copy(error = null) }
            val year = _uiState.value.year

            repository.peekHistoricalStandingsCache(year)?.let { (drivers, constructors) ->
                _uiState.update {
                    it.copy(
                        drivers = AsyncValue.Value(drivers),
                        constructors = AsyncValue.Value(constructors),
                    )
                }
            } ?: run {
                _uiState.update {
                    it.copy(
                        drivers = AsyncValue.Loading,
                        constructors = AsyncValue.Loading,
                    )
                }
            }

            repository.getHistoricalStandings(year).applyUnlessCached(
                current = _uiState.value.drivers,
                onSuccess = { (drivers, constructors) ->
                    _uiState.update {
                        it.copy(
                            drivers = AsyncValue.Value(drivers),
                            constructors = AsyncValue.Value(constructors),
                        )
                    }
                },
                onFailure = { err ->
                    if (_uiState.value.drivers !is AsyncValue.Value) {
                        _uiState.update {
                            it.copy(
                                drivers = err.toAsyncError(),
                                constructors = err.toAsyncError(),
                                error = err,
                            )
                        }
                    }
                },
            )
        }
    }
}
