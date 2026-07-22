package com.example.f1_kotlin.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.f1_kotlin.data.model.EspnDriverCardData
import com.example.f1_kotlin.data.repository.IEspnRepository
import com.example.f1_kotlin.data.repository.IF1Repository
import com.example.f1_kotlin.domain.AppError
import com.example.f1_kotlin.domain.AsyncValue
import com.example.f1_kotlin.domain.ErrorStrings
import com.example.f1_kotlin.domain.toAppError
import com.example.f1_kotlin.ui.navigation.DriverDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class DriverDetailUiState(
    val driver: AsyncValue<com.example.f1_kotlin.domain.model.Driver> = AsyncValue.Loading,
    val careerStats: AsyncValue<
        com.example.f1_kotlin.data.model.CareerStats<com.example.f1_kotlin.domain.model.Constructor>
        > = AsyncValue.Loading,
    val espnCard: EspnDriverCardData = EspnDriverCardData(),
    val error: AppError? = null,
)

@HiltViewModel
class DriverDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: IF1Repository,
    private val espnRepository: IEspnRepository,
) : ViewModel() {
    private val loadJob = LoadJobHolder()
    private val driverId = savedStateHandle.toRoute<DriverDetail>().driverId

    private val _uiState = MutableStateFlow(DriverDetailUiState())
    val uiState: StateFlow<DriverDetailUiState> = _uiState.asStateFlow()

    init {
        loadAllData()
    }

    fun loadAllData() {
        loadJob.launch(viewModelScope) {
            _uiState.update {
                it.copy(
                    error = null,
                    driver = AsyncValue.Loading,
                    careerStats = AsyncValue.Loading,
                    espnCard = EspnDriverCardData(),
                )
            }

            val currentConstructors = repository.currentConstructorsForDriver(driverId)
            val driverResult = repository.getDriver(driverId)
            driverResult.onFailure { ex ->
                val err = ex.toAppError()
                _uiState.update {
                    it.copy(
                        driver = err.toAsyncError(),
                        error = err,
                    )
                }
                return@launch
            }
            val loadedDriver = driverResult.getOrNull()
            if (loadedDriver == null) {
                _uiState.update { it.copy(driver = AsyncValue.Error(ErrorStrings.driverNotFound)) }
                return@launch
            }
            _uiState.update { it.copy(driver = AsyncValue.Value(loadedDriver)) }

            coroutineScope {
                val careerDeferred = async {
                    repository.getDriverCareerStats(driverId, currentConstructors)
                }
                val espnDeferred = async {
                    espnRepository.driverCardData(loadedDriver.givenName, loadedDriver.familyName)
                }

                careerDeferred.await().applyUnlessCached(
                    current = _uiState.value.careerStats,
                    onSuccess = { stats ->
                        _uiState.update { it.copy(careerStats = AsyncValue.Value(stats)) }
                    },
                    onFailure = { err ->
                        _uiState.update {
                            it.copy(
                                careerStats = err.toAsyncError(),
                                error = err,
                            )
                        }
                    },
                )
                _uiState.update { it.copy(espnCard = espnDeferred.await()) }
            }
        }
    }
}
