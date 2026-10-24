package com.luum.michi.app.profile.ui.model

import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.media.domain.label as mediaFormatLabel
import com.luum.michi.app.core.media.domain.parseMediaFormat
import com.luum.michi.app.core.medialist.domain.ScoreFormat
import com.luum.michi.app.core.medialist.domain.formatScoreValue
import com.luum.michi.app.core.medialist.domain.label as mediaListStatusLabel
import com.luum.michi.app.core.medialist.domain.parseMediaListStatus
import com.luum.michi.app.core.medialist.domain.scoreMaxFor
import com.luum.michi.app.profile.domain.model.ProfileStatDistributionEntry
import kotlin.math.round

/**
 * AniList returns `statistics.meanScore` on the 0-100 scale whatever the
 * viewer's [ScoreFormat] is, so it is converted to the viewer's scale before
 * display (a 5-star user reads their mean as 0-5). Pure, so the profile screen
 * never imports settings.
 */
internal fun formatProfileMeanScore(meanScore0To100: Double, format: ScoreFormat): String {
    val scaled = (meanScore0To100 / 100.0 * scoreMaxFor(format)).toFloat()
    val value = when (format) {
        // Keep one decimal for the formats that have one; the rest round in
        // formatScoreValue itself.
        ScoreFormat.POINT_10_DECIMAL, ScoreFormat.POINT_5_STARS -> oneDecimal(scaled)
        else -> scaled
    }
    return formatScoreValue(format, value)
}

/** Anime days watched (or manga volumes) up to one decimal, integers verbatim. */
internal fun formatProfileDays(days: Double): String {
    val rounded = oneDecimal(days.toFloat())
    return if (rounded == rounded.toInt().toFloat()) rounded.toInt().toString() else rounded.toString()
}

/**
 * The score distribution as paintable bars, ascending. `statistics.scores[].score`
 * already comes in the viewer's format, except:
 * - POINT_100 and POINT_10_DECIMAL ship up to 20 buckets on the 0-100 scale
 *   (including non-multiples of 10, e.g. 5/15/…/95), so they are grouped by tens
 *   to keep at most ten bars; the decimal buckets are relabelled on the 0-10 scale.
 * - POINT_10/POINT_5/POINT_3 ship few buckets and are labelled with the same
 *   representation the lists and the editor use.
 */
internal fun profileScoreBars(
    distribution: List<ProfileStatDistributionEntry>,
    format: ScoreFormat,
): List<ProfileStatDistributionEntry> {
    val parsed = distribution.mapNotNull { entry ->
        entry.label.toIntOrNull()?.takeIf { it >= 0 }?.let { it to entry.count }
    }
    if (parsed.isEmpty()) return emptyList()
    return when (format) {
        ScoreFormat.POINT_100 -> parsed.groupedByTens { group -> "${group * 10 + 1}-${group * 10 + 10}" }
        ScoreFormat.POINT_10_DECIMAL -> parsed.groupedByTens { group -> "$group-${group + 1}" }
        else -> parsed.sortedBy { it.first }.map { (score, count) ->
            ProfileStatDistributionEntry(label = formatScoreValue(format, score.toFloat()), count = count)
        }
    }
}

/** Sums the raw buckets into the ten decade groups (0..9), ascending, dropping empties. */
private fun List<Pair<Int, Int>>.groupedByTens(
    label: (group: Int) -> String,
): List<ProfileStatDistributionEntry> {
    val groups = IntArray(10)
    forEach { (score, count) ->
        val group = ((score - 1).coerceAtLeast(0) / 10).coerceIn(0, 9)
        groups[group] += count
    }
    return groups.toList().mapIndexedNotNull { group, total ->
        total.takeIf { it > 0 }?.let { ProfileStatDistributionEntry(label = label(group), count = it) }
    }
}

/** Localized format label ("TV", "One Shot", …) via the shared media vocabulary. */
internal fun ProfileStatDistributionEntry.formatDistributionLabel(strings: LanguageStrings): String =
    parseMediaFormat(label).mediaFormatLabel(strings.formatUnknownLabel)

/** Localized list-status label ("Watching" / "Reading", …), type-aware. */
internal fun ProfileStatDistributionEntry.statusDistributionLabel(
    strings: LanguageStrings,
    isManga: Boolean,
): String = parseMediaListStatus(label)?.mediaListStatusLabel(strings, isManga) ?: label

private fun oneDecimal(value: Float): Float = round(value * 10f) / 10f
