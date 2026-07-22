package com.example.f1_kotlin.data.repository

import android.net.Uri
import com.example.f1_kotlin.data.api.EspnApiService
import com.example.f1_kotlin.data.model.EspnDriverCardData
import com.example.f1_kotlin.data.model.EspnScoreboardEvent
import com.example.f1_kotlin.data.model.NewsArticle
import com.example.f1_kotlin.data.model.toDomain
import com.example.f1_kotlin.domain.ApiCallHandler
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ESPN F1 news + scoreboard + driver/constructor media with in-memory TTL/cache.
 */
@Singleton
class EspnRepository @Inject constructor(
    private val api: EspnApiService,
) {
    private val newsMutex = Mutex()
    private val scoreboardMutex = Mutex()

    @Volatile
    private var newsCache: List<NewsArticle>? = null

    @Volatile
    private var newsCachedAtMs: Long = 0L

    @Volatile
    private var scoreboardCache: EspnScoreboardEvent? = null

    @Volatile
    private var scoreboardCachedAtMs: Long = 0L

    @Volatile
    private var scoreboardHasCache: Boolean = false

    private val driverCardCache = ConcurrentHashMap<String, EspnDriverCardData>()
    private val constructorNewsCache = ConcurrentHashMap<String, List<NewsArticle>>()

    val peekNews: List<NewsArticle>? get() = newsCache

    val isNewsFresh: Boolean
        get() = newsCache != null &&
            System.currentTimeMillis() - newsCachedAtMs < EspnApiService.NEWS_CACHE_TTL_MS

    val peekScoreboard: EspnScoreboardEvent? get() = scoreboardCache

    /** Fresh includes an empty events response (null event). */
    val isScoreboardFresh: Boolean
        get() = scoreboardHasCache &&
            System.currentTimeMillis() - scoreboardCachedAtMs < EspnApiService.SCOREBOARD_CACHE_TTL_MS

    suspend fun getNews(forceRefresh: Boolean = false): Result<List<NewsArticle>> {
        if (!forceRefresh && isNewsFresh) {
            return Result.success(newsCache!!)
        }
        return newsMutex.withLock {
            if (!forceRefresh && isNewsFresh) {
                return@withLock Result.success(newsCache!!)
            }
            ApiCallHandler.safeCall {
                val response = api.getNews()
                val articles = response.articles.orEmpty().mapNotNull { it.toDomain() }
                newsCache = articles
                newsCachedAtMs = System.currentTimeMillis()
                articles
            }
        }
    }

    /**
     * First event from scoreboard, or null if events list is empty.
     * Failures are [Result.failure]; callers on Results hide scoreboard silently.
     */
    suspend fun getScoreboardEvent(forceRefresh: Boolean = false): Result<EspnScoreboardEvent?> {
        if (!forceRefresh && isScoreboardFresh) {
            return Result.success(scoreboardCache)
        }
        return scoreboardMutex.withLock {
            if (!forceRefresh && isScoreboardFresh) {
                return@withLock Result.success(scoreboardCache)
            }
            ApiCallHandler.safeCall {
                val response = api.getScoreboard()
                val events = response.events
                if (events.isNullOrEmpty()) {
                    scoreboardCache = null
                    scoreboardCachedAtMs = System.currentTimeMillis()
                    scoreboardHasCache = true
                    return@safeCall null
                }
                val event = events.first().toDomain()
                scoreboardCache = event
                scoreboardCachedAtMs = System.currentTimeMillis()
                scoreboardHasCache = true
                event
            }
        }
    }

    /** Photo + news for a driver (search → athlete → overview). Errors → empty card. */
    suspend fun driverCardData(givenName: String, familyName: String): EspnDriverCardData {
        val cacheKey = normalize("$givenName|$familyName")
        driverCardCache[cacheKey]?.let { return it }

        val fullName = "$givenName $familyName".trim()
        val espnId = searchF1PlayerId(fullName)
            ?: familyName.trim().takeIf { it.isNotEmpty() }?.let { searchF1PlayerId(it) }

        if (espnId == null) {
            val empty = EspnDriverCardData()
            driverCardCache[cacheKey] = empty
            return empty
        }

        val photoUrl = loadAthletePhoto(espnId)
        val news = loadDriverNews(espnId)
        val data = EspnDriverCardData(photoUrl = photoUrl, news = news)
        driverCardCache[cacheKey] = data
        return data
    }

    /** Team news (up to 5). Errors / empty → []. */
    suspend fun constructorNews(constructorId: String, constructorName: String): List<NewsArticle> {
        val cacheKey = normalize("$constructorId|$constructorName")
        constructorNewsCache[cacheKey]?.let { return it }

        return try {
            val teamId = resolveTeamId(constructorId, constructorName)
            val news = if (teamId != null) {
                loadNewsByTeamId(teamId)
            } else {
                loadNewsByTeamNameFallback(constructorName)
            }
            constructorNewsCache[cacheKey] = news
            news
        } catch (_: Exception) {
            val empty = emptyList<NewsArticle>()
            constructorNewsCache[cacheKey] = empty
            empty
        }
    }

    fun clearCaches() {
        newsCache = null
        newsCachedAtMs = 0L
        scoreboardCache = null
        scoreboardCachedAtMs = 0L
        scoreboardHasCache = false
        driverCardCache.clear()
        constructorNewsCache.clear()
    }

    private suspend fun searchF1PlayerId(query: String): String? {
        if (query.isEmpty()) return null
        return try {
            val response = api.searchPlayers(query = query)
            val items = response.items.orEmpty()
            val normalizedQuery = normalize(query)
            var exact: String? = null
            var fallback: String? = null
            for (raw in items) {
                val sport = raw.sport?.lowercase()
                val league = raw.league?.lowercase()
                if (sport != "racing" || league != "f1") continue
                val id = raw.id ?: continue
                if (fallback == null) fallback = id
                val name = normalize(raw.displayName.orEmpty())
                if (name == normalizedQuery || name.contains(normalizedQuery) || normalizedQuery.contains(name)) {
                    exact = id
                    break
                }
            }
            exact ?: fallback
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun loadAthletePhoto(espnId: String): String? {
        return try {
            val href = api.getAthlete(espnId).headshot?.href ?: return null
            val path = Uri.parse(href).path?.takeIf { it.isNotEmpty() } ?: return null
            "${EspnApiService.DRIVER_PHOTO_BASE}$path&w=1200&h=800"
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun loadDriverNews(espnId: String): List<NewsArticle> {
        return try {
            api.getAthleteOverview(espnId).news.orEmpty().mapNotNull { it.toDomain() }.take(5)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun loadNewsByTeamId(teamId: String): List<NewsArticle> {
        val response = api.getNews(limit = 5, team = teamId)
        return response.articles.orEmpty().mapNotNull { it.toDomain() }.take(5)
    }

    private suspend fun loadNewsByTeamNameFallback(constructorName: String): List<NewsArticle> {
        val needle = normalizeConstructorName(constructorName)
        if (needle.isEmpty()) return emptyList()
        val response = api.getNews()
        val matched = mutableListOf<NewsArticle>()
        for (item in response.articles.orEmpty()) {
            if (!articleMentionsTeam(item.categories, needle)) continue
            val article = item.toDomain() ?: continue
            matched.add(article)
            if (matched.size >= 5) break
        }
        return matched
    }

    private fun articleMentionsTeam(
        categories: List<com.example.f1_kotlin.data.model.EspnCategoryDto>?,
        needle: String,
    ): Boolean {
        if (categories == null) return false
        for (raw in categories) {
            if (raw.type != "team") continue
            val description = normalizeConstructorName(raw.description.orEmpty())
            if (description.isEmpty()) continue
            if (description == needle || needle.contains(description) || description.contains(needle)) {
                return true
            }
        }
        return false
    }

    private fun resolveTeamId(constructorId: String, constructorName: String): String? {
        teamIdsByConstructorId[constructorId.trim().lowercase()]?.let { return it }
        val name = normalizeConstructorName(constructorName)
        return teamIdsByAlias[name] ?: teamIdsByAlias[normalize(constructorName)]
    }

    private fun normalize(value: String): String =
        value.lowercase().replace(Regex("\\s+"), " ").trim()

    private fun normalizeConstructorName(value: String): String {
        var name = normalize(value)
        name = name.replace(Regex("\\bf1 team\\b"), "").replace(Regex("\\bteam\\b"), "")
        return name.replace(Regex("\\s+"), " ").trim()
    }

    companion object {
        private val teamIdsByConstructorId = mapOf(
            "mercedes" to "106893",
            "ferrari" to "106842",
            "mclaren" to "106892",
            "red_bull" to "106921",
            "alpine" to "106922",
            "williams" to "106967",
            "aston_martin" to "123986",
            "rb" to "123988",
            "sauber" to "106925",
            "kick_sauber" to "106925",
            "audi" to "106925",
        )

        private val teamIdsByAlias = mapOf(
            "mercedes" to "106893",
            "mercedes-benz" to "106893",
            "ferrari" to "106842",
            "mclaren" to "106892",
            "red bull" to "106921",
            "alpine" to "106922",
            "alpine f1 team" to "106922",
            "williams" to "106967",
            "aston martin" to "123986",
            "racing bulls" to "123988",
            "rb" to "123988",
            "rb f1 team" to "123988",
            "visa cash app rb" to "123988",
            "sauber" to "106925",
            "kick sauber" to "106925",
            "audi" to "106925",
        )
    }
}
