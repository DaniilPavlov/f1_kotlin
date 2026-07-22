package com.example.f1_kotlin.data.model

import com.squareup.moshi.JsonClass
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

// region Domain

/** ESPN F1 news article. */
data class NewsArticle(
    val id: Int,
    val headline: String,
    val description: String,
    val webUrl: String,
    val byline: String? = null,
    val published: LocalDateTime? = null,
    val imageUrl: String? = null,
)

/** Photo + news for a driver detail card. */
data class EspnDriverCardData(
    val photoUrl: String? = null,
    val news: List<NewsArticle> = emptyList(),
)

/** Current F1 weekend from ESPN scoreboard. */
data class EspnScoreboardEvent(
    val name: String,
    val shortName: String,
    val statusState: String,
    val statusDetail: String,
    val startDate: LocalDateTime? = null,
    val endDate: LocalDateTime? = null,
    val circuitName: String? = null,
    val circuitCity: String? = null,
    val circuitCountry: String? = null,
    val sessions: List<EspnScoreboardSession> = emptyList(),
) {
    val isLive: Boolean
        get() = statusState == "in" || sessions.any { it.isLive }

    /** Live → nearest upcoming → last completed. */
    val highlightedSession: EspnScoreboardSession?
        get() {
            sessions.firstOrNull { it.isLive }?.let { return it }
            sessions.firstOrNull { it.isUpcoming }?.let { return it }
            return sessions.lastOrNull()
        }
}

/** Weekend session (FP / Qual / Race / Sprint). */
data class EspnScoreboardSession(
    val abbreviation: String,
    val statusState: String,
    val statusDetail: String,
    val date: LocalDateTime? = null,
    val leaderName: String? = null,
    val isWinner: Boolean = false,
    val results: List<EspnScoreboardResultEntry> = emptyList(),
) {
    val isLive: Boolean get() = statusState == "in"
    val isUpcoming: Boolean get() = statusState == "pre"
    val hasResults: Boolean get() = results.isNotEmpty()
}

/** Session result row (position + driver). */
data class EspnScoreboardResultEntry(
    val position: Int,
    val displayName: String,
    val country: String? = null,
    val isWinner: Boolean = false,
)

// endregion

// region Moshi DTOs

@JsonClass(generateAdapter = true)
data class EspnNewsResponseDto(
    val articles: List<EspnArticleDto>? = null,
)

@JsonClass(generateAdapter = true)
data class EspnArticleDto(
    val id: Int? = null,
    val headline: String? = null,
    val description: String? = null,
    val byline: String? = null,
    val published: String? = null,
    val lastModified: String? = null,
    val images: List<EspnImageDto>? = null,
    val links: EspnLinksDto? = null,
    val categories: List<EspnCategoryDto>? = null,
)

@JsonClass(generateAdapter = true)
data class EspnCategoryDto(
    val type: String? = null,
    val description: String? = null,
)

@JsonClass(generateAdapter = true)
data class EspnSearchResponseDto(
    val items: List<EspnSearchItemDto>? = null,
)

@JsonClass(generateAdapter = true)
data class EspnSearchItemDto(
    val id: String? = null,
    val displayName: String? = null,
    val sport: String? = null,
    val league: String? = null,
)

@JsonClass(generateAdapter = true)
data class EspnAthleteResponseDto(
    val headshot: EspnHeadshotDto? = null,
)

@JsonClass(generateAdapter = true)
data class EspnHeadshotDto(
    val href: String? = null,
)

@JsonClass(generateAdapter = true)
data class EspnOverviewResponseDto(
    val news: List<EspnArticleDto>? = null,
)

@JsonClass(generateAdapter = true)
data class EspnImageDto(
    val url: String? = null,
)

@JsonClass(generateAdapter = true)
data class EspnLinksDto(
    val web: EspnHrefDto? = null,
)

@JsonClass(generateAdapter = true)
data class EspnHrefDto(
    val href: String? = null,
)

@JsonClass(generateAdapter = true)
data class EspnScoreboardResponseDto(
    val events: List<EspnEventDto>? = null,
)

@JsonClass(generateAdapter = true)
data class EspnEventDto(
    val name: String? = null,
    val shortName: String? = null,
    val date: String? = null,
    val endDate: String? = null,
    val status: EspnStatusWrapperDto? = null,
    val circuit: EspnCircuitDto? = null,
    val competitions: List<EspnCompetitionDto>? = null,
)

@JsonClass(generateAdapter = true)
data class EspnStatusWrapperDto(
    val type: EspnStatusTypeDto? = null,
)

@JsonClass(generateAdapter = true)
data class EspnStatusTypeDto(
    val state: String? = null,
    val shortDetail: String? = null,
    val detail: String? = null,
    val description: String? = null,
)

@JsonClass(generateAdapter = true)
data class EspnCircuitDto(
    val fullName: String? = null,
    val address: EspnAddressDto? = null,
)

