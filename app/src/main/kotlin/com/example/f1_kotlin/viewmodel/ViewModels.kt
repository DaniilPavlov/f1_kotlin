package com.example.f1_kotlin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1_kotlin.data.model.CircuitModel
import com.example.f1_kotlin.data.model.RaceDateModel
import com.example.f1_kotlin.data.model.RaceModel
import com.example.f1_kotlin.data.repository.F1Repository
import com.example.f1_kotlin.domain.AppException
import com.example.f1_kotlin.domain.AsyncValue
import com.example.f1_kotlin.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * Элемент списка «сессия на выбранный день» для экрана «Календарь».
 * [title] пустой у заголовка-разделителя с названием гонки.
 */
data class ScheduleSessionItem(
    val raceName: String,
    val title: String,
    val date: RaceDateModel,
)

/**
 * ViewModel вкладки «Календарь».
 *
 * Загружает расписание сезона, строит список сессий на выбранный день
 * и подсказывает иконки для дней в календаре (практика / гонка).
 */
@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val repository: F1Repository,
) : ViewModel() {
    private val loadJob = LoadJobHolder()

    private val _races = MutableStateFlow<AsyncValue<List<RaceModel>>>(AsyncValue.Loading)
    val races: StateFlow<AsyncValue<List<RaceModel>>> = _races.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    /** true после первого клика по дню — чтобы «сегодня» оставался красным до тапа. */
    private val _userPickedDay = MutableStateFlow(false)
    val userPickedDay: StateFlow<Boolean> = _userPickedDay.asStateFlow()

    private val _focusedMonth = MutableStateFlow(YearMonth.now())
    val focusedMonth: StateFlow<YearMonth> = _focusedMonth.asStateFlow()

    private val _scheduleItems = MutableStateFlow<List<ScheduleSessionItem>>(emptyList())
    val scheduleItems: StateFlow<List<ScheduleSessionItem>> = _scheduleItems.asStateFlow()

    private val _allDataLoaded = MutableStateFlow(false)
    val allDataLoaded: StateFlow<Boolean> = _allDataLoaded.asStateFlow()

    private val _error = MutableStateFlow<AppException?>(null)
    val error: StateFlow<AppException?> = _error.asStateFlow()

    init {
        loadAllData()
    }

    fun loadAllData() {
        loadJob.launch(viewModelScope) {
            _error.value = null

            repository.peekScheduleCache()?.let {
                _races.value = AsyncValue.Value(it)
                onSelectDay(LocalDate.now())
                _allDataLoaded.value = true
            } ?: run {
                _races.value = AsyncValue.Loading
                _allDataLoaded.value = false
            }

            repository.getCurrentSchedule().applyUnlessCached(
                current = _races.value,
                onSuccess = { races ->
                    _races.value = AsyncValue.Value(races)
                    onSelectDay(LocalDate.now())
                    _allDataLoaded.value = true
                },
                onFailure = { ex ->
                    if (_races.value !is AsyncValue.Value) {
                        _races.value = AsyncValue.Error(ex.title, ex.subtitle)
                        _error.value = ex
                    }
                    _allDataLoaded.value = _races.value is AsyncValue.Value
                },
            )
        }
    }

    fun onMonthChanged(month: YearMonth) {
        _focusedMonth.value = month
    }

    /** Обновляет [scheduleItems] при клике на день в календаре. */
    fun onSelectDay(date: LocalDate) {
        _userPickedDay.value = true
        _selectedDate.value = date
        buildScheduleForDate(date)
    }

    /** Иконка под днём: финиш — день гонки, машина — день сессии. */
    fun logoForDay(day: LocalDate): Int? {
        val races = _races.value.getOrNull() ?: return null
        if (races.any { LocalDate.parse(it.date) == day }) return com.example.f1_kotlin.R.drawable.calendar_finish
        if (races.any { hasSessionOnDay(it, day) }) return com.example.f1_kotlin.R.drawable.calendar_car
        return null
    }

    private fun hasSessionOnDay(race: RaceModel, day: LocalDate): Boolean =
        sessions(race).any { it?.date?.let { d -> LocalDate.parse(d) == day } == true }

    private fun sessions(race: RaceModel) = listOf(
        race.firstPractice,
        race.secondPractice,
        race.thirdPractice,
        race.qualifying,
        race.sprint,
    )

    private fun buildScheduleForDate(date: LocalDate) {
        val races = _races.value.getOrNull() ?: return
        val items = mutableListOf<ScheduleSessionItem>()

        for (race in races) {
            val raceDate = LocalDate.parse(race.date)
            if (DateUtils.isSameDay(raceDate, date) || raceDate.isAfter(date)) {
                addSessionsForDay(race, date, items)
                if (DateUtils.isSameDay(raceDate, date)) {
                    items.add(
                        ScheduleSessionItem(
                            raceName = race.raceName,
                            title = "Гонка",
                            date = RaceDateModel(date = race.date, time = race.time),
                        ),
                    )
                }
                if (items.isNotEmpty()) {
                    _scheduleItems.value = listOf(
                        ScheduleSessionItem(raceName = race.raceName, title = "", date = RaceDateModel("", null)),
                    ) + items
                    return
                }
            }
        }
        _scheduleItems.value = emptyList()
    }

    private fun addSessionsForDay(race: RaceModel, day: LocalDate, items: MutableList<ScheduleSessionItem>) {
        val sessionPairs = listOf(
            race.firstPractice to "Первая практика",
            race.secondPractice to "Вторая практика",
            race.thirdPractice to "Третья практика",
            race.sprint to "Спринт",
            race.qualifying to "Квалификация",
        )
        sessionPairs.forEach { (session, title) ->
            if (session != null && LocalDate.parse(session.date) == day) {
                items.add(ScheduleSessionItem(race.raceName, title, session))
            }
        }
    }
}

