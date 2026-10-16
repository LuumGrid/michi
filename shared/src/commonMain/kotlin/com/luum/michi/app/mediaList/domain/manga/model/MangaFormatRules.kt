package com.luum.michi.app.mediaList.domain.manga.model

import com.luum.michi.app.core.media.domain.MediaFormat

/** Whether entries of this format track progress by volumes instead of chapters. */
internal fun MediaFormat.isVolumeBased(): Boolean = this == MediaFormat.NOVEL

/**
 * Completed section this manga format belongs to. With split on, unknown/manga
 * formats fall into the generic manga section (like anime's COMPLETED_TV)
 * instead of the merged COMPLETED, which is not in the split section list and
 * would drop the entry from the UI.
 */
internal fun MediaFormat.completedMangaSection(splitCompleted: Boolean): MangaListSection =
    if (!splitCompleted) {
        MangaListSection.COMPLETED
    } else {
        when (this) {
            MediaFormat.NOVEL -> MangaListSection.COMPLETED_NOVEL
            MediaFormat.ONE_SHOT -> MangaListSection.COMPLETED_ONE_SHOT
            else -> MangaListSection.COMPLETED_MANGA
        }
    }
