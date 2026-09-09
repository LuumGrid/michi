package com.luum.michi.app.mediaList.domain.manga.model

import com.luum.michi.app.core.model.MediaFormat

/** Whether entries of this format track progress by volumes instead of chapters. */
internal fun MediaFormat.isVolumeBased(): Boolean = this == MediaFormat.NOVEL
