package com.luum.michi.app.account.presentation.util

private const val MillisPerDay = 86_400_000L

internal fun String.toBirthDateInput(): String {
    val digits = filter(Char::isDigit).take(8)
    return buildString {
        digits.forEachIndexed { index, char ->
            if (index == 2 || index == 4) append('/')
            append(char)
        }
    }
}

internal fun Long.toDayMonthYear(): String {
    val epochDay = floorDivBy(MillisPerDay)
    val (year, month, day) = epochDay.toGregorianDate()
    return "${day.twoDigits()}/${month.twoDigits()}/$year"
}

private fun Int.twoDigits(): String = toString().padStart(2, '0')

private fun Long.floorDivBy(other: Long): Long {
    var result = this / other
    if ((this xor other) < 0 && result * other != this) result--
    return result
}

private fun Long.toGregorianDate(): Triple<Int, Int, Int> {
    var zeroDay = this + 719_528L
    zeroDay -= 60L
    var adjust = 0L
    if (zeroDay < 0) {
        val adjustCycles = (zeroDay + 1L) / 146_097L - 1L
        adjust = adjustCycles * 400L
        zeroDay += -adjustCycles * 146_097L
    }
    var yearEstimate = (400L * zeroDay + 591L) / 146_097L
    var dayOfYearEstimate = zeroDay - (
        365L * yearEstimate + yearEstimate / 4L - yearEstimate / 100L + yearEstimate / 400L
    )
    if (dayOfYearEstimate < 0) {
        yearEstimate--
        dayOfYearEstimate = zeroDay - (
            365L * yearEstimate + yearEstimate / 4L - yearEstimate / 100L + yearEstimate / 400L
        )
    }
    yearEstimate += adjust
    val marchMonth = (dayOfYearEstimate * 5L + 2L) / 153L
    val month = ((marchMonth + 2L) % 12L + 1L).toInt()
    val day = (dayOfYearEstimate - (marchMonth * 306L + 5L) / 10L + 1L).toInt()
    val year = (yearEstimate + marchMonth / 10L).toInt()
    return Triple(year, month, day)
}
