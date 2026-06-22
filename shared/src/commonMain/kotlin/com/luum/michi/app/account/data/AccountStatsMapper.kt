package com.luum.michi.app.account.data

import com.luum.michi.app.account.presentation.model.AccountMediaTypeStats
import com.luum.michi.app.account.presentation.model.AccountStatDistributionEntry
import com.luum.michi.app.core.anilist.dto.UserStatisticsDto

/**
 * Maps a single anime/manga [UserStatisticsDto] block into the domain
 * [AccountMediaTypeStats]. `isManga` picks chapters/volumes vs episodes/days.
 */
internal fun UserStatisticsDto?.toAccountMediaTypeStats(isManga: Boolean): AccountMediaTypeStats {
    if (this == null) return AccountMediaTypeStats()
    return AccountMediaTypeStats(
        count = count,
        episodesOrChapters = if (isManga) chaptersRead ?: 0 else episodesWatched ?: 0,
        daysOrVolumes = if (isManga) (volumesRead ?: 0).toDouble() else (minutesWatched ?: 0) / 1440.0,
        meanScore = meanScore,
        standardDeviation = standardDeviation,
        scoreDistribution = scores
            .sortedBy { it.score }
            .map { AccountStatDistributionEntry(label = it.score.toString(), count = it.count) },
        formatDistribution = formats
            .sortedByDescending { it.count }
            .map { AccountStatDistributionEntry(label = it.format.orEmpty(), count = it.count) },
        statusDistribution = statuses
            .sortedByDescending { it.count }
            .map { AccountStatDistributionEntry(label = it.status.orEmpty(), count = it.count) },
        topGenres = genres
            .sortedByDescending { it.count }
            .map { AccountStatDistributionEntry(label = it.genre.orEmpty(), count = it.count) },
    )
}
