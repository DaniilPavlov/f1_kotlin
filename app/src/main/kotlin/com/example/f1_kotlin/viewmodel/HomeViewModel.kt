package com.example.f1_kotlin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1_kotlin.domain.model.ConstructorStanding
import com.example.f1_kotlin.domain.model.DriverStanding
import com.example.f1_kotlin.data.repository.IF1Repository
import com.example.f1_kotlin.domain.AppDataRefresh
import com.example.f1_kotlin.domain.AppError
import com.example.f1_kotlin.domain.AsyncValue
import com.example.f1_kotlin.domain.NetworkReachability
import com.example.f1_kotlin.domain.clearOfflineBannerIfOnline
import com.example.f1_kotlin.domain.shouldShowOfflineCachedBanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
    val isRefreshing: Boolean = false,
    val showingCachedData: Boolean = false,
)

/**
 * ViewModel вкладки «Главная» — [LoadJobHolder] + peek-кэш, затем refresh с сети.
 * [refreshAll] чистит кэши через [AppDataRefresh] и грузит заново (ErrorBody retry / pull-to-refresh).
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: IF1Repository,
    private val appDataRefresh: AppDataRefresh,
    private val networkReachability: NetworkReachability,
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
            loadInternal(clearCaches = false)
        }
    }

    /** ErrorBody / pull-to-refresh: сброс кэшей, затем сеть. */
    fun refreshAll() {
        loadJob.launch(viewModelScope) {
            loadInternal(clearCaches = true)
        }
    }

    /** После resume: спрятать баннер, если сеть снова есть. */
    fun dismissOfflineBannerIfOnline() {
        _uiState.update {
            it.copy(
                showingCachedData = clearOfflineBannerIfOnline(
                    currentlyShowing = it.showingCachedData,
                    reachability = networkReachability,
                ),
            )
        }
    }

    private suspend fun loadInternal(clearCaches: Boolean) = coroutineScope {
        try {
            if (clearCaches) {
                appDataRefresh.clearAll()
                _uiState.update {
                    it.copy(
                        isRefreshing = true,
                        error = null,
                        drivers = if (it.drivers is AsyncValue.Value) it.drivers else AsyncValue.Loading,
                        constructors = if (it.constructors is AsyncValue.Value) {
                            it.constructors
                        } else {
                            AsyncValue.Loading
                        },
                    )
                }
            } else {
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
        } finally {
            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    showingCachedData = shouldShowOfflineCachedBanner(
                        hasCachedContent = it.drivers is AsyncValue.Value &&
                            it.constructors is AsyncValue.Value,
                        reachability = networkReachability,
                    ),
                )
            }
        }
    }
}
