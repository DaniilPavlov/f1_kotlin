package com.example.f1_kotlin.data.repository

import com.example.f1_kotlin.domain.predictor.PredictorLeaderboardEntry
import com.example.f1_kotlin.domain.predictor.PredictorLeaderboardProfile
import com.example.f1_kotlin.domain.predictor.PredictorLeaderboardResult
import com.example.f1_kotlin.domain.predictor.PredictorSeason
import com.example.f1_kotlin.domain.predictor.PredictorStore
import com.example.f1_kotlin.domain.predictor.PredictorWeekendPrediction

/** Контракт store предиктора: Firestore `users/{uid}/seasons` + in-memory кэш. */
interface IPredictorRepository {
    /** Читает все сезоны uid; пустой store, если не залогинен. */
    suspend fun load(): PredictorStore
    /** Upsert одного уикенда в сезон и обновляет memory. */
    suspend fun saveWeekend(year: String, weekend: PredictorWeekendPrediction): PredictorStore
    /** Полная перезапись store после batch-scoring. */
    suspend fun replace(store: PredictorStore): PredictorStore
    fun clearMemoryCache()
}

/** Контракт лидерборда: профиль/никнеймы/`leaderboards/{year}/entries`. */
interface IPredictorLeaderboardRepository {
    val currentUid: String?
    suspend fun loadProfile(): PredictorLeaderboardProfile
    suspend fun loadLeaderboard(year: String): List<PredictorLeaderboardEntry>
    /** Opt-in + захват ника (транзакция) и запись очков сезона. */
    suspend fun join(
        nickname: String,
        year: String,
        totalPoints: Int,
    ): PredictorLeaderboardResult
    /** Opt-out: снять флаг и удалить entry года. */
    suspend fun leave(year: String): PredictorLeaderboardResult
    suspend fun updateNickname(
        nickname: String,
        year: String,
    ): PredictorLeaderboardResult
    /** Тихий апдейт `totalPoints`, только если пользователь в лидерборде. */
    suspend fun syncPoints(year: String, totalPoints: Int)
    fun clearMemoryCache()
}
