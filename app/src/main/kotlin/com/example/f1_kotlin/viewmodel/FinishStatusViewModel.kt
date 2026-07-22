package com.example.f1_kotlin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1_kotlin.data.model.FinishStatusItem
import com.example.f1_kotlin.data.repository.IF1Repository
import com.example.f1_kotlin.domain.AsyncValue
import com.example.f1_kotlin.domain.toAppError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FinishStatusUiState(
    val year: String = "",
    val statuses: AsyncValue<List<FinishStatusItem>> = AsyncValue.Loading,
)

@HiltViewModel
class FinishStatusViewModel @Inject constructor(
    private val repository: IF1Repository,
) : ViewModel() {
    private val loadJob = LoadJobHolder()

    private val _uiState = MutableStateFlow(FinishStatusUiState())
    val uiState: StateFlow<FinishStatusUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getSeasonYears().onSuccess { years ->
                if (_uiState.value.year.isEmpty() && years.isNotEmpty()) {
                    _uiState.update { it.copy(year = years.first()) }
                    loadAllData()
                }
            }
        }
    }

    fun onYearChanged(value: String) {
        _uiState.update { it.copy(year = value) }
        if (value.length == 4) loadAllData()
    }

    suspend fun loadSeasonYears(): Result<List<String>> = repository.getSeasonYears()

    fun loadAllData() {
        if (_uiState.value.year.length != 4) return
        loadJob.launch(viewModelScope) {
            _uiState.update { it.copy(statuses = AsyncValue.Loading) }
            repository.getSeasonFinishStatuses(_uiState.value.year).fold(
                onSuccess = { list ->
                    _uiState.update { it.copy(statuses = AsyncValue.Value(list)) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(statuses = e.toAppError().toAsyncError()) }
                },
            )
        }
    }
}
