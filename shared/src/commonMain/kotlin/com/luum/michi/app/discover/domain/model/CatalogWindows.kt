package com.luum.michi.app.discover.domain.model

import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Shared catalog windows: the Dashboard's static rails and the "View all"
 * catalog pages must agree on these numbers, so they live here instead of in
 * either query.
 */

/**
 * Popularity floor for the "best rated" shelves: pure SCORE_DESC surfaces
 * obscure 90-score titles with a handful of votes, so a floor keeps them
 * recognizable (same idea as the New releases shelf).
 */
internal const val BestRatedMinPopularity = 5000

/** Popularity floor for the (narrower, 12-month) New releases shelf. */
internal const val NewReleasesMinPopularity = 1000

/** FuzzyDateInt (YYYYMMDD) one year back: the "recent" shelf window. */
internal fun recentStartFuzzy(now: Instant = Clock.System.now()): Int {
    val date = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
    val year = date.year - 1
    return year * 10000 + date.month.number * 100 + date.day
}
