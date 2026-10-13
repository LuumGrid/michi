package com.luum.michi.app.calendar.domain.model

import com.luum.michi.app.core.language.domain.LanguageStrings

/**
 * Countdown suffix for the calendar episode line ("in 3 hours 40 minutes").
 * Null once the episode has aired, so past buckets (yesterday) never show
 * a negative countdown.
 */
internal fun airingCountdownLabel(
    strings: LanguageStrings,
    airingAtEpochSeconds: Long,
    nowEpochSeconds: Long,
): String? {
    val remaining = airingAtEpochSeconds - nowEpochSeconds
    if (remaining <= 0) return null
    val days = (remaining / 86_400).toInt()
    val hours = ((remaining % 86_400) / 3600).toInt()
    val minutes = ((remaining % 3600) / 60).toInt()
    return strings.airingCountdownLabel(days, hours, minutes)
}