@JsonClass(generateAdapter = true)
data class EspnAddressDto(
    val city: String? = null,
    val country: String? = null,
)

@JsonClass(generateAdapter = true)
data class EspnCompetitionDto(
    val type: EspnCompetitionTypeDto? = null,
    val status: EspnStatusWrapperDto? = null,
    val date: String? = null,
    val startDate: String? = null,
    val competitors: List<EspnCompetitorDto>? = null,
)

@JsonClass(generateAdapter = true)
data class EspnCompetitionTypeDto(
    val abbreviation: String? = null,
)

@JsonClass(generateAdapter = true)
data class EspnCompetitorDto(
    val order: Int? = null,
    val winner: Boolean? = null,
    val athlete: EspnAthleteDto? = null,
)

@JsonClass(generateAdapter = true)
data class EspnAthleteDto(
    val displayName: String? = null,
    val shortName: String? = null,
    val flag: EspnFlagDto? = null,
)

@JsonClass(generateAdapter = true)
data class EspnFlagDto(
    val alt: String? = null,
)

// endregion

// region Mappers

fun EspnArticleDto.toDomain(): NewsArticle? {
    val headline = headline?.trim().orEmpty()
    val webUrl = links?.web?.href?.trim().orEmpty()
    if (headline.isEmpty() || webUrl.isEmpty()) return null
    return NewsArticle(
        id = id ?: 0,
        headline = headline,
        description = description?.trim().orEmpty(),
        webUrl = webUrl,
        byline = byline?.trim()?.takeIf { it.isNotEmpty() },
        published = parseEspnDateTime(published ?: lastModified),
        imageUrl = images?.firstOrNull()?.url?.trim()?.takeIf { it.isNotEmpty() },
    )
}

fun EspnEventDto.toDomain(): EspnScoreboardEvent {
    val statusType = status?.type
    val sessions = competitions.orEmpty().map { it.toDomain() }
    return EspnScoreboardEvent(
        name = name?.trim().orEmpty(),
        shortName = shortName?.trim().orEmpty(),
        statusState = statusType?.state?.trim().orEmpty(),
        statusDetail = statusType.statusDetail(),
        startDate = parseEspnDateTime(date),
        endDate = parseEspnDateTime(endDate),
        circuitName = circuit?.fullName?.trim()?.takeIf { it.isNotEmpty() },
        circuitCity = circuit?.address?.city?.trim()?.takeIf { it.isNotEmpty() },
        circuitCountry = circuit?.address?.country?.trim()?.takeIf { it.isNotEmpty() },
        sessions = sessions,
    )
}

fun EspnCompetitionDto.toDomain(): EspnScoreboardSession {
    val statusType = status?.type
    val results = competitors.toResultEntries()
    val leader = results.firstOrNull()
    val abbr = type?.abbreviation?.trim().orEmpty()
    return EspnScoreboardSession(
        abbreviation = abbr.ifEmpty { "Session" },
        statusState = statusType?.state?.trim().orEmpty(),
        statusDetail = statusType.statusDetail(),
        date = parseEspnDateTime(date ?: startDate),
        leaderName = leader?.displayName,
        isWinner = leader?.isWinner == true,
        results = results,
    )
}

private fun List<EspnCompetitorDto>?.toResultEntries(): List<EspnScoreboardResultEntry> {
    if (this == null) return emptyList()
    val entries = mapNotNull { raw ->
        val athlete = raw.athlete
        val displayName = athlete?.displayName?.trim()?.takeIf { it.isNotEmpty() }
            ?: athlete?.shortName?.trim()?.takeIf { it.isNotEmpty() }
            ?: return@mapNotNull null
        EspnScoreboardResultEntry(
            position = raw.order ?: 0,
            displayName = displayName,
            country = athlete?.flag?.alt?.trim()?.takeIf { it.isNotEmpty() },
            isWinner = raw.winner == true,
        )
    }.toMutableList()
    entries.forEachIndexed { index, entry ->
        if (entry.position == 0) {
            entries[index] = entry.copy(position = index + 1)
        }
    }
    return entries.sortedBy { it.position }
}

private fun EspnStatusTypeDto?.statusDetail(): String {
    val shortDetail = this?.shortDetail?.trim()
    if (!shortDetail.isNullOrEmpty()) return shortDetail
    val detail = this?.detail?.trim()
    if (!detail.isNullOrEmpty()) return detail
    return this?.description?.trim().orEmpty()
}

fun parseEspnDateTime(raw: String?): LocalDateTime? {
    if (raw.isNullOrBlank()) return null
    return try {
        Instant.parse(raw).atZone(ZoneId.systemDefault()).toLocalDateTime()
    } catch (_: Exception) {
        try {
            // ESPN sometimes omits zone; treat as UTC-ish ISO local
            LocalDateTime.parse(raw.removeSuffix("Z").substringBefore('+').substringBefore('.'))
        } catch (_: Exception) {
            null
        }
    }
}

// endregion
