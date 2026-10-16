package com.luum.michi.app.core.media.repository.dto

import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.model.CalendarDateParts
import com.luum.michi.app.core.model.calendarPartsToMillis
import kotlinx.serialization.Serializable

/** AniList `FuzzyDate` shape (year/month/day all optional). */
@Serializable
internal data class FuzzyDateDto(
    val year: Int? = null,
    val month: Int? = null,
    val day: Int? = null,
)

/**
 * Single shared formatter for a [FuzzyDateDto] (media airing dates, staff
 * birth/death dates, character birthdays). Uses the language's month name and
 * its day/month order: EN `"Oct 5, 2026"`, ES `"5 oct 2026"`.
 *
 * Partial dates keep their existing shape: month+year → `"Oct 2026"`,
 * year-only → `"2026"`, month-only → `"Oct"`. [includeYear] lets the birthday
 * label stay year-less while still sharing the one formatter.
 */
internal fun FuzzyDateDto?.format(strings: LanguageStrings, includeYear: Boolean = true): String? {
    if (this == null) return null
    val monthName = month?.let { strings.monthShortName(it) }
    val dayText = day?.toString()
    val yearText = if (includeYear) year?.toString() else null
    return when {
        monthName != null && dayText != null && yearText != null ->
            if (strings.dateDayFirst) "$dayText $monthName $yearText" else "$monthName $dayText, $yearText"
        monthName != null && dayText != null ->
            if (strings.dateDayFirst) "$dayText $monthName" else "$monthName $dayText"
        monthName != null && yearText != null -> "$monthName $yearText"
        yearText != null -> yearText
        monthName != null -> monthName
        else -> null
    }
}

/** Local-midnight millis for a complete FuzzyDate, or null when any part is missing. */
internal fun FuzzyDateDto.toMillisOrNull(): Long? {
    val y = year ?: return null
    val m = month ?: return null
    val d = day ?: return null
    return calendarPartsToMillis(CalendarDateParts(y, m, d))
}
