package com.example.f1kotlin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1kotlin.data.model.RaceModel
import com.example.f1kotlin.data.repository.F1Repository
import com.example.f1kotlin.domain.AsyncValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/** ViewModel «Результаты» — peek-кэш последней гонки, затем сеть. */
@HiltViewModel
class ResultsViewModel @Inject constructor(
    private val repository: F1Repository,
) : ViewModel() {
    private val loadJob = LoadJobHolder()

    private val _lastRace = MutableStateFlow<AsyncValue<RaceModel>>(AsyncValue.Loading)
    val lastRace: StateFlow<AsyncValue<RaceModel>> = _lastRace.asStateFlow()

    init {
        loadAllData()
    }

    fun loadAllData() {
        loadJob.launch(viewModelScope) {
            repository.peekLastRaceCache()?.let { _lastRace.value = AsyncValue.Value(it) }
                ?: run { _lastRace.value = AsyncValue.Loading }

            repository.getLastRace().applyUnlessCached(
                current = _lastRace.value,
                onSuccess = { _lastRace.value = AsyncValue.Value(it) },
                onFailure = { ex -> _lastRace.value = AsyncValue.Error(ex.title, ex.subtitle) },
            )
        }
    }
}
