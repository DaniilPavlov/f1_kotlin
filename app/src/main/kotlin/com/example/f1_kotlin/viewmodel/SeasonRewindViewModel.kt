package com.example.f1_kotlin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1_kotlin.data.analytics.AnalyticsEvent
import com.example.f1_kotlin.data.analytics.AnalyticsGateway
import com.example.f1_kotlin.data.repository.IF1Repository
import com.example.f1_kotlin.domain.AppError
import com.example.f1_kotlin.domain.AsyncValue
import com.example.f1_kotlin.domain.ErrorStrings
import com.example.f1_kotlin.domain.model.ConstructorStanding
import com.example.f1_kotlin.domain.model.DriverStanding
import com.example.f1_kotlin.domain.model.Race
import com.example.f1_kotlin.domain.toAppError
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Полоса racing-chart (очки + rank, в т.ч. дробный при lerp). */
data class SeasonRewindBarEntry(
    val id: String,
    val constructorId: String,
    val label: String,
    val tag: String,
    val points: Double,
    /** 0-based позиция; дробная во время lerp-анимации. */
    val rank: Float,
)

data class SeasonRewindUiState(
    val year: String = "",
    val races: AsyncValue<List<Race>> = AsyncValue.Loading,
    val selectedRoundIndex: Int = 0,
    val isPlaying: Boolean = false,
    val chartLoading: Boolean = false,
    val chartLoadFailed: Boolean = false,
    /** Реальная ошибка сети/парсинга — для subtitle в ErrorBody. */
    val chartError: AppError? = null,
    val activeTable: Int = 0,
    val driverBars: List<SeasonRewindBarEntry> = emptyList(),
    val constructorBars: List<SeasonRewindBarEntry> = emptyList(),
    val chartRound: String? = null,
) {
    val selectedRace: Race?
        get() {
            val list = (races as? AsyncValue.Value)?.value ?: return null
            return list.getOrNull(selectedRoundIndex)
        }

    val canPlay: Boolean
        get() = ((races as? AsyncValue.Value)?.value?.size ?: 0) > 1

    val hasChartData: Boolean
        get() = driverBars.isNotEmpty() && constructorBars.isNotEmpty()

    /** Очки на экране не от [selectedRace] — chart устарел. */
    val isChartStale: Boolean
        get() {
            val race = selectedRace ?: return true
            return chartRound != race.round
        }

    val activeBars: List<SeasonRewindBarEntry>
        get() = if (activeTable == 0) driverBars else constructorBars
}

/**
 * Rewind сезона: scrubber раундов, playback и standings→bar chart.
 */
