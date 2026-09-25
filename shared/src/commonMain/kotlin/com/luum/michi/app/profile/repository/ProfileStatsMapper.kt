package com.luum.michi.app.profile.repository

import com.luum.michi.app.profile.domain.model.ProfileMediaTypeStats
import com.luum.michi.app.profile.domain.model.ProfileStatDistributionEntry
import com.luum.michi.app.core.network.repository.dto.UserStatisticsDto

/**
 * Maps a single anime/manga [UserStatisticsDto] block into the domain
 * [ProfileMediaTypeStats]. `isManga` picks chapters/volumes vs episodes/days.
 */
internal fun UserStatisticsDto?.toProfileMediaTypeStats(isManga: Boolean): ProfileMediaTypeStats {
    if (this == null) return ProfileMediaTypeStats()
    return ProfileMediaTypeStats(
        count = count,
        episodesOrChapters = if (isManga) chaptersRead ?: 0 else episodesWatched ?: 0,
        daysOrVolumes = if (isManga) (volumesRead ?: 0).toDouble() else (minutesWatched ?: 0) / 1440.0,
        meanScore = meanScore,
        standardDeviation = standardDeviation,
        scoreDistribution = scores
            .sortedBy { it.score }
            .map { ProfileStatDistributionEntry(label = it.score.toString(), count = it.count) },
        formatDistribution = formats
            .sortedByDescending { it.count }
            .map { ProfileStatDistributionEntry(label = it.format.orEmpty(), count = it.count) },
        statusDistribution = statuses
            .sortedByDescending { it.count }
            .map { ProfileStatDistributionEntry(label = it.status.orEmpty(), count = it.count) },
        topGenres = genres
            .sortedByDescending { it.count }
            .map { ProfileStatDistributionEntry(label = it.genre.orEmpty(), count = it.count) },
    )
}
