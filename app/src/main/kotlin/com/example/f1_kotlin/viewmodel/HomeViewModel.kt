package com.example.f1_kotlin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1_kotlin.domain.model.ConstructorStanding
import com.example.f1_kotlin.domain.model.DriverStanding
import com.example.f1_kotlin.data.repository.IF1Repository
import com.example.f1_kotlin.domain.AppError
import com.example.f1_kotlin.domain.AsyncValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class HomeUiState(
    val drivers: AsyncValue<List<DriverStanding>> = AsyncValue.Loading,
    val constructors: AsyncValue<List<ConstructorStanding>> = AsyncValue.Loading,
    val season: String = "",
    val round: String = "",
    val activeTable: Int = 0,
    val error: AppError? = null,
)

/** ViewModel вкладки «Главная» — [LoadJobHolder] + peek-кэш, затем refresh с сети. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: IF1Repository,
) : ViewModel() {
    private val loadJob = LoadJobHolder()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadAllData()
    }

    fun changeActiveTable(index: Int) {
        _uiState.update { it.copy(activeTable = index) }
    }

    fun loadAllData() {
        loadJob.launch(viewModelScope) {
            _uiState.update { it.copy(error = null) }

            repository.peekCurrentDriversCache()?.let { (list, meta) ->
                _uiState.update {
                    it.copy(
                        drivers = AsyncValue.Value(list),
                        season = meta.season,
                        round = meta.round,
                    )
                }
            } ?: run {
                _uiState.update { it.copy(drivers = AsyncValue.Loading) }
            }

            repository.peekCurrentConstructorsCache()?.let { list ->
                _uiState.update { it.copy(constructors = AsyncValue.Value(list)) }
            } ?: run {
                _uiState.update { it.copy(constructors = AsyncValue.Loading) }
            }

            val driversDeferred = async { repository.getCurrentDriverStandings() }
            val constructorsDeferred = async { repository.getCurrentConstructorStandings() }

            driversDeferred.await().applyUnlessCached(
                current = _uiState.value.drivers,
                onSuccess = { (list, meta) ->
                    _uiState.update {
                        it.copy(
                            drivers = AsyncValue.Value(list),
                            season = meta.season,
                            round = meta.round,
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            drivers = err.toAsyncError(),
                            error = err,
                        )
                    }
                },
            )

            constructorsDeferred.await().applyUnlessCached(
                current = _uiState.value.constructors,
                onSuccess = { list ->
                    _uiState.update { it.copy(constructors = AsyncValue.Value(list)) }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            constructors = err.toAsyncError(),
                            error = err,
                        )
                    }
                },
            )
        }
    }
}
