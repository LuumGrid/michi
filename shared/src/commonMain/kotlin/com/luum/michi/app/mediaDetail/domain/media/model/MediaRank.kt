package com.luum.michi.app.mediaDetail.domain.media.model

/**
 * Prefers an all-time rank of the given [type] (AniList `RATED`/`POPULAR`);
 * among those (or among all of that type when none is all-time) the smallest
 * rank number wins. Returns null when no rank of that type exists.
 */
private fun List<MediaRank>.pickBestRank(type: String): MediaRank? {
    val ofType = filter { it.type.equals(type, ignoreCase = true) }
    return ofType.filter { it.allTime }.minByOrNull { it.rank } ?: ofType.minByOrNull { it.rank }
}

internal fun List<MediaRank>.bestRatedRank(): MediaRank? = pickBestRank("RATED")

internal fun List<MediaRank>.bestPopularRank(): MediaRank? = pickBestRank("POPULAR")

internal fun MediaTrailer.videoUrl(): String? = when {
    site.equals("youtube", ignoreCase = true) -> "https://youtu.be/$videoId"
    site.isNullOrBlank() && videoId.startsWith("http") -> videoId
    else -> null
}
