package com.example.f1_kotlin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1_kotlin.data.model.ConstructorModel
import com.example.f1_kotlin.data.model.DriverModel
import com.example.f1_kotlin.data.model.FinishStatusItem
import com.example.f1_kotlin.data.model.H2hStats
import com.example.f1_kotlin.data.repository.F1Repository
import com.example.f1_kotlin.domain.AppException
import com.example.f1_kotlin.domain.AsyncValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class H2hDriverCompareResult(
    val driverA: DriverModel,
    val driverB: DriverModel,
    val statsA: H2hStats,
    val statsB: H2hStats,
    val season: String?,
)

data class H2hConstructorCompareResult(
    val constructorA: ConstructorModel,
    val constructorB: ConstructorModel,
    val statsA: H2hStats,
    val statsB: H2hStats,
    val season: String?,
)

@HiltViewModel
class H2hDriversViewModel @Inject constructor(
    private val repository: F1Repository,
) : ViewModel() {
    private val loadJob = LoadJobHolder()

    private val _scopeMode = MutableStateFlow(0)
    val scopeMode: StateFlow<Int> = _scopeMode.asStateFlow()

    private val _useCurrentSeason = MutableStateFlow(true)
    val useCurrentSeason: StateFlow<Boolean> = _useCurrentSeason.asStateFlow()

    private val _currentOnly = MutableStateFlow(true)
    val currentOnly: StateFlow<Boolean> = _currentOnly.asStateFlow()

    private val _latestSeason = MutableStateFlow("")
    val latestSeason: StateFlow<String> = _latestSeason.asStateFlow()

    private val _pickedSeason = MutableStateFlow("")
    val pickedSeason: StateFlow<String> = _pickedSeason.asStateFlow()

    private val _driverA = MutableStateFlow<DriverModel?>(null)
    val driverA: StateFlow<DriverModel?> = _driverA.asStateFlow()

    private val _driverB = MutableStateFlow<DriverModel?>(null)
    val driverB: StateFlow<DriverModel?> = _driverB.asStateFlow()

    private val _comparison = MutableStateFlow<AsyncValue<H2hDriverCompareResult?>>(AsyncValue.Value(null))
    val comparison: StateFlow<AsyncValue<H2hDriverCompareResult?>> = _comparison.asStateFlow()

    val isSeasonScope: Boolean get() = _scopeMode.value == 1
    val showYearPicker: Boolean get() = isSeasonScope && !_useCurrentSeason.value

    val selectedSeason: String?
        get() {
            if (!isSeasonScope) return null
            return if (_useCurrentSeason.value) {
                _latestSeason.value.takeIf { it.isNotEmpty() }
            } else {
                _pickedSeason.value.takeIf { it.length == 4 }
            }
        }

    val canCompare: Boolean
        get() {
            val a = _driverA.value
            val b = _driverB.value
            return a != null && b != null && a.driverId != b.driverId &&
                (!isSeasonScope || selectedSeason != null)
        }

    init {
        viewModelScope.launch {
            repository.getSeasonYears().onSuccess { years ->
                if (years.isNotEmpty()) {
                    _latestSeason.value = years.first()
                    _pickedSeason.value = years.first()
                }
            }
        }
    }

    fun setScopeMode(mode: Int) {
        if (_scopeMode.value == mode) return
        _scopeMode.value = mode
        resetComparison()
    }

    fun setUseCurrentSeason(value: Boolean) {
        if (_useCurrentSeason.value == value) return
        _useCurrentSeason.value = value
        resetComparison()
    }

    fun setCurrentOnly(value: Boolean) {
        if (_currentOnly.value == value) return
        _currentOnly.value = value
        _driverA.value = null
        _driverB.value = null
        resetComparison()
    }

    fun onSeasonPicked(year: String) {
        _pickedSeason.value = year
        resetComparison()
    }

    fun setDriverA(driver: DriverModel) {
        _driverA.value = driver
        resetComparison()
    }

    fun setDriverB(driver: DriverModel) {
        _driverB.value = driver
        resetComparison()
    }

    suspend fun loadSeasonYears(): Result<List<String>> = repository.getSeasonYears()

    suspend fun loadDriversForPicker(): Result<List<DriverModel>> =
        if (_currentOnly.value) repository.getCurrentDrivers() else repository.getAllDrivers()

    fun compare() {
        if (!canCompare) return
        val a = _driverA.value ?: return
        val b = _driverB.value ?: return
        val season = selectedSeason
        loadJob.launch(viewModelScope) {
            _comparison.value = AsyncValue.Loading
            coroutineScope {
                val statsADeferred = async { repository.getDriverH2hStats(a.driverId, season) }
                val statsBDeferred = async { repository.getDriverH2hStats(b.driverId, season) }
                val statsA = statsADeferred.await()
                val statsB = statsBDeferred.await()
                val leftEx = statsA.exceptionOrNull() as? AppException
                if (leftEx != null) {
                    _comparison.value = AsyncValue.Error(leftEx.title, leftEx.subtitle)
                    return@coroutineScope
                }
                val rightEx = statsB.exceptionOrNull() as? AppException
                if (rightEx != null) {
                    _comparison.value = AsyncValue.Error(rightEx.title, rightEx.subtitle)
                    return@coroutineScope
                }
                _comparison.value = AsyncValue.Value(
                    H2hDriverCompareResult(a, b, statsA.getOrThrow(), statsB.getOrThrow(), season),
                )
            }
        }
    }

    private fun resetComparison() {
        _comparison.value = AsyncValue.Value(null)
    }
}

