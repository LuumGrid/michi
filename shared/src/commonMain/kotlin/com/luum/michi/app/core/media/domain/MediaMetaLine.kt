package com.luum.michi.app.core.media.domain

import com.luum.michi.app.core.language.domain.LanguageStrings

/**
 * One catalog meta line: `format · season year · status`, with labels already
 * translated. Season and year read together ("Fall 2026"); year alone when there
 * is no season. Returns an empty string when nothing is known (callers decide
 * whether to drop the line).
 */
internal fun catalogMetaLine(
    format: String?,
    season: String?,
    year: Int?,
    status: String?,
): String {
    val seasonYear = when {
        season != null && year != null -> "$season $year"
        season != null -> season
        year != null -> year.toString()
        else -> null
    }
    return listOfNotNull(format, seasonYear, status).joinToString(" · ")
}

/**
 * Countdown suffix for an upcoming airing ("in 3 hours 40 minutes"), or null once
 * [remainingSeconds] is 0 or negative, so a past airing never shows a negative
 * countdown.
 */
internal fun airingCountdown(strings: LanguageStrings, remainingSeconds: Long): String? {
    if (remainingSeconds <= 0) return null
    val days = (remainingSeconds / 86_400).toInt()
    val hours = ((remainingSeconds % 86_400) / 3600).toInt()
    val minutes = ((remainingSeconds % 3600) / 60).toInt()
    return strings.airingCountdownLabel(days, hours, minutes)
}
