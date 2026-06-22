package com.luum.michi.app.account.presentation.model

internal data class AccountStats(
    val animeCount: Int,
    val mangaCount: Int,
    val followingCount: Int,
    val followersCount: Int,
    val anime: AccountMediaTypeStats = AccountMediaTypeStats(),
    val manga: AccountMediaTypeStats = AccountMediaTypeStats(),
)

/** A single labeled bucket in a distribution (score / format / status / genre). */
internal data class AccountStatDistributionEntry(
    val label: String,
    val count: Int,
)

/** Detailed statistics for either the anime or the manga side of a profile. */
internal data class AccountMediaTypeStats(
    val count: Int = 0,
    /** Episodes watched (anime) or chapters read (manga). */
    val episodesOrChapters: Int = 0,
    /** Days watched (anime) or volumes read (manga). */
    val daysOrVolumes: Double = 0.0,
    val meanScore: Double = 0.0,
    val standardDeviation: Double = 0.0,
    val scoreDistribution: List<AccountStatDistributionEntry> = emptyList(),
    val formatDistribution: List<AccountStatDistributionEntry> = emptyList(),
    val statusDistribution: List<AccountStatDistributionEntry> = emptyList(),
    val topGenres: List<AccountStatDistributionEntry> = emptyList(),
)

internal fun Int.toCompactCountLabel(): String = when {
    this >= 1_000_000 -> "${this / 100_000 / 10.0}M"
    this >= 10_000 -> "${this / 1_000}K"
    this >= 1_000 -> "${this / 100 / 10.0}K"
    else -> toString()
}
