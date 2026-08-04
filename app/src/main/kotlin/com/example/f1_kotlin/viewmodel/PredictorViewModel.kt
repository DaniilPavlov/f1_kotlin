package com.example.f1_kotlin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1_kotlin.data.repository.IAuthRepository
import com.example.f1_kotlin.data.repository.IF1Repository
import com.example.f1_kotlin.data.repository.IPredictorLeaderboardRepository
import com.example.f1_kotlin.data.repository.IPredictorRepository
import com.example.f1_kotlin.domain.AppDataRefresh
import com.example.f1_kotlin.domain.AppError
import com.example.f1_kotlin.domain.model.Constructor
import com.example.f1_kotlin.domain.model.Driver
import com.example.f1_kotlin.domain.model.Race
import com.example.f1_kotlin.domain.predictor.PredictorGridKind
import com.example.f1_kotlin.domain.predictor.PredictorLock
import com.example.f1_kotlin.domain.predictor.PredictorOrder
import com.example.f1_kotlin.domain.predictor.PredictorScoringCoordinator
import com.example.f1_kotlin.domain.predictor.PredictorSeason
import com.example.f1_kotlin.domain.predictor.PredictorSeasonSummary
import com.example.f1_kotlin.domain.predictor.PredictorStore
import com.example.f1_kotlin.domain.predictor.PredictorWeekendPrediction
import com.example.f1_kotlin.domain.toAppError
import com.example.f1_kotlin.util.AppLogger
import com.example.f1_kotlin.util.CountdownParts
import com.example.f1_kotlin.util.RaceDateTimeHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZonedDateTime

/** UI-состояние вкладки Predictor (расписание, драфт, store, тикер `now`). */
data class PredictorUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: AppError? = null,
    val races: List<Race> = emptyList(),
    val driversById: Map<String, Driver> = emptyMap(),
    val constructorsByDriverId: Map<String, Constructor> = emptyMap(),
    val championshipDriverOrder: List<String> = emptyList(),
    val store: PredictorStore = PredictorStore.empty(),
    val selectedGrid: PredictorGridKind = PredictorGridKind.Qualifying,
    val draftQualifyingOrder: List<String> = emptyList(),
    val draftRaceOrder: List<String> = emptyList(),
    val now: ZonedDateTime = ZonedDateTime.now(),
)

/**
 * Главный Predictor: драфт сеток, lock/countdown, scoring, синк лидерборда.
 */
