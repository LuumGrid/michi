package com.luum.michi.app.core.model

data class MediaReleaseDateTime(
    val day: Int,
    val month: Int,
    val year: Int,
    val hour: Int,
    val minute: Int,
) {
    fun formatDateTime(): String {
        return "${day.twoDigits()}/${month.twoDigits()}/$year ${hour.twoDigits()}:${minute.twoDigits()}"
    }

    fun formatReadableDateTime(monthName: (Int) -> String): String {
        return "$day ${monthName(month)} $year, ${hour.twoDigits()}:${minute.twoDigits()}"
    }
}

private fun Int.twoDigits(): String = toString().padStart(length = 2, padChar = '0')
