package com.luum.michi.app.mediaDetail.domain.media.model

import com.luum.michi.app.core.medialist.domain.MediaListViewerEntry

/**
 * Minimal media data the entry editor needs: the title, the catalog, the totals
 * (for progress clamping/automation), the favourite flag and the viewer's own
 * entry. Loaded by its own query, so opening the editor never pays for
 * characters/staff/relations/links.
 */
internal data class MediaEntryEditorData(
    val title: String,
    val type: MediaDetailType,
    val episodes: Int?,
    val chapters: Int?,
    val volumes: Int?,
    val isFavourite: Boolean,
    val viewerEntry: MediaListViewerEntry?,
)
