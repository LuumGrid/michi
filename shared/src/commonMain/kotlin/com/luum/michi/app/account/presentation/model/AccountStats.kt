package com.luum.michi.app.account.presentation.model

internal data class AccountStats(
    val animeCount: Int,
    val mangaCount: Int,
    val followingCount: Int,
    val followersCount: Int,
)

internal fun Int.toCompactCountLabel(): String = when {
    this >= 1_000_000 -> "${this / 100_000 / 10.0}M"
    this >= 10_000 -> "${this / 1_000}K"
    this >= 1_000 -> "${this / 100 / 10.0}K"
    else -> toString()
}
