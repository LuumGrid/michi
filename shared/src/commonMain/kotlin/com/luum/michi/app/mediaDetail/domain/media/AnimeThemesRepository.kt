package com.luum.michi.app.mediaDetail.domain.media

import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.mediaDetail.domain.media.model.AnimeTheme

internal interface AnimeThemesRepository {
    /** Themes of the anime with this AniList id, in AnimeThemes' order; empty when AnimeThemes has none. */
    suspend fun themes(aniListId: Int): NetworkResult<List<AnimeTheme>>
}
