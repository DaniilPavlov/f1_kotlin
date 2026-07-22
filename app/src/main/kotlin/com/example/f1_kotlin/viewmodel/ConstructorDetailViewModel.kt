package com.example.f1_kotlin.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.f1_kotlin.data.model.NewsArticle
import com.example.f1_kotlin.data.repository.IEspnRepository
import com.example.f1_kotlin.data.repository.IF1Repository
import com.example.f1_kotlin.domain.AppError
import com.example.f1_kotlin.domain.AsyncValue
import com.example.f1_kotlin.domain.ErrorStrings
import com.example.f1_kotlin.domain.toAppError
import com.example.f1_kotlin.ui.navigation.ConstructorDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class ConstructorDetailUiState(
    val constructor: AsyncValue<com.example.f1_kotlin.domain.model.Constructor> = AsyncValue.Loading,
    val careerStats: AsyncValue<
        com.example.f1_kotlin.data.model.CareerStats<com.example.f1_kotlin.domain.model.Driver>
        > = AsyncValue.Loading,
    val news: List<NewsArticle> = emptyList(),
    val error: AppError? = null,
)

@HiltViewModel
class ConstructorDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: IF1Repository,
    private val espnRepository: IEspnRepository,
) : ViewModel() {
    private val loadJob = LoadJobHolder()
    private val constructorId = savedStateHandle.toRoute<ConstructorDetail>().constructorId

    private val _uiState = MutableStateFlow(ConstructorDetailUiState())
    val uiState: StateFlow<ConstructorDetailUiState> = _uiState.asStateFlow()

    init {
        loadAllData()
    }

    fun loadAllData() {
        loadJob.launch(viewModelScope) {
            _uiState.update {
                it.copy(
                    error = null,
                    constructor = AsyncValue.Loading,
                    careerStats = AsyncValue.Loading,
                    news = emptyList(),
                )
            }

            val currentDrivers = repository.currentDriversForConstructor(constructorId)
            val constructorResult = repository.getConstructor(constructorId)
            constructorResult.onFailure { ex ->
                val err = ex.toAppError()
                _uiState.update {
                    it.copy(
                        constructor = err.toAsyncError(),
                        error = err,
                    )
                }
                return@launch
            }
            val loaded = constructorResult.getOrNull()
            if (loaded == null) {
                _uiState.update {
                    it.copy(constructor = AsyncValue.Error(ErrorStrings.constructorNotFound))
                }
                return@launch
            }
            _uiState.update { it.copy(constructor = AsyncValue.Value(loaded)) }

            coroutineScope {
                val careerDeferred = async {
                    repository.getConstructorCareerStats(constructorId, currentDrivers)
                }
                val newsDeferred = async {
                    espnRepository.constructorNews(loaded.constructorId, loaded.name)
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
                _uiState.update { it.copy(news = newsDeferred.await()) }
            }
        }
    }
}