@HiltViewModel
class PredictorViewModel @Inject constructor(
    private val authRepository: IAuthRepository,
    private val f1Repository: IF1Repository,
    private val predictorRepository: IPredictorRepository,
    private val leaderboardRepository: IPredictorLeaderboardRepository,
    private val scoringCoordinator: PredictorScoringCoordinator,
    private val appDataRefresh: AppDataRefresh,
) : ViewModel() {
    private val loadJob = LoadJobHolder()
    private var tickerJob: Job? = null
    private var boundDraftKey: String? = null

    val canUsePredictor: StateFlow<Boolean> = authRepository.userChanges
        .map { authRepository.canUsePredictor }
        .stateIn(viewModelScope, SharingStarted.Eagerly, authRepository.canUsePredictor)

    val isSignedIn: StateFlow<Boolean> = authRepository.userChanges
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, authRepository.isSignedIn)

    private val _uiState = MutableStateFlow(PredictorUiState())
    val uiState: StateFlow<PredictorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            canUsePredictor.collect { allowed ->
                if (allowed) {
                    load()
                } else {
                    stopTicker()
                    _uiState.value = PredictorUiState(isLoading = false)
                    boundDraftKey = null
                }
            }
        }
    }

    fun load() {
        if (!authRepository.canUsePredictor) return
        loadJob.launch(viewModelScope) {
            loadInternal(clearCaches = false)
        }
    }

    fun refreshAll() {
        if (!authRepository.canUsePredictor) return
        loadJob.launch(viewModelScope) {
            loadInternal(clearCaches = true)
        }
    }

    fun selectGrid(kind: PredictorGridKind) {
        _uiState.update { it.copy(selectedGrid = kind) }
    }

    /** DnD активной сетки; после lock не пишет. */
    fun reorderDraft(oldIndex: Int, newIndex: Int) {
        val state = _uiState.value
        if (computeIsLocked(state) || oldIndex == newIndex) return
        val list = activeDraft(state).toMutableList()
        if (oldIndex !in list.indices) return
        val item = list.removeAt(oldIndex)
        val insertAt = newIndex.coerceIn(0, list.size)
        list.add(insertAt, item)
        applyDraft(list)
        viewModelScope.launch { persistDraft() }
    }

    fun moveDraftTo(fromIndex: Int, toIndex: Int) {
        val state = _uiState.value
        if (computeIsLocked(state) || fromIndex == toIndex) return
        val list = activeDraft(state).toMutableList()
        if (fromIndex !in list.indices) return
        val target = toIndex.coerceIn(0, list.lastIndex)
        if (fromIndex == target) return
        val tmp = list[fromIndex]
        list[fromIndex] = list[target]
        list[target] = tmp
        applyDraft(list)
        viewModelScope.launch { persistDraft() }
    }

    /** Копирует quali-драфт в race до lock. */
    fun copyQualifyingToRace() {
        val state = _uiState.value
        if (computeIsLocked(state) || state.draftQualifyingOrder.isEmpty()) return
        _uiState.update { it.copy(draftRaceOrder = it.draftQualifyingOrder) }
        viewModelScope.launch { persistDraft() }
    }

    fun seasonByYear(year: String): PredictorSeason? = _uiState.value.store.season(year)

    fun upcomingRace(state: PredictorUiState = _uiState.value): Race? {
        val upcoming = state.races
            .filter { RaceDateTimeHelper.isUpcoming(it, state.now) }
            .sortedBy { RaceDateTimeHelper.raceLocal(it) }
        return upcoming.firstOrNull()
    }

    fun seasonYear(state: PredictorUiState = _uiState.value): String? =
        upcomingRace(state)?.season ?: state.races.firstOrNull()?.season

    fun seasonTotalPoints(state: PredictorUiState = _uiState.value): Int {
        val year = seasonYear(state) ?: return 0
        return state.store.season(year)?.totalPoints ?: 0
    }

    fun lockAt(state: PredictorUiState = _uiState.value): ZonedDateTime? {
        val race = upcomingRace(state) ?: return null
        return PredictorLock.lockAt(race)
    }

    /** Lock относительно `now` в state (тикер). */
    fun computeIsLocked(state: PredictorUiState = _uiState.value): Boolean {
        val race = upcomingRace(state) ?: return true
        return PredictorLock.isLocked(race, state.now)
    }

    fun missingQualifyingTime(state: PredictorUiState = _uiState.value): Boolean {
        val race = upcomingRace(state)
        return race != null && race.qualifying == null
    }

    /** Countdown до lock относительно `now` в state. */
    fun lockCountdown(state: PredictorUiState = _uiState.value): CountdownParts {
        val at = lockAt(state) ?: return CountdownParts.ZERO
        return CountdownParts.until(at, state.now)
    }

    fun currentPrediction(state: PredictorUiState = _uiState.value): PredictorWeekendPrediction? {
        val race = upcomingRace(state) ?: return null
        val year = seasonYear(state) ?: return null
        return state.store.weekend(year, race.round)
    }

    fun historyWeekends(state: PredictorUiState = _uiState.value): List<PredictorWeekendPrediction> {
        val year = seasonYear(state) ?: return emptyList()
        val season = state.store.season(year) ?: return emptyList()
        val upcomingRound = upcomingRace(state)?.round
        return season.weekendsSorted.filter { it.round != upcomingRound }.asReversed()
    }

    /** Прошлые сезоны store кроме текущего года расписания. */
    fun archivedSeasonSummaries(state: PredictorUiState = _uiState.value): List<PredictorSeasonSummary> {
        val current = seasonYear(state)
        return state.store.seasons.values
            .filter { it.year != current && it.weekends.isNotEmpty() }
            .map { PredictorSeasonSummary.fromSeason(it) }
            .sortedByDescending { it.year }
    }

    fun activeDraft(state: PredictorUiState = _uiState.value): List<String> =
        if (state.selectedGrid == PredictorGridKind.Qualifying) {
            state.draftQualifyingOrder
        } else {
            state.draftRaceOrder
        }

    private fun applyDraft(list: List<String>) {
        _uiState.update { state ->
            if (state.selectedGrid == PredictorGridKind.Qualifying) {
                state.copy(draftQualifyingOrder = list)
            } else {
                state.copy(draftRaceOrder = list)
            }
        }
    }

    private suspend fun loadInternal(clearCaches: Boolean) {
        _uiState.update {
            it.copy(
                isLoading = it.races.isEmpty(),
                isRefreshing = it.races.isNotEmpty() || clearCaches,
                error = null,
            )
        }
        try {
            if (clearCaches) appDataRefresh.clearAll()

            // Firestore rules читают claim email_verified из ID token.
            authRepository.refreshIdToken()

            val scheduleResult = f1Repository.getCurrentSchedule()
            val driversResult = f1Repository.getCurrentDrivers()
            val schedule = scheduleResult.getOrElse { throw it }
            val drivers = driversResult.getOrElse { throw it }
                .filter { PredictorOrder.hasUsableDriverCode(it) }
            val driversById = drivers.associateBy { it.driverId }
            val rosterIds = drivers.map { it.driverId }

            var constructorsByDriver = emptyMap<String, Constructor>()
            var championshipOrder = emptyList<String>()
            f1Repository.getCurrentDriverStandings().getOrNull()?.let { (standings, _) ->
                val sorted = standings.sortedBy { it.position.toIntOrNull() ?: 999 }
                constructorsByDriver = sorted.mapNotNull { row ->
                    row.constructors.firstOrNull()?.let { row.driver.driverId to it }
                }.toMap()
                championshipOrder = sorted.map { it.driver.driverId }
            }

            // Сначала показываем расписание/ростер — Firestore не должен держать спиннер вечно.
            _uiState.update {
                it.copy(
                    races = schedule,
                    driversById = driversById,
                    constructorsByDriverId = constructorsByDriver,
                    championshipDriverOrder = championshipOrder,
                    now = ZonedDateTime.now(),
                    isLoading = false,
                    isRefreshing = true,
                )
            }

            val store = try {
                predictorRepository.load()
            } catch (e: Exception) {
                AppLogger.e(TAG, "predictor store load failed", e)
                _uiState.update {
                    it.copy(
                        store = PredictorStore.empty(),
                        error = e.toAppError(),
                        isRefreshing = false,
                    )
                }
                return
            }

            _uiState.update { it.copy(store = store, error = null) }

            try {
                ensureCurrentDraft(rosterIds)
                scoreAllPending()
                syncLeaderboardPoints()
            } catch (e: Exception) {
                AppLogger.e(TAG, "predictor draft/score failed", e)
                _uiState.update { it.copy(error = e.toAppError()) }
            }

            startTicker()
            _uiState.update { it.copy(isRefreshing = false) }
        } catch (e: Exception) {
            AppLogger.e(TAG, "predictor load failed", e)
            stopTicker()
            _uiState.update {
                it.copy(isLoading = false, isRefreshing = false, error = e.toAppError())
            }
        }
    }

    private suspend fun ensureCurrentDraft(rosterIdsOverride: List<String>? = null) {
        val state = _uiState.value
        val race = upcomingRace(state)
        val rosterIds = rosterIdsOverride
            ?: state.driversById.values.map { it.driverId }
        if (race == null || rosterIds.isEmpty()) {
            _uiState.update {
                it.copy(draftQualifyingOrder = emptyList(), draftRaceOrder = emptyList())
            }
            boundDraftKey = null
            return
        }

        val year = race.season
        val existing = state.store.weekend(year, race.round)
        boundDraftKey = "${race.season}_${race.round}"
        val championship = state.championshipDriverOrder

        if (existing == null) {
            val initial = PredictorOrder.defaultPredictorOrder(rosterIds, championship)
            _uiState.update {
                it.copy(draftQualifyingOrder = initial, draftRaceOrder = initial)
            }
            persistDraft(raceName = race.raceName, round = race.round, year = year)
            return
        }

        if (PredictorLock.isLocked(race, state.now)) {
            _uiState.update {
                it.copy(
                    draftQualifyingOrder = existing.qualifyingOrder,
                    draftRaceOrder = existing.raceOrder,
                )
            }
            if (existing.lockedAt == null) {
                val locked = existing.copy(
                    lockedAt = PredictorLock.lockAt(race)?.toInstant() ?: Instant.now(),
                )
                runCatching {
                    val next = predictorRepository.saveWeekend(year, locked)
                    _uiState.update { it.copy(store = next) }
                }.onFailure { AppLogger.e(TAG, "stamp lockedAt failed", it) }
            }
            return
        }

        val q = PredictorOrder.syncOrderToRoster(existing.qualifyingOrder, rosterIds)
        val r = PredictorOrder.syncOrderToRoster(existing.raceOrder, rosterIds)
        _uiState.update { it.copy(draftQualifyingOrder = q, draftRaceOrder = r) }
        persistDraft(raceName = race.raceName, round = race.round, year = year)
    }

    private suspend fun persistDraft(
        raceName: String? = null,
        round: String? = null,
        year: String? = null,
    ) {
        val state = _uiState.value
        val race = upcomingRace(state)
        val y = year ?: race?.season
        val r = round ?: race?.round
        val name = raceName ?: race?.raceName.orEmpty()
        if (y == null || r == null) return
        if (state.draftQualifyingOrder.isEmpty() || state.draftRaceOrder.isEmpty()) return

        val previous = state.store.weekend(y, r)
        val weekend = PredictorWeekendPrediction(
            round = r,
            raceName = name,
            qualifyingOrder = state.draftQualifyingOrder,
            raceOrder = state.draftRaceOrder,
            lockedAt = previous?.lockedAt,
            qualiPoints = previous?.qualiPoints,
            racePoints = previous?.racePoints,
            scoredAt = previous?.scoredAt,
            actualQualifyingOrder = previous?.actualQualifyingOrder,
            actualRaceOrder = previous?.actualRaceOrder,
        )
        try {
            val next = predictorRepository.saveWeekend(y, weekend)
            _uiState.update { it.copy(store = next) }
        } catch (e: Exception) {
            AppLogger.e(TAG, "persistDraft failed", e)
            // Держим драфт в памяти даже если Firestore недоступен.
            _uiState.update {
                it.copy(
                    store = it.store.upsertWeekend(y, weekend),
                    error = e.toAppError(),
                )
            }
        }
    }

    private suspend fun scoreAllPending() {
        val state = _uiState.value
        val year = seasonYear(state) ?: return
        val nextStore = scoringCoordinator.scoreAllPending(state.store, year) ?: return
        val replaced = predictorRepository.replace(nextStore)
        _uiState.update { it.copy(store = replaced) }
        syncLeaderboardPoints()
        val race = upcomingRace(_uiState.value)
        val current = race?.let { replaced.weekend(year, it.round) }
        if (current != null && computeIsLocked(_uiState.value)) {
            _uiState.update {
                it.copy(
                    draftQualifyingOrder = current.qualifyingOrder,
                    draftRaceOrder = current.raceOrder,
                )
            }
        }
    }

    private suspend fun syncLeaderboardPoints() {
        val state = _uiState.value
        val year = seasonYear(state) ?: return
        runCatching {
            leaderboardRepository.syncPoints(year, seasonTotalPoints(state))
        }.onFailure { AppLogger.w(TAG, "syncLeaderboardPoints failed", it) }
    }

    private fun startTicker() {
        stopTicker()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                tickNow()
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private suspend fun tickNow() {
        val before = _uiState.value
        val raceBefore = upcomingRace(before)
        val wasLocked = raceBefore == null || PredictorLock.isLocked(raceBefore, before.now)
        _uiState.update { it.copy(now = ZonedDateTime.now()) }
        val after = _uiState.value
        val race = upcomingRace(after)
        val nextKey = race?.let { "${it.season}_${it.round}" }
        if (nextKey != boundDraftKey) {
            runCatching { ensureCurrentDraft() }
                .onFailure { AppLogger.e(TAG, "ensureCurrentDraft on tick failed", it) }
        }
        if (race != null && !wasLocked && PredictorLock.isLocked(race, after.now)) {
            runCatching { onBecameLocked(race) }
                .onFailure { AppLogger.e(TAG, "onBecameLocked failed", it) }
        }
    }

    private suspend fun onBecameLocked(race: Race) {
        val state = _uiState.value
        val existing = state.store.weekend(race.season, race.round) ?: return
        val locked = existing.copy(
            lockedAt = PredictorLock.lockAt(race)?.toInstant() ?: Instant.now(),
            qualifyingOrder = state.draftQualifyingOrder,
            raceOrder = state.draftRaceOrder,
        )
        val next = predictorRepository.saveWeekend(race.season, locked)
        _uiState.update { it.copy(store = next) }
    }

    override fun onCleared() {
        stopTicker()
        super.onCleared()
    }

    companion object {
        private const val TAG = "PredictorViewModel"
    }
}
