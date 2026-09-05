package com.luum.michi.app.core.medialist

/** Authenticated viewer's list entry for a media item. Neutral core model shared by list features. */
internal data class MediaListViewerEntry(
    val id: Int,
    val status: MediaListStatus?,
    val progress: Int,
    val progressVolumes: Int?,
    val score: Float,
    val notes: String,
    val repeat: Int,
    val priority: Int,
    val isPrivate: Boolean,
    val hiddenFromStatusLists: Boolean,
    val startedAtMillis: Long?,
    val completedAtMillis: Long?,
)
