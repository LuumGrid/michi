package com.luum.michi.app.calendar.domain.model

import com.luum.michi.app.core.domain.language.LanguageStrings
import com.luum.michi.app.core.domain.medialist.MediaListStatus
import com.luum.michi.app.core.domain.model.MediaSeason
import com.luum.michi.app.core.domain.model.MediaSeasonYear
import com.luum.michi.app.core.domain.model.next
import com.luum.michi.app.core.domain.model.previous

/**
 * Season bucket for calendar entries, relative to [current] season.
 * OTHER covers media without season tracking (long-running or sporadic
 * releases). Each predicate lives here exactly once.
 */
internal enum class CalendarSeasonFilter { ALL, PREVIOUS, CURRENT, NEXT, OTHER }

internal fun CalendarSeasonFilter.label(strings: LanguageStrings): String = when (this) {
    CalendarSeasonFilter.ALL -> strings.calendarSeasonAll
    CalendarSeasonFilter.PREVIOUS -> strings.calendarSeasonPrevious
    CalendarSeasonFilter.CURRENT -> strings.calendarSeasonCurrent
    CalendarSeasonFilter.NEXT -> strings.calendarSeasonNext
    CalendarSeasonFilter.OTHER -> strings.otherLabel
}

internal fun CalendarSeasonFilter.matches(
    season: MediaSeason?,
    seasonYear: Int?,
    current: MediaSeasonYear,
): Boolean = when (this) {
    CalendarSeasonFilter.ALL -> true
    CalendarSeasonFilter.PREVIOUS -> seasonMatches(season, seasonYear, current.previous())
    CalendarSeasonFilter.CURRENT -> seasonMatches(season, seasonYear, current)
    CalendarSeasonFilter.NEXT -> seasonMatches(season, seasonYear, current.next())
    CalendarSeasonFilter.OTHER -> season == null || seasonYear == null
}

private fun seasonMatches(season: MediaSeason?, seasonYear: Int?, target: MediaSeasonYear): Boolean =
    season == target.season && seasonYear == target.year

/**
 * List-membership bucket for calendar entries. Predicates are literal:
 * an untracked-season item outside the lists answers both NOT_IN_LIST and
 * OTHER — each label asks a different question, so the corner overlaps
 * honestly instead of hiding behind invented exclusions.
 */
internal enum class CalendarStatusFilter { ALL, WATCHING_PLANNING, NOT_IN_LIST, OTHER }

internal fun CalendarStatusFilter.label(strings: LanguageStrings): String = when (this) {
    CalendarStatusFilter.ALL -> strings.calendarStatusAll
    CalendarStatusFilter.WATCHING_PLANNING -> strings.calendarStatusWatchingPlanning
    CalendarStatusFilter.NOT_IN_LIST -> strings.calendarStatusNotInList
    CalendarStatusFilter.OTHER -> strings.otherLabel
}

internal fun CalendarStatusFilter.matches(
    userStatus: MediaListStatus?,
    season: MediaSeason?,
    seasonYear: Int?,
): Boolean = when (this) {
    CalendarStatusFilter.ALL -> true
    CalendarStatusFilter.WATCHING_PLANNING ->
        userStatus == MediaListStatus.CURRENT || userStatus == MediaListStatus.PLANNING
    CalendarStatusFilter.NOT_IN_LIST -> userStatus == null
    CalendarStatusFilter.OTHER -> season == null || seasonYear == null
}