@HiltViewModel
class H2hConstructorsViewModel @Inject constructor(
    private val repository: F1Repository,
) : ViewModel() {
    private val loadJob = LoadJobHolder()

    private val _scopeMode = MutableStateFlow(0)
    val scopeMode: StateFlow<Int> = _scopeMode.asStateFlow()

    private val _useCurrentSeason = MutableStateFlow(true)
    val useCurrentSeason: StateFlow<Boolean> = _useCurrentSeason.asStateFlow()

    private val _currentOnly = MutableStateFlow(true)
    val currentOnly: StateFlow<Boolean> = _currentOnly.asStateFlow()

    private val _latestSeason = MutableStateFlow("")
    val latestSeason: StateFlow<String> = _latestSeason.asStateFlow()

    private val _pickedSeason = MutableStateFlow("")
    val pickedSeason: StateFlow<String> = _pickedSeason.asStateFlow()

    private val _constructorA = MutableStateFlow<ConstructorModel?>(null)
    val constructorA: StateFlow<ConstructorModel?> = _constructorA.asStateFlow()

    private val _constructorB = MutableStateFlow<ConstructorModel?>(null)
    val constructorB: StateFlow<ConstructorModel?> = _constructorB.asStateFlow()

    private val _comparison =
        MutableStateFlow<AsyncValue<H2hConstructorCompareResult?>>(AsyncValue.Value(null))
    val comparison: StateFlow<AsyncValue<H2hConstructorCompareResult?>> = _comparison.asStateFlow()

    val isSeasonScope: Boolean get() = _scopeMode.value == 1
    val showYearPicker: Boolean get() = isSeasonScope && !_useCurrentSeason.value

    val selectedSeason: String?
        get() {
            if (!isSeasonScope) return null
            return if (_useCurrentSeason.value) {
                _latestSeason.value.takeIf { it.isNotEmpty() }
            } else {
                _pickedSeason.value.takeIf { it.length == 4 }
            }
        }

    val canCompare: Boolean
        get() {
            val a = _constructorA.value
            val b = _constructorB.value
            return a != null && b != null && a.constructorId != b.constructorId &&
                (!isSeasonScope || selectedSeason != null)
        }

    init {
        viewModelScope.launch {
            repository.getSeasonYears().onSuccess { years ->
                if (years.isNotEmpty()) {
                    _latestSeason.value = years.first()
                    _pickedSeason.value = years.first()
                }
            }
        }
    }

    fun setScopeMode(mode: Int) {
        if (_scopeMode.value == mode) return
        _scopeMode.value = mode
        resetComparison()
    }

    fun setUseCurrentSeason(value: Boolean) {
        if (_useCurrentSeason.value == value) return
        _useCurrentSeason.value = value
        resetComparison()
    }

    fun setCurrentOnly(value: Boolean) {
        if (_currentOnly.value == value) return
        _currentOnly.value = value
        _constructorA.value = null
        _constructorB.value = null
        resetComparison()
    }

    fun onSeasonPicked(year: String) {
        _pickedSeason.value = year
        resetComparison()
    }

    fun setConstructorA(constructor: ConstructorModel) {
        _constructorA.value = constructor
        resetComparison()
    }

    fun setConstructorB(constructor: ConstructorModel) {
        _constructorB.value = constructor
        resetComparison()
    }

    suspend fun loadSeasonYears(): Result<List<String>> = repository.getSeasonYears()

    suspend fun loadConstructorsForPicker(): Result<List<ConstructorModel>> =
        if (_currentOnly.value) {
            repository.getCurrentConstructorsList()
        } else {
            repository.getAllConstructors()
        }

    fun compare() {
        if (!canCompare) return
        val a = _constructorA.value ?: return
        val b = _constructorB.value ?: return
        val season = selectedSeason
        loadJob.launch(viewModelScope) {
            _comparison.value = AsyncValue.Loading
            coroutineScope {
                val statsADeferred = async { repository.getConstructorH2hStats(a.constructorId, season) }
                val statsBDeferred = async { repository.getConstructorH2hStats(b.constructorId, season) }
                val statsA = statsADeferred.await()
                val statsB = statsBDeferred.await()
                val leftEx = statsA.exceptionOrNull() as? AppException
                if (leftEx != null) {
                    _comparison.value = AsyncValue.Error(leftEx.title, leftEx.subtitle)
                    return@coroutineScope
                }
                val rightEx = statsB.exceptionOrNull() as? AppException
                if (rightEx != null) {
                    _comparison.value = AsyncValue.Error(rightEx.title, rightEx.subtitle)
                    return@coroutineScope
                }
                _comparison.value = AsyncValue.Value(
                    H2hConstructorCompareResult(a, b, statsA.getOrThrow(), statsB.getOrThrow(), season),
                )
            }
        }
    }

    private fun resetComparison() {
        _comparison.value = AsyncValue.Value(null)
    }
}

