package com.example.f1_kotlin.data.repository

import com.example.f1_kotlin.data.model.EspnDriverCardData
import com.example.f1_kotlin.data.model.EspnScoreboardEvent
import com.example.f1_kotlin.data.model.NewsArticle

/** Контракт ESPN: новости/scoreboard/карточки с TTL in-memory. */
interface IEspnRepository {
    val peekNews: List<NewsArticle>?
    val isNewsFresh: Boolean
    val peekScoreboard: EspnScoreboardEvent?
    val isScoreboardFresh: Boolean

    suspend fun getNews(forceRefresh: Boolean = false): Result<List<NewsArticle>>

    suspend fun getScoreboardEvent(forceRefresh: Boolean = false): Result<EspnScoreboardEvent?>

    suspend fun driverCardData(givenName: String, familyName: String): EspnDriverCardData

    suspend fun constructorNews(constructorId: String, constructorName: String): List<NewsArticle>

    fun clearCaches()
}
