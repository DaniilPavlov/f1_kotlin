package com.example.f1kotlin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1kotlin.data.model.ConstructorStandingsModel
import com.example.f1kotlin.data.model.DriverStandingsModel
import com.example.f1kotlin.data.repository.F1Repository
import com.example.f1kotlin.domain.AppException
import com.example.f1kotlin.domain.AsyncValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/** ViewModel вкладки «Главная» — [LoadJobHolder] + peek-кэш, затем refresh с сети. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: F1Repository,
) : ViewModel() {
    private val loadJob = LoadJobHolder()

    private val _drivers = MutableStateFlow<AsyncValue<List<DriverStandingsModel>>>(AsyncValue.Loading)
    val drivers: StateFlow<AsyncValue<List<DriverStandingsModel>>> = _drivers.asStateFlow()

    private val _constructors = MutableStateFlow<AsyncValue<List<ConstructorStandingsModel>>>(AsyncValue.Loading)
    val constructors: StateFlow<AsyncValue<List<ConstructorStandingsModel>>> = _constructors.asStateFlow()

    private val _season = MutableStateFlow("")
    val season: StateFlow<String> = _season.asStateFlow()

    private val _round = MutableStateFlow("")
    val round: StateFlow<String> = _round.asStateFlow()

    private val _activeTable = MutableStateFlow(0)
    val activeTable: StateFlow<Int> = _activeTable.asStateFlow()

    private val _error = MutableStateFlow<AppException?>(null)
    val error: StateFlow<AppException?> = _error.asStateFlow()

    init {
        loadAllData()
    }

    fun changeActiveTable(index: Int) {
        _activeTable.value = index
    }

    fun loadAllData() {
        loadJob.launch(viewModelScope) {
            _error.value = null

            repository.peekCurrentDriversCache()?.let { (list, meta) ->
                _drivers.value = AsyncValue.Value(list)
                _season.value = meta.season
                _round.value = meta.round
            } ?: run { _drivers.value = AsyncValue.Loading }

            repository.peekCurrentConstructorsCache()?.let {
                _constructors.value = AsyncValue.Value(it)
            } ?: run { _constructors.value = AsyncValue.Loading }

            val driversDeferred = async { repository.getCurrentDriverStandings() }
            val constructorsDeferred = async { repository.getCurrentConstructorStandings() }

            driversDeferred.await().applyUnlessCached(
                current = _drivers.value,
                onSuccess = { (list, meta) ->
                    _drivers.value = AsyncValue.Value(list)
                    _season.value = meta.season
                    _round.value = meta.round
                },
                onFailure = { ex ->
                    _drivers.value = AsyncValue.Error(ex.title, ex.subtitle)
                    _error.value = ex
                },
            )

            constructorsDeferred.await().applyUnlessCached(
                current = _constructors.value,
                onSuccess = { _constructors.value = AsyncValue.Value(it) },
                onFailure = { ex ->
                    _constructors.value = AsyncValue.Error(ex.title, ex.subtitle)
                    _error.value = ex
                },
            )
        }
    }
}