@HiltViewModel
class FinishStatusViewModel @Inject constructor(
    private val repository: F1Repository,
) : ViewModel() {
    private val loadJob = LoadJobHolder()

    private val _year = MutableStateFlow("")
    val year: StateFlow<String> = _year.asStateFlow()

    private val _statuses = MutableStateFlow<AsyncValue<List<FinishStatusItem>>>(AsyncValue.Loading)
    val statuses: StateFlow<AsyncValue<List<FinishStatusItem>>> = _statuses.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getSeasonYears().onSuccess { years ->
                if (_year.value.isEmpty() && years.isNotEmpty()) {
                    _year.value = years.first()
                    loadAllData()
                }
            }
        }
    }

    fun onYearChanged(value: String) {
        _year.value = value
        if (value.length == 4) loadAllData()
    }

    suspend fun loadSeasonYears(): Result<List<String>> = repository.getSeasonYears()

    fun loadAllData() {
        if (_year.value.length != 4) return
        loadJob.launch(viewModelScope) {
            _statuses.value = AsyncValue.Loading
            repository.getSeasonFinishStatuses(_year.value).fold(
                onSuccess = { _statuses.value = AsyncValue.Value(it) },
                onFailure = { e ->
                    val ex = e as AppException
                    _statuses.value = AsyncValue.Error(ex.title, ex.subtitle)
                },
            )
        }
    }
}
