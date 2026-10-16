package com.luum.michi.app.discover.domain

import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.media.domain.MediaGridItem
import com.luum.michi.app.core.model.Page
import com.luum.michi.app.core.media.domain.MediaType
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.discover.domain.model.ExplorePage

internal interface ExploreRepository {
    /**
     * One catalog search for both types: [type] decides the API `type` and the
     * year filter (`season`/`seasonYear` for anime, `startDate_*` bounds for
     * manga). `season` is ignored for manga.
     */
    suspend fun searchMedia(
        type: MediaType,
        query: String?,
        genres: List<String>,
        formats: List<String>,
        year: Int?,
        sort: String,
        page: Int = 1,
        perPage: Int = 30,
        season: String? = null,
        onList: Boolean? = null,
        hideAdult: Boolean = true,
        strings: LanguageStrings,
    ): NetworkResult<ExplorePage>

    /**
     * The same catalog query shaped as a poster grid (studio works, genre
     * works); ordered by the server, paged at `GridPageSize`.
     */
    suspend fun searchMediaGrid(
        type: MediaType,
        genres: List<String>,
        page: Int = 1,
        sort: String,
        hideAdult: Boolean = true,
    ): NetworkResult<Page<MediaGridItem>>

    suspend fun searchCharacters(
        query: String?,
        page: Int = 1,
        perPage: Int = 30,
        strings: LanguageStrings,
    ): NetworkResult<ExplorePage>

    suspend fun searchStaff(
        query: String?,
        page: Int = 1,
        perPage: Int = 30,
        strings: LanguageStrings,
    ): NetworkResult<ExplorePage>

    suspend fun searchStudios(
        query: String?,
        page: Int = 1,
        perPage: Int = 30,
        strings: LanguageStrings,
    ): NetworkResult<ExplorePage>
}
