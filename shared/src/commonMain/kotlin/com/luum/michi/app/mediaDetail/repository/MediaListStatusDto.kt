package com.luum.michi.app.mediaDetail.repository

import kotlinx.serialization.Serializable

/**
 * The `mediaListEntry { status }` shape embedded in detail/relation nodes: the
 * viewer's list status only, distinct from the full `MediaViewerListEntryDto`
 * and the list `MediaListEntryDto` (both in `core/network`).
 */
@Serializable
internal data class MediaListStatusDto(
    val status: String? = null,
)
