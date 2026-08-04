package com.example.f1_kotlin.data.repository

import com.example.f1_kotlin.domain.predictor.PredictorLeaderboardEntry
import com.example.f1_kotlin.domain.predictor.PredictorLeaderboardProfile
import com.example.f1_kotlin.domain.predictor.PredictorLeaderboardResult
import com.example.f1_kotlin.domain.predictor.PredictorSeason
import com.example.f1_kotlin.domain.predictor.PredictorStore
import com.example.f1_kotlin.domain.predictor.PredictorWeekendPrediction

interface IPredictorRepository {
    suspend fun load(): PredictorStore
    suspend fun saveWeekend(year: String, weekend: PredictorWeekendPrediction): PredictorStore
    suspend fun replace(store: PredictorStore): PredictorStore
    fun clearMemoryCache()
}

interface IPredictorLeaderboardRepository {
    val currentUid: String?
    suspend fun loadProfile(): PredictorLeaderboardProfile
    suspend fun loadLeaderboard(year: String): List<PredictorLeaderboardEntry>
    suspend fun join(
        nickname: String,
        year: String,
        totalPoints: Int,
    ): PredictorLeaderboardResult
    suspend fun leave(year: String): PredictorLeaderboardResult
    suspend fun updateNickname(
        nickname: String,
        year: String,
    ): PredictorLeaderboardResult
    suspend fun syncPoints(year: String, totalPoints: Int)
    fun clearMemoryCache()
}
