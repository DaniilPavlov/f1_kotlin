package com.example.f1_kotlin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1_kotlin.data.repository.IF1Repository
import com.example.f1_kotlin.domain.AppDataRefresh
import com.example.f1_kotlin.domain.AsyncValue
import com.example.f1_kotlin.domain.NetworkReachability
import com.example.f1_kotlin.domain.clearOfflineBannerIfOnline
import com.example.f1_kotlin.domain.model.Circuit
import com.example.f1_kotlin.domain.shouldShowOfflineCachedBanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class CircuitsUiState(
    val circuits: AsyncValue<List<Circuit>> = AsyncValue.Loading,
    val activePage: Int = 0,
    val isRefreshing: Boolean = false,
    val showingCachedData: Boolean = false,
)

/**
 * ViewModel вкладки «Трассы» — список всех трасс F1 (с offline-кэшем).
 * [refreshAll] чистит кэши через [AppDataRefresh] и грузит заново (ErrorBody retry).
 */
@HiltViewModel
class CircuitsViewModel @Inject constructor(
    private val repository: IF1Repository,
    private val appDataRefresh: AppDataRefresh,
    private val networkReachability: NetworkReachability,
) : ViewModel() {
    private val loadJob = LoadJobHolder()

    private val _uiState = MutableStateFlow(CircuitsUiState())
    val uiState: StateFlow<CircuitsUiState> = _uiState.asStateFlow()

    init {
        loadCircuits()
    }

    fun changeActivePage(index: Int) {
        _uiState.update { it.copy(activePage = index) }
    }

    fun loadCircuits() {
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

    private suspend fun loadInternal(clearCaches: Boolean) {
        try {
            if (clearCaches) {
                appDataRefresh.clearAll()
                _uiState.update {
                    it.copy(
                        isRefreshing = true,
                        circuits = if (it.circuits is AsyncValue.Value) it.circuits else AsyncValue.Loading,
                    )
                }
            } else {
                repository.peekCircuitsCache()?.let { cached ->
                    _uiState.update { it.copy(circuits = AsyncValue.Value(cached)) }
                } ?: run {
                    _uiState.update { it.copy(circuits = AsyncValue.Loading) }
                }
            }

            repository.getCircuits().applyUnlessCached(
                current = _uiState.value.circuits,
                onSuccess = { list ->
                    _uiState.update { it.copy(circuits = AsyncValue.Value(list)) }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(circuits = err.toAsyncError()) }
                },
            )
        } finally {
            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    showingCachedData = shouldShowOfflineCachedBanner(
                        hasCachedContent = it.circuits is AsyncValue.Value,
                        reachability = networkReachability,
                    ),
                )
            }
        }
    }
}
