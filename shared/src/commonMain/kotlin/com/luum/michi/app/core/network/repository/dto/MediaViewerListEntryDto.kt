package com.luum.michi.app.core.network.repository.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The viewer's list entry as embedded in a `Media` node (id/status/progress/
 * score/dates). Shared by [MediaDto] and the media search shape, so it lives in
 * core.
 */
@Serializable
internal data class MediaViewerListEntryDto(
    val id: Int,
    val status: String? = null,
    val progress: Int? = null,
    val progressVolumes: Int? = null,
    val score: Double? = null,
    val advancedScores: Map<String, Float>? = null,
    val notes: String? = null,
    val repeat: Int? = null,
    val priority: Int? = null,
    @SerialName("private") val isPrivate: Boolean? = null,
    val hiddenFromStatusLists: Boolean? = null,
    val startedAt: FuzzyDateDto? = null,
    val completedAt: FuzzyDateDto? = null,
)
