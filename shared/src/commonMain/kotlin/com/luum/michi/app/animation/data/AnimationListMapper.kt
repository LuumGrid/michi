package com.luum.michi.app.animation.data

import androidx.compose.ui.graphics.Color
import com.luum.michi.app.animation.presentation.model.AnimationListEntry
import com.luum.michi.app.animation.presentation.model.AnimationListSection
import com.luum.michi.app.core.anilist.dto.MediaDto
import com.luum.michi.app.core.anilist.dto.MediaListEntryDto
import com.luum.michi.app.core.anilist.dto.MediaNextAiringEpisodeDto
import com.luum.michi.app.core.anilist.dto.toComparableInt
import com.luum.michi.app.core.model.MediaReleaseDateTime
import com.luum.michi.app.core.platform.hexToPalette
import com.luum.michi.app.core.media.toLocalMediaReleaseDateTime

internal fun MediaListEntryDto.toAnimationListEntry(index: Int = 0): AnimationListEntry {
    val section = mapAnimationStatus(status, media.format)
    return AnimationListEntry(
        id = media.id,
        title = media.title.bestTitle(),
        format = formatLabel(media),
        status = section,
        progress = progress,
        totalEpisodes = media.episodes,
        score = formatScore(score),
        nextEpisodeRelease = media.nextAiringEpisode.toMediaReleaseDateTime(),
        palette = hexToPalette(media.coverImage?.color),
        coverUrl = media.coverImage?.extraLarge ?: media.coverImage?.large,
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

private fun mapAnimationStatus(status: String?, format: String?): AnimationListSection {
    return when (status?.uppercase()) {
        "CURRENT" -> AnimationListSection.WATCHING
        "COMPLETED" -> completedSectionForFormat(format)
        "PAUSED" -> AnimationListSection.PAUSED
        "DROPPED" -> AnimationListSection.DROPPED
        "PLANNING" -> AnimationListSection.PLANNING
        "REPEATING" -> AnimationListSection.REWATCHING
        else -> AnimationListSection.WATCHING
    }
}

private fun completedSectionForFormat(format: String?): AnimationListSection = when (format?.uppercase()) {
    "MOVIE" -> AnimationListSection.COMPLETED_MOVIE
    "OVA" -> AnimationListSection.COMPLETED_OVA
    "ONA" -> AnimationListSection.COMPLETED_ONA
    "TV_SHORT" -> AnimationListSection.COMPLETED_TV_SHORT
    "SPECIAL" -> AnimationListSection.COMPLETED_SPECIAL
    else -> AnimationListSection.COMPLETED_TV
}

private fun formatLabel(media: MediaDto): String {
    val raw = media.format?.replace("_", " ")?.lowercase()
        ?.replaceFirstChar { it.uppercase() }
        ?: "Animation"
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

private fun MediaNextAiringEpisodeDto?.toMediaReleaseDateTime(): MediaReleaseDateTime? {
    if (this == null) return null
    return airingAt.toLocalMediaReleaseDateTime()
}

internal fun Long.toMediaReleaseDateTime(): MediaReleaseDateTime {
    return this.toLocalMediaReleaseDateTime()
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
