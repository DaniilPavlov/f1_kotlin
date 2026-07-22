package com.example.f1_kotlin.data.api

import com.example.f1_kotlin.data.model.EspnAthleteResponseDto
import com.example.f1_kotlin.data.model.EspnNewsResponseDto
import com.example.f1_kotlin.data.model.EspnOverviewResponseDto
import com.example.f1_kotlin.data.model.EspnScoreboardResponseDto
import com.example.f1_kotlin.data.model.EspnSearchResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * ESPN Site API (F1 news / scoreboard / athlete media).
 *
 * Docs: https://github.com/pseudo-r/Public-ESPN-API/blob/main/docs/sports/racing.md
 * Base URL: https://site.api.espn.com/
 *
 * Absolute @GET URLs are used for search / athlete hosts outside the base.
 */
interface EspnApiService {

    /** F1 news feed. Without [limit] ESPN returns ~6 articles; max is 50. */
    @GET("apis/site/v2/sports/racing/f1/news")
    suspend fun getNews(
        @Query("limit") limit: Int = NEWS_LIMIT,
        @Query("team") team: String? = null,
    ): EspnNewsResponseDto

    /** Current / upcoming F1 race weekend scoreboard. */
    @GET("apis/site/v2/sports/racing/f1/scoreboard")
    suspend fun getScoreboard(): EspnScoreboardResponseDto

    /** Player search (site.web.api). */
    @GET("https://site.web.api.espn.com/apis/common/v3/search")
    suspend fun searchPlayers(
        @Query("region") region: String = "us",
        @Query("lang") lang: String = "en",
        @Query("query") query: String,
        @Query("limit") limit: Int = 10,
        @Query("type") type: String = "player",
    ): EspnSearchResponseDto

    /** Athlete card (core API) — headshot. */
    @GET("https://sports.core.api.espn.com/v2/sports/racing/leagues/f1/athletes/{id}")
    suspend fun getAthlete(@Path("id") espnAthleteId: String): EspnAthleteResponseDto

    /** Athlete overview — news. */
    @GET("https://site.web.api.espn.com/apis/common/v3/sports/racing/f1/athletes/{id}/overview")
    suspend fun getAthleteOverview(
        @Path("id") espnAthleteId: String,
        @Query("region") region: String = "us",
        @Query("lang") lang: String = "en",
    ): EspnOverviewResponseDto

    companion object {
        const val NEWS_LIMIT = 50
        const val BASE_URL = "https://site.api.espn.com/"
        const val NEWS_CACHE_TTL_MS = 15 * 60 * 1000L
        const val SCOREBOARD_CACHE_TTL_MS = 5 * 60 * 1000L
        const val SCOREBOARD_POLL_INTERVAL_MS = 30_000L
        const val DRIVER_PHOTO_BASE = "https://a.espncdn.com/combiner/i?img="
    }
}