/** ViewModel вкладки «Трассы» — список всех трасс F1 (с offline-кэшем). */
@HiltViewModel
class CircuitsViewModel @Inject constructor(
    private val repository: F1Repository,
) : ViewModel() {
    private val loadJob = LoadJobHolder()

    private val _circuits = MutableStateFlow<AsyncValue<List<CircuitModel>>>(AsyncValue.Loading)
    val circuits: StateFlow<AsyncValue<List<CircuitModel>>> = _circuits.asStateFlow()

    private val _activePage = MutableStateFlow(0)
    val activePage: StateFlow<Int> = _activePage.asStateFlow()

    init {
        loadCircuits()
    }

    fun changeActivePage(index: Int) {
        _activePage.value = index
    }

    fun loadCircuits() {
        loadJob.launch(viewModelScope) {
            repository.peekCircuitsCache()?.let { _circuits.value = AsyncValue.Value(it) }
                ?: run { _circuits.value = AsyncValue.Loading }

            repository.getCircuits().applyUnlessCached(
                current = _circuits.value,
                onSuccess = { _circuits.value = AsyncValue.Value(it) },
                onFailure = { ex -> _circuits.value = AsyncValue.Error(ex.title, ex.subtitle) },
            )
        }
    }
}

/**
 * ViewModel экрана «Поиск гонки».
 *
 * Пользователь вводит год и номер раунда; по кнопке «Найти» — запрос без кэша.
 * Результат — одна гонка или сообщение «не найдено».
 */
@HiltViewModel
class RaceSearchViewModel @Inject constructor(
    private val repository: F1Repository,
) : ViewModel() {
    private val loadJob = LoadJobHolder()

    private val _year = MutableStateFlow("")
    val year: StateFlow<String> = _year.asStateFlow()

    private val _round = MutableStateFlow("")
    val round: StateFlow<String> = _round.asStateFlow()

    private val _searchedRace = MutableStateFlow<AsyncValue<RaceModel?>>(AsyncValue.Value(null))
    val searchedRace: StateFlow<AsyncValue<RaceModel?>> = _searchedRace.asStateFlow()

    private val _fieldsInputted = MutableStateFlow(false)
    val fieldsInputted: StateFlow<Boolean> = _fieldsInputted.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    private val _dataLoaded = MutableStateFlow(true)
    val dataLoaded: StateFlow<Boolean> = _dataLoaded.asStateFlow()

    fun onYearChanged(value: String) {
        _year.value = value.filter { it.isDigit() }.take(4)
        checkFields()
    }

    fun onRoundChanged(value: String) {
        _round.value = value.filter { it.isDigit() }
        checkFields()
    }

    fun checkFields() {
        _fieldsInputted.value = _year.value.length == 4 && _round.value.isNotEmpty()
    }

    fun loadRaceResults() {
        loadJob.launch(viewModelScope) {
            _dataLoaded.value = false
            _errorMessage.value = ""
            _searchedRace.value = AsyncValue.Loading
            repository.getRaceResults(_year.value, _round.value)
                .onSuccess { race ->
                    if (race != null) {
                        _searchedRace.value = AsyncValue.Value(race)
                    } else {
                        _searchedRace.value = AsyncValue.Value(null)
                        _errorMessage.value =
                            "По вашему запросу гонок не найдено. Проверьте введенные данные и попробуйте еще раз."
                    }
                }
                .onFailure { e ->
                    val ex = e as AppException
                    _searchedRace.value = AsyncValue.Error(ex.title, ex.subtitle)
                    _errorMessage.value = ex.title
                }
            _dataLoaded.value = true
        }
    }
}
