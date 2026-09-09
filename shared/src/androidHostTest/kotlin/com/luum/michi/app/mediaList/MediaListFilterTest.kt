package com.luum.michi.app.mediaList

import com.luum.michi.app.core.model.MediaFormat
import com.luum.michi.app.core.model.MediaSeason
import com.luum.michi.app.core.medialist.domain.MediaListEntryRepository
import com.luum.michi.app.core.medialist.domain.MediaListStatus
import com.luum.michi.app.core.medialist.domain.MediaListViewerEntry
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.mediaList.domain.anime.AnimeListRepository
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListEntry
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListSection
import com.luum.michi.app.mediaList.ui.anime.state.AnimeListStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun animeEntry(
    id: Int,
    genres: List<String> = emptyList(),
    season: MediaSeason? = null,
    seasonYear: Int? = null,
    format: MediaFormat = MediaFormat.TV,
) = AnimeListEntry(
    id = id,
    title = "Title $id",
    format = format,
    status = AnimeListSection.WATCHING,
    progress = 1,
    totalEpisodes = 12,
    score = 8.0,
    nextEpisodeRelease = null,
    paletteHex = null,
    genres = genres,
    season = season,
    seasonYear = seasonYear,
)

private class FakeAnimeListRepository(
    private val entries: List<AnimeListEntry>,
) : AnimeListRepository {
    override suspend fun loadList(userId: Int): NetworkResult<List<AnimeListEntry>> =
        NetworkResult.Success(entries)
}

private class FakeEntryRepository : MediaListEntryRepository {
    override suspend fun saveEntry(
        mediaId: Int,
        status: MediaListStatus,
        progress: Int,
        progressVolumes: Int?,
        score: Float,
        notes: String,
        repeat: Int,
        priority: Int,
        isPrivate: Boolean,
        hiddenFromStatusLists: Boolean,
        startedAtMillis: Long?,
        completedAtMillis: Long?,
    ): NetworkResult<MediaListViewerEntry> = throw NotImplementedError()

    override suspend fun saveProgress(
        mediaId: Int,
        progress: Int,
        status: MediaListStatus?,
        progressVolumes: Int?,
    ): NetworkResult<Unit> = NetworkResult.Success(Unit)

    override suspend fun toggleFavourite(mediaId: Int, isManga: Boolean): NetworkResult<Unit> =
        throw NotImplementedError()

    override suspend fun deleteEntry(entryId: Int): NetworkResult<Unit> =
        throw NotImplementedError()
}

class MediaListFilterTest {

    private fun holderWith(vararg entries: AnimeListEntry): AnimeListStateHolder {
        val holder = AnimeListStateHolder(
            repository = FakeAnimeListRepository(entries.toList()),
            entryRepository = FakeEntryRepository(),
            scope = CoroutineScope(Dispatchers.Unconfined),
        )
        holder.load(userId = 1)
        return holder
    }

    @Test
    fun noFiltersShowsEverything() {
        val holder = holderWith(
            animeEntry(1, genres = listOf("Action"), season = MediaSeason.WINTER, seasonYear = 2024),
            animeEntry(2, genres = listOf("Drama"), season = MediaSeason.SPRING, seasonYear = 2023),
        )
        assertEquals(listOf(1, 2), holder.entriesInSection(AnimeListSection.WATCHING).map { it.id }.sorted())
    }

    @Test
    fun genreFilterMatchesAnyGenre() {
        val holder = holderWith(
            animeEntry(1, genres = listOf("Action", "Comedy")),
            animeEntry(2, genres = listOf("Drama")),
        )
        holder.updateListFilters(season = null, genres = listOf("Comedy"), formats = emptyList(), year = null)
        assertEquals(listOf(1), holder.entriesInSection(AnimeListSection.WATCHING).map { it.id }.sorted())
    }

    @Test
    fun seasonAndYearFilterCombine() {
        val holder = holderWith(
            animeEntry(1, season = MediaSeason.WINTER, seasonYear = 2024),
            animeEntry(2, season = MediaSeason.WINTER, seasonYear = 2023),
            animeEntry(3, season = MediaSeason.SPRING, seasonYear = 2024),
        )
        holder.updateListFilters(season = MediaSeason.WINTER, genres = emptyList(), formats = emptyList(), year = 2024)
        assertEquals(listOf(1), holder.entriesInSection(AnimeListSection.WATCHING).map { it.id }.sorted())
    }

    @Test
    fun clearingFiltersRestoresEverything() {
        val holder = holderWith(
            animeEntry(1, genres = listOf("Action")),
            animeEntry(2, genres = listOf("Drama")),
        )
        holder.updateListFilters(season = null, genres = listOf("Action"), formats = emptyList(), year = null)
        assertEquals(listOf(1), holder.entriesInSection(AnimeListSection.WATCHING).map { it.id }.sorted())
        holder.updateListFilters(season = null, genres = emptyList(), formats = emptyList(), year = null)
        assertEquals(listOf(1, 2), holder.entriesInSection(AnimeListSection.WATCHING).map { it.id }.sorted())
    }

    @Test
    fun formatFilterSelectsMatchingFormats() {
        val holder = holderWith(
            animeEntry(1, format = MediaFormat.MOVIE),
            animeEntry(2, format = MediaFormat.TV),
            animeEntry(3, format = MediaFormat.OVA),
        )
        holder.updateListFilters(season = null, genres = emptyList(), formats = listOf("MOVIE", "OVA"), year = null)
        assertEquals(listOf(1, 3), holder.entriesInSection(AnimeListSection.WATCHING).map { it.id }.sorted())
    }

    @Test
    fun entriesWithoutDataAreExcludedOnlyWhenFiltering() {
        val holder = holderWith(animeEntry(1))
        assertTrue(holder.entriesInSection(AnimeListSection.WATCHING).isNotEmpty())
        holder.updateListFilters(season = MediaSeason.WINTER, genres = emptyList(), formats = emptyList(), year = null)
        assertTrue(holder.entriesInSection(AnimeListSection.WATCHING).isEmpty())
    }
}
