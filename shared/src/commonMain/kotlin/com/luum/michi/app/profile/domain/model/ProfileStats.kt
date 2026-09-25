package com.luum.michi.app.profile.domain.model

internal data class ProfileStats(
    val animeCount: Int,
    val mangaCount: Int,
    val followingCount: Int,
    val followersCount: Int,
    val anime: ProfileMediaTypeStats = ProfileMediaTypeStats(),
    val manga: ProfileMediaTypeStats = ProfileMediaTypeStats(),
)

/** A single labeled bucket in a distribution (score / format / status / genre).
 *  Labels are raw API values ("TV", "CURRENT"); the stats UI formats them at
 *  display time. */
internal data class ProfileStatDistributionEntry(
    val label: String,
    val count: Int,
)

/** Detailed statistics for either the anime or the manga side of a profile. */
internal data class ProfileMediaTypeStats(
    val count: Int = 0,
    /** Episodes watched (anime) or chapters read (manga). */
    val episodesOrChapters: Int = 0,
    /** Days watched (anime) or volumes read (manga). */
    val daysOrVolumes: Double = 0.0,
    val meanScore: Double = 0.0,
    val standardDeviation: Double = 0.0,
    val scoreDistribution: List<ProfileStatDistributionEntry> = emptyList(),
    val formatDistribution: List<ProfileStatDistributionEntry> = emptyList(),
    val statusDistribution: List<ProfileStatDistributionEntry> = emptyList(),
    val topGenres: List<ProfileStatDistributionEntry> = emptyList(),
)

internal fun Int.toCompactCountLabel(): String = when {
    this >= 1_000_000 -> "${this / 100_000 / 10.0}M"
    this >= 10_000 -> "${this / 1_000}K"
    this >= 1_000 -> "${this / 100 / 10.0}K"
    else -> toString()
}
