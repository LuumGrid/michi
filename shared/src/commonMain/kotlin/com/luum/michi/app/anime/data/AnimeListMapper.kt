package com.luum.michi.app.anime.data

import androidx.compose.ui.graphics.Color
import com.luum.michi.app.anime.presentation.model.AnimeListEntry
import com.luum.michi.app.anime.presentation.model.AnimeListSection
import com.luum.michi.app.core.anilist.dto.MediaDto
import com.luum.michi.app.core.anilist.dto.MediaListEntryDto
import com.luum.michi.app.core.anilist.dto.toComparableInt
import com.luum.michi.app.core.model.MediaReleaseDateTime
import com.luum.michi.app.core.model.toMediaReleaseDateTime
import com.luum.michi.app.core.platform.hexToPalette

internal fun MediaListEntryDto.toAnimeListEntry(index: Int = 0): AnimeListEntry {
    val section = mapAnimeStatus(status, media.format)
    return AnimeListEntry(
        id = media.id,
        title = media.title.bestTitle(),
        format = formatLabel(media),
        status = section,
        progress = progress,
        totalEpisodes = media.episodes,
        score = formatScore(score),
        nextEpisodeRelease = media.nextAiringEpisode.toMediaReleaseDateTime(),
        palette = hexToPalette(media.coverImage?.color),
        coverUrl = media.coverImage?.thumbnailUrl,
        originalIndex = index,
        scoreDouble = score,
        updatedAt = updatedAt ?: 0L,
        startedAtInt = startedAt.toComparableInt(),
        completedAtInt = completedAt.toComparableInt(),
        releaseDateInt = media.startDate.toComparableInt(),
        averageScore = media.averageScore ?: 0,
        popularity = media.popularity ?: 0,
        favouritesCount = media.favourites ?: 0,
        trending = media.trending ?: 0,
        priority = priority ?: 0,
        nextAiringAt = media.nextAiringEpisode?.airingAt ?: 0L,
        nextEpisodeNumber = media.nextAiringEpisode?.episode,
    )
}

private fun mapAnimeStatus(status: String?, format: String?): AnimeListSection {
    return when (status?.uppercase()) {
        "CURRENT" -> AnimeListSection.WATCHING
        "COMPLETED" -> completedSectionForFormat(format)
        "PAUSED" -> AnimeListSection.PAUSED
        "DROPPED" -> AnimeListSection.DROPPED
        "PLANNING" -> AnimeListSection.PLANNING
        "REPEATING" -> AnimeListSection.REWATCHING
        else -> AnimeListSection.WATCHING
    }
}

private fun completedSectionForFormat(format: String?): AnimeListSection = when (format?.uppercase()) {
    "MOVIE" -> AnimeListSection.COMPLETED_MOVIE
    "OVA" -> AnimeListSection.COMPLETED_OVA
    "ONA" -> AnimeListSection.COMPLETED_ONA
    "TV_SHORT" -> AnimeListSection.COMPLETED_TV_SHORT
    "SPECIAL" -> AnimeListSection.COMPLETED_SPECIAL
    else -> AnimeListSection.COMPLETED_TV
}

private fun formatLabel(media: MediaDto): String {
    val raw = media.format?.replace("_", " ")?.lowercase()
        ?.replaceFirstChar { it.uppercase() }
        ?: "Anime"
    return raw
}

private fun formatScore(score: Double): String {
    if (score <= 0.0) return "-"
    if (score == score.toLong().toDouble()) return score.toLong().toString()
    return ((score * 10).toLong() / 10.0).toString()
}

private fun com.luum.michi.app.core.anilist.dto.MediaTitleDto?.bestTitle(): String {
    if (this == null) return ""
    return userPreferred ?: english ?: romaji ?: native ?: ""
}

private fun epochDayToGregorian(epochDayInput: Long): Triple<Int, Int, Int> {
    var zeroDay = epochDayInput + 719_528L
    zeroDay -= 60L
    var adjust = 0L
    if (zeroDay < 0) {
        val adjustCycles = (zeroDay + 1L) / 146_097L - 1L
        adjust = adjustCycles * 400L
        zeroDay += -adjustCycles * 146_097L
    }
    var yearEstimate = (400L * zeroDay + 591L) / 146_097L
    var dayOfYearEstimate = zeroDay - (
        365L * yearEstimate + yearEstimate / 4L - yearEstimate / 100L + yearEstimate / 400L
    )
    if (dayOfYearEstimate < 0) {
        yearEstimate--
        dayOfYearEstimate = zeroDay - (
            365L * yearEstimate + yearEstimate / 4L - yearEstimate / 100L + yearEstimate / 400L
        )
    }
    yearEstimate += adjust
    val marchMonth = (dayOfYearEstimate * 5L + 2L) / 153L
    val month = ((marchMonth + 2L) % 12L + 1L).toInt()
    val day = (dayOfYearEstimate - (marchMonth * 306L + 5L) / 10L + 1L).toInt()
    val year = (yearEstimate + marchMonth / 10L).toInt()
    return Triple(year, month, day)
}
