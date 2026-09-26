package com.luum.michi.app.settings.domain.model

import kotlin.math.roundToInt

/**
 * Score-format translations. The server returns and accepts scores in the
 * user's format (POINT_100 users get 85, DECIMAL users 8.5), so the app
 * holds values verbatim and only translates at the display/parse boundary.
 * Pure logic, covered by unit tests per format.
 */
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

private fun smileyFor(value: Float): String = when {
    value <= 0f -> "—"
    value <= 1f -> "🙁"
    value <= 2f -> "😐"
    else -> "🙂"
}
