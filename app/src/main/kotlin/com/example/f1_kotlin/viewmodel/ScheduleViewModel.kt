package com.example.f1_kotlin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1_kotlin.data.repository.IF1Repository
import com.example.f1_kotlin.domain.AppDataRefresh
import com.example.f1_kotlin.domain.AppError
import com.example.f1_kotlin.domain.AsyncValue
import com.example.f1_kotlin.domain.model.Race
import com.example.f1_kotlin.domain.model.RaceSession
import com.example.f1_kotlin.util.DateUtils
import com.example.f1_kotlin.util.RaceDateTimeHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * Элемент списка «сессия на выбранный день» для экрана «Календарь».
 * [title] пустой у заголовка-разделителя с названием гонки.
 */
data class ScheduleSessionItem(
    val raceName: String,
    val titleRes: Int?,
    val date: RaceSession,
)

data class ScheduleUiState(
    val races: AsyncValue<List<Race>> = AsyncValue.Loading,
    val selectedDate: LocalDate = LocalDate.now(),
    /** true после первого клика по дню — чтобы «сегодня» оставался красным до тапа. */
    val userPickedDay: Boolean = false,
    val focusedMonth: YearMonth = YearMonth.now(),
    val scheduleItems: List<ScheduleSessionItem> = emptyList(),
    val upcomingRace: Race? = null,
    val error: AppError? = null,
    val isRefreshing: Boolean = false,
)

/**
 * ViewModel вкладки «Календарь».
 *
 * Загружает расписание сезона, строит список сессий на выбранный день
 * и подсказывает иконки для дней в календаре (практика / гонка).
 * [refreshAll] чистит кэши через [AppDataRefresh] и грузит заново (ErrorBody / pull-to-refresh).
 */
@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val repository: IF1Repository,
    private val appDataRefresh: AppDataRefresh,
) : ViewModel() {
    private val loadJob = LoadJobHolder()

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        loadAllData()
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

    private suspend fun loadInternal(clearCaches: Boolean) {
        try {
            if (clearCaches) {
                appDataRefresh.clearAll()
                _uiState.update {
                    it.copy(
                        isRefreshing = true,
                        error = null,
                        races = if (it.races is AsyncValue.Value) it.races else AsyncValue.Loading,
                    )
                }
            } else {
                _uiState.update { it.copy(error = null) }

                repository.peekScheduleCache()?.let {
                    _uiState.update { state -> state.copy(races = AsyncValue.Value(it)) }
                    refreshUpcoming()
                    onSelectDay(LocalDate.now())
                } ?: run {
                    _uiState.update { it.copy(races = AsyncValue.Loading) }
                }
            }

            repository.getCurrentSchedule().applyUnlessCached(
                current = _uiState.value.races,
                onSuccess = { races ->
                    _uiState.update { it.copy(races = AsyncValue.Value(races)) }
                    refreshUpcoming()
                    onSelectDay(LocalDate.now())
                },
                onFailure = { err ->
                    if (_uiState.value.races !is AsyncValue.Value) {
                        _uiState.update {
                            it.copy(
                                races = err.toAsyncError(),
                                error = err,
                            )
                        }
                    }
                },
            )
        } finally {
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun onMonthChanged(month: YearMonth) {
        _uiState.update { it.copy(focusedMonth = month) }
    }

    /** Обновляет [ScheduleUiState.scheduleItems] при клике на день в календаре. */
    fun onSelectDay(date: LocalDate) {
        _uiState.update { it.copy(userPickedDay = true, selectedDate = date) }
        buildScheduleForDate(date)
    }

    /** Иконка под днём: финиш — день гонки, машина — день сессии. */
    fun logoForDay(day: LocalDate): Int? {
        val races = _uiState.value.races.getOrNull() ?: return null
        if (races.any { LocalDate.parse(it.date) == day }) return com.example.f1_kotlin.R.drawable.calendar_finish
        if (races.any { hasSessionOnDay(it, day) }) return com.example.f1_kotlin.R.drawable.calendar_car
        return null
    }

    private fun refreshUpcoming() {
        val races = _uiState.value.races.getOrNull() ?: run {
            _uiState.update { it.copy(upcomingRace = null) }
            return
        }
        val now = ZonedDateTime.now()
        _uiState.update {
            it.copy(
                upcomingRace = races
                    .filter { race -> RaceDateTimeHelper.isUpcoming(race, now) }
                    .minByOrNull { race -> RaceDateTimeHelper.raceLocal(race) },
            )
        }
    }

    private fun hasSessionOnDay(race: Race, day: LocalDate): Boolean =
        sessions(race).any { it?.date?.let { d -> LocalDate.parse(d) == day } == true }

    private fun sessions(race: Race) = listOf(
        race.firstPractice,
        race.secondPractice,
        race.thirdPractice,
        race.sprintQualifying,
        race.sprint,
        race.qualifying,
    )

    private fun buildScheduleForDate(date: LocalDate) {
        val races = _uiState.value.races.getOrNull() ?: return
        val items = mutableListOf<ScheduleSessionItem>()

        for (race in races) {
            val raceDate = LocalDate.parse(race.date)
            if (DateUtils.isSameDay(raceDate, date) || raceDate.isAfter(date)) {
                addSessionsForDay(race, date, items)
                if (DateUtils.isSameDay(raceDate, date)) {
                    items.add(
                        ScheduleSessionItem(
                            raceName = race.raceName,
                            titleRes = com.example.f1_kotlin.R.string.race,
                            date = RaceSession(date = race.date, time = race.time),
                        ),
                    )
                }
                if (items.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            scheduleItems = listOf(
                                ScheduleSessionItem(
                                    raceName = race.raceName,
                                    titleRes = null,
                                    date = RaceSession("", null),
                                ),
                            ) + items,
                        )
                    }
                    return
                }
            }
        }
        _uiState.update { it.copy(scheduleItems = emptyList()) }
    }

    private fun addSessionsForDay(race: Race, day: LocalDate, items: MutableList<ScheduleSessionItem>) {
        val sessionPairs = listOf(
            race.firstPractice to com.example.f1_kotlin.R.string.first_practice,
            race.secondPractice to com.example.f1_kotlin.R.string.second_practice,
            race.thirdPractice to com.example.f1_kotlin.R.string.third_practice,
            race.sprintQualifying to com.example.f1_kotlin.R.string.sprint_qualifying,
            race.sprint to com.example.f1_kotlin.R.string.sprint,
            race.qualifying to com.example.f1_kotlin.R.string.qualifying,
        )
        sessionPairs.forEach { (session, titleRes) ->
            if (session != null && LocalDate.parse(session.date) == day) {
                items.add(ScheduleSessionItem(race.raceName, titleRes, session))
            }
        }
    }
}