@HiltViewModel
class SeasonRewindViewModel @Inject constructor(
    private val repository: IF1Repository,
    private val analytics: AnalyticsGateway,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SeasonRewindUiState())
    val uiState: StateFlow<SeasonRewindUiState> = _uiState.asStateFlow()

    private var playJob: Job? = null
    private var standingsJob: Job? = null
    private var standingsRequestId = 0

    init {
        analytics.log(AnalyticsEvent.SeasonRewindOpened)
        viewModelScope.launch {
            val year = repository.getSeasonYears().getOrNull()?.firstOrNull().orEmpty()
            _uiState.update { it.copy(year = year.ifEmpty { "2026" }) }
            loadSeason()
        }
    }

    suspend fun loadSeasonYears(): Result<List<String>> = repository.getSeasonYears()

    fun onSeasonChanged(year: String) {
        stopPlayback()
        _uiState.update { it.copy(year = year) }
        loadSeason()
    }

    fun refreshAll() {
        stopPlayback()
        loadSeason()
    }

    fun onTableChanged(index: Int) {
        _uiState.update { it.copy(activeTable = index.coerceIn(0, 1)) }
    }

    fun selectRound(index: Int) {
        val races = (_uiState.value.races as? AsyncValue.Value)?.value ?: return
        if (index !in races.indices) return
        _uiState.update { it.copy(selectedRoundIndex = index) }
        loadStandingsForSelected()
    }

    fun togglePlayback() {
        if (_uiState.value.isPlaying) stopPlayback() else startPlayback()
    }

    fun stopPlayback() {
        playJob?.cancel()
        playJob = null
        _uiState.update { it.copy(isPlaying = false) }
    }

    private fun startPlayback() {
        val races = (_uiState.value.races as? AsyncValue.Value)?.value ?: return
        if (races.size < 2) return

        stopPlayback()
        if (_uiState.value.selectedRoundIndex >= races.lastIndex) {
            _uiState.update { it.copy(selectedRoundIndex = 0) }
            loadStandingsForSelected()
        }

        _uiState.update { it.copy(isPlaying = true) }
        playJob = viewModelScope.launch {
            while (isActive) {
                delay(PLAY_INTERVAL_MS)
                val state = _uiState.value
                val list = (state.races as? AsyncValue.Value)?.value
                val next = state.selectedRoundIndex + 1
                if (list == null || next >= list.size) {
                    stopPlayback()
                    return@launch
                }
                _uiState.update { it.copy(selectedRoundIndex = next) }
                loadStandingsForSelected()
            }
        }
    }

    private fun loadSeason() {
        val year = _uiState.value.year
        if (year.length != 4) {
            _uiState.update { it.copy(races = AsyncValue.Error("Invalid year", year)) }
            return
        }
        standingsJob?.cancel()
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    races = AsyncValue.Loading,
                    selectedRoundIndex = 0,
                    driverBars = emptyList(),
                    constructorBars = emptyList(),
                    chartRound = null,
                    chartLoadFailed = false,
                    chartError = null,
                )
            }
            repository.getSeasonRaces(year)
                .onSuccess { all ->
                    val completed = completedRaces(all)
                    val startIndex = if (completed.isEmpty()) 0 else completed.lastIndex
                    _uiState.update {
                        it.copy(
                            races = AsyncValue.Value(completed),
                            selectedRoundIndex = startIndex,
                        )
                    }
                    if (completed.isNotEmpty()) {
                        loadStandingsForSelected()
                    }
                }
                .onFailure { e ->
                    val err = e.toAppError()
                    _uiState.update {
                        it.copy(races = AsyncValue.Error(err.title, err.subtitle))
                    }
                }
        }
    }

    private fun loadStandingsForSelected() {
        val race = _uiState.value.selectedRace ?: return
        val requestId = ++standingsRequestId
        // Отменяем предыдущий HTTP — иначе параллельный scrub → 429 на первом раунде.
        standingsJob?.cancel()
        _uiState.update {
            it.copy(chartLoading = true, chartLoadFailed = false, chartError = null)
        }
        standingsJob = viewModelScope.launch {
            repository.getStandingsAfterRound(race.season, race.round)
                .onSuccess { (drivers, constructors) ->
                    if (requestId != standingsRequestId) return@onSuccess
                    if (drivers.isEmpty() || constructors.isEmpty()) {
                        _uiState.update {
                            it.copy(
                                chartLoading = false,
                                chartLoadFailed = true,
                                chartError = AppError(
                                    title = ErrorStrings.responseParseError,
                                    subtitle = ErrorStrings.errorRetrySubtitle,
                                ),
                            )
                        }
                        return@onSuccess
                    }
                    _uiState.update {
                        it.copy(
                            chartLoading = false,
                            chartLoadFailed = false,
                            chartError = null,
                            chartRound = race.round,
                            driverBars = drivers.toDriverBars(),
                            constructorBars = constructors.toConstructorBars(),
                        )
                    }
                }
                .onFailure { e ->
                    if (requestId != standingsRequestId) return@onFailure
                    // Отмена предыдущего scrub — не ошибка UI.
                    if (e is kotlinx.coroutines.CancellationException) return@onFailure
                    val err = e.toAppError()
                    _uiState.update {
                        it.copy(
                            chartLoading = false,
                            chartLoadFailed = true,
                            chartError = err,
                        )
                    }
                }
        }
    }

    private fun completedRaces(races: List<Race>): List<Race> {
        val today = LocalDate.now()
        return races.filter { race ->
            runCatching { LocalDate.parse(race.date) }.getOrNull()?.let { !it.isAfter(today) } == true
        }.sortedBy { it.round.toIntOrNull() ?: 0 }
    }

    private fun List<DriverStanding>.toDriverBars(): List<SeasonRewindBarEntry> =
        sortedWith(
            compareByDescending<DriverStanding> { it.points.toDoubleOrNull() ?: 0.0 }
                .thenBy { it.position.toIntOrNull() ?: 99 },
        ).mapIndexed { index, s ->
            SeasonRewindBarEntry(
                id = s.driver.driverId,
                constructorId = s.constructors.firstOrNull()?.constructorId
                    ?: s.driver.driverId,
                label = s.driver.familyName,
                tag = s.driver.code.orEmpty(),
                points = s.points.toDoubleOrNull() ?: 0.0,
                rank = index.toFloat(),
            )
        }

    private fun List<ConstructorStanding>.toConstructorBars(): List<SeasonRewindBarEntry> =
        sortedWith(
            compareByDescending<ConstructorStanding> { it.points.toDoubleOrNull() ?: 0.0 }
                .thenBy { it.position.toIntOrNull() ?: 99 },
        ).mapIndexed { index, s ->
            SeasonRewindBarEntry(
                id = s.constructor.constructorId,
                constructorId = s.constructor.constructorId,
                label = s.constructor.name,
                tag = "",
                points = s.points.toDoubleOrNull() ?: 0.0,
                rank = index.toFloat(),
            )
        }

    override fun onCleared() {
        stopPlayback()
        super.onCleared()
    }

    private companion object {
        const val PLAY_INTERVAL_MS = 1500L
    }
}
