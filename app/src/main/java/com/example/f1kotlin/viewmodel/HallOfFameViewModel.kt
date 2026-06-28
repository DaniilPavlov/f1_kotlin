package com.example.f1kotlin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1kotlin.data.model.ConstructorStandingsModel
import com.example.f1kotlin.data.model.DriverStandingsModel
import com.example.f1kotlin.data.repository.F1Repository
import com.example.f1kotlin.domain.AppException
import com.example.f1kotlin.domain.AsyncValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/** ViewModel «Зал славы» — peek-кэш по году + refresh, [LoadJobHolder]. */
@HiltViewModel
class HallOfFameViewModel @Inject constructor(
    private val repository: F1Repository,
) : ViewModel() {
    private val loadJob = LoadJobHolder()

    private val _drivers = MutableStateFlow<AsyncValue<List<DriverStandingsModel>>>(AsyncValue.Loading)
    val drivers: StateFlow<AsyncValue<List<DriverStandingsModel>>> = _drivers.asStateFlow()

    private val _constructors = MutableStateFlow<AsyncValue<List<ConstructorStandingsModel>>>(AsyncValue.Loading)
    val constructors: StateFlow<AsyncValue<List<ConstructorStandingsModel>>> = _constructors.asStateFlow()

    private val _year = MutableStateFlow("2026")
    val year: StateFlow<String> = _year.asStateFlow()

    private val _fieldsInputted = MutableStateFlow(false)
    val fieldsInputted: StateFlow<Boolean> = _fieldsInputted.asStateFlow()

    private val _activeTable = MutableStateFlow(0)
    val activeTable: StateFlow<Int> = _activeTable.asStateFlow()

    private val _error = MutableStateFlow<AppException?>(null)
    val error: StateFlow<AppException?> = _error.asStateFlow()

    init {
        checkFields()
        loadAllData()
    }

    fun onYearChanged(value: String) {
        _year.value = value
        checkFields()
    }

    fun checkFields() {
        _fieldsInputted.value = _year.value.length == 4 && _year.value.isNotEmpty()
    }

    fun changeActiveTable(index: Int) {
        _activeTable.value = index
    }

    fun loadAllData() {
        if (!_fieldsInputted.value) return
        loadJob.launch(viewModelScope) {
            _error.value = null

            repository.peekHistoricalStandingsCache(_year.value)?.let { (drivers, constructors) ->
                _drivers.value = AsyncValue.Value(drivers)
                _constructors.value = AsyncValue.Value(constructors)
            } ?: run {
                _drivers.value = AsyncValue.Loading
                _constructors.value = AsyncValue.Loading
            }

            repository.getHistoricalStandings(_year.value).applyUnlessCached(
                current = _drivers.value,
                onSuccess = { (drivers, constructors) ->
                    _drivers.value = AsyncValue.Value(drivers)
                    _constructors.value = AsyncValue.Value(constructors)
                },
                onFailure = { ex ->
                    if (_drivers.value !is AsyncValue.Value) {
                        _drivers.value = AsyncValue.Error(ex.title, ex.subtitle)
                        _constructors.value = AsyncValue.Error(ex.title, ex.subtitle)
                        _error.value = ex
                    }
                },
            )
        }
    }
}
