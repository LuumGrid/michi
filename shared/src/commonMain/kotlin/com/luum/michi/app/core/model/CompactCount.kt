package com.luum.michi.app.core.model

/**
 * Compact count for stat chips and stat columns: `950`, `1.8K`, `12K`, `1.3M`.
 * Truncates (never rounds up across a unit) and drops a zero decimal.
 */
internal fun Int.compactCount(): String = when {
    this >= 1_000_000 -> compactUnit(this, 1_000_000, "M")
    this >= 1_000 -> compactUnit(this, 1_000, "K")
    else -> toString()
}

private fun compactUnit(value: Int, unit: Int, suffix: String): String {
    val tenths = value.toLong() * 10 / unit
    val whole = tenths / 10
    val fraction = tenths % 10
    return if (fraction == 0L || whole >= 10) "$whole$suffix" else "$whole.$fraction$suffix"
}
