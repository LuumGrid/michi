package com.luum.michi.app.core.util

internal fun relativeTime(epochSeconds: Long, nowEpochSeconds: Long): String {
    val diff = nowEpochSeconds - epochSeconds
    return when {
        diff < 60 -> "${diff}s"
        diff < 3600 -> "${diff / 60}m"
        diff < 86400 -> "${diff / 3600}h"
        else -> "${diff / 86400}d"
    }
}
