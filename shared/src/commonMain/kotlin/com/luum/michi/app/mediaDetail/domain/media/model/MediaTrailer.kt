package com.luum.michi.app.mediaDetail.domain.media.model

internal fun MediaTrailer.videoUrl(): String? = when {
    site.equals("youtube", ignoreCase = true) -> "https://youtu.be/$videoId"
    site.equals("dailymotion", ignoreCase = true) -> "https://www.dailymotion.com/video/$videoId"
    site.isNullOrBlank() && videoId.startsWith("http") -> videoId
    else -> null
}
