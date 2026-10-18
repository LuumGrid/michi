package com.luum.michi.app.core.medialist.domain

import com.luum.michi.app.core.language.domain.LanguageStrings
import kotlin.math.roundToInt

/**
 * AniList's media-list scoring type (`Viewer.mediaListOptions.scoreFormat`).
 * Neutral vocabulary shared by the editor, the lists, settings and the profile
 * statistics, so it lives in `core` and every feature consumes it.
 *
 * The server returns and accepts scores in the user's format (POINT_100 users
 * get 85, DECIMAL users 8.5), so values are held verbatim and only translated at
 * the display/parse boundary.
 */
internal enum class ScoreFormat {
    POINT_100,
    POINT_10_DECIMAL,
    POINT_10,
    POINT_5_STARS,
    POINT_3_SMILEYS,
}

/** Parses AniList's raw `ScoreFormat` value (`Viewer.mediaListOptions.scoreFormat`). */
internal fun parseScoreFormat(raw: String?): ScoreFormat = when (raw) {
    "POINT_100" -> ScoreFormat.POINT_100
    "POINT_10" -> ScoreFormat.POINT_10
    "POINT_5" -> ScoreFormat.POINT_5_STARS
    "POINT_3" -> ScoreFormat.POINT_3_SMILEYS
    else -> ScoreFormat.POINT_10_DECIMAL
}

internal fun ScoreFormat.label(strings: LanguageStrings): String = when (this) {
    ScoreFormat.POINT_100 -> strings.settingsScoreFormatPoint100
    ScoreFormat.POINT_10_DECIMAL -> strings.settingsScoreFormatPoint10Decimal
    ScoreFormat.POINT_10 -> strings.settingsScoreFormatPoint10
    ScoreFormat.POINT_5_STARS -> strings.settingsScoreFormatPoint5Stars
    ScoreFormat.POINT_3_SMILEYS -> strings.settingsScoreFormatPoint3Smileys
}

internal fun scoreMaxFor(format: ScoreFormat): Float = when (format) {
    ScoreFormat.POINT_100 -> 100f
    ScoreFormat.POINT_10_DECIMAL -> 10f
    ScoreFormat.POINT_10 -> 10f
    ScoreFormat.POINT_5_STARS -> 5f
    ScoreFormat.POINT_3_SMILEYS -> 3f
}

internal fun scoreStepFor(format: ScoreFormat): Float = when (format) {
    ScoreFormat.POINT_100 -> 1f
    ScoreFormat.POINT_10_DECIMAL -> 0.5f
    ScoreFormat.POINT_10 -> 1f
    ScoreFormat.POINT_5_STARS -> 1f
    ScoreFormat.POINT_3_SMILEYS -> 1f
}

internal fun formatScoreValue(format: ScoreFormat, value: Float): String = when (format) {
    ScoreFormat.POINT_100 -> value.roundToInt().toString()
    ScoreFormat.POINT_10_DECIMAL -> trimScoreNumber(value)
    ScoreFormat.POINT_10 -> value.roundToInt().toString()
    ScoreFormat.POINT_5_STARS -> trimScoreNumber(value)
    ScoreFormat.POINT_3_SMILEYS -> smileyFor(value)
}

internal fun parseScoreValue(format: ScoreFormat, raw: String): Float? {
    val parsed = when (format) {
        ScoreFormat.POINT_100 -> raw.toFloatOrNull()
        ScoreFormat.POINT_10_DECIMAL, ScoreFormat.POINT_10 -> raw.toFloatOrNull()
        ScoreFormat.POINT_5_STARS -> raw.toFloatOrNull()
        // Smileys have no typed form (steppers only).
        ScoreFormat.POINT_3_SMILEYS -> null
    } ?: return null
    return parsed.takeIf { it in 0f..scoreMaxFor(format) }
}

internal fun scoreSuffixFor(format: ScoreFormat): String? = when (format) {
    ScoreFormat.POINT_100 -> "/100"
    ScoreFormat.POINT_10_DECIMAL, ScoreFormat.POINT_10 -> "/10"
    ScoreFormat.POINT_5_STARS -> "/5"
    ScoreFormat.POINT_3_SMILEYS -> null
}

private fun trimScoreNumber(value: Float): String =
    if (value == value.toInt().toFloat()) value.toInt().toString() else value.toString()

/** The one smiley representation of the 3-point format, shared by lists, the
 *  editor and the profile statistics. */
private fun smileyFor(value: Float): String = when {
    value <= 0f -> "—"
    value <= 1f -> "🙁"
    value <= 2f -> "😐"
    else -> "🙂"
}
