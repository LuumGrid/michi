package com.luum.michi.app.mediaDetail

import com.luum.michi.app.core.language.domain.EnglishStrings
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.medialist.domain.MediaListStatus
import com.luum.michi.app.core.medialist.domain.MediaListViewerEntry
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.mediaDetail.domain.media.MediaDetailRepository
import com.luum.michi.app.mediaDetail.domain.media.model.MediaCharactersPage
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetail
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetailType
import com.luum.michi.app.mediaDetail.domain.media.model.MediaRecommendationEntry
import com.luum.michi.app.mediaDetail.domain.media.model.MediaStaffPage
import com.luum.michi.app.mediaDetail.repository.media.MediaListEntryRepositoryImpl
import com.luum.michi.app.mediaDetail.ui.media.state.MediaEntryEditorState
import com.luum.michi.app.mediaList.FakeGraphQL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class StubDetailRepository(
    private val detail: MediaDetail,
) : MediaDetailRepository {
    override suspend fun loadDetail(
        mediaId: Int,
        voiceLanguage: String,
        strings: LanguageStrings,
    ): NetworkResult<MediaDetail> = NetworkResult.Success(detail)

    override suspend fun loadCharactersPage(
        mediaId: Int,
        page: Int,
        voiceLanguage: String,
    ): NetworkResult<MediaCharactersPage> =
        NetworkResult.Failure(NetworkError.Http(500, null))

    override suspend fun loadStaffPage(
        mediaId: Int,
        page: Int,
    ): NetworkResult<MediaStaffPage> =
        NetworkResult.Failure(NetworkError.Http(500, null))

    override suspend fun loadRecommendations(
        mediaId: Int,
    ): NetworkResult<List<MediaRecommendationEntry>> =
        NetworkResult.Failure(NetworkError.Http(500, null))
}

private fun editorDetail(viewerEntry: MediaListViewerEntry?) = MediaDetail(
    id = 101,
    type = MediaDetailType.ANIME,
    title = "Editor Anime",
    coverUrl = null,
    bannerUrl = null,
    paletteHex = null,
    format = "TV",
    status = "RELEASING",
    episodes = 12,
    chapters = null,
    volumes = null,
    duration = null,
    genres = emptyList(),
    studios = emptyList(),
    source = null,
    season = null,
    startedLabel = null,
    endedLabel = null,
    averageScore = null,
    meanScore = null,
    popularity = null,
    favourites = null,
    descriptionPlain = "",
    isAdult = false,
    isFavourite = false,
    viewerEntry = viewerEntry,
    relations = emptyList(),
    scoreDistribution = emptyList(),
    statusDistribution = emptyList(),
    characters = MediaCharactersPage(emptyList(), hasNextPage = false, currentPage = 1),
    staff = MediaStaffPage(emptyList(), hasNextPage = false, currentPage = 1),
)

private fun existingEntry() = MediaListViewerEntry(
    id = 7,
    status = MediaListStatus.CURRENT,
    progress = 5,
    progressVolumes = null,
    score = 8f,
    notes = "note",
    repeat = 1,
    priority = 2,
    isPrivate = true,
    hiddenFromStatusLists = false,
    startedAtMillis = null,
    completedAtMillis = null,
)

private fun editorWired(
    graphQL: FakeGraphQL,
    viewerEntry: MediaListViewerEntry?,
    mediaId: Int = 101,
): MediaEntryEditorState = MediaEntryEditorState(
    entryRepository = MediaListEntryRepositoryImpl(graphQL),
    detailRepository = StubDetailRepository(editorDetail(viewerEntry)),
    scope = CoroutineScope(Dispatchers.Unconfined),
    strings = EnglishStrings,
    mediaId = mediaId,
)

class MediaEntryEditorIntegrationTest {

    @Test
    fun loadHydratesExistingEntry() {
        val editor = editorWired(FakeGraphQL(listOf(buildJsonObject {} )), existingEntry())

        editor.load()

        assertEquals(MediaListStatus.CURRENT, editor.status)
        assertEquals(5, editor.progress)
        assertEquals(8f, editor.score)
        assertEquals("note", editor.notes)
        assertEquals(1, editor.repeat)
        assertEquals(2, editor.priority)
        assertTrue(editor.isPrivate)
        assertTrue(editor.isExisting)
    }

    @Test
    fun loadDefaultsPlanningForNewEntry() {
        val editor = editorWired(FakeGraphQL(listOf(buildJsonObject {})), viewerEntry = null)

        editor.load()

        assertEquals(MediaListStatus.PLANNING, editor.status)
        assertEquals(0, editor.progress)
        assertFalse(editor.isExisting)
    }

    @Test
    fun saveSendsVariablesAndInvokesSaved() {
        val saveAck = buildJsonObject {
            put("SaveMediaListEntry", buildJsonObject { put("id", 7) })
        }
        val graphQL = FakeGraphQL(listOf(saveAck))
        val editor = editorWired(graphQL, existingEntry())

        editor.load()
        editor.updateStatus(MediaListStatus.COMPLETED)
        editor.updateScore(9f)
        var saved = false
        editor.save { saved = true }

        assertNull(editor.error, "save error")
        assertEquals(1, graphQL.requests.size, "save fired")
        assertFalse(editor.isSaving, "still saving")
        assertTrue(saved)
        val variables = graphQL.requests.single().variables
        assertEquals("COMPLETED", variables?.get("status")?.toString()?.trim('"'))
        assertEquals(101, variables?.get("mediaId")?.toString()?.toInt())
    }

    @Test
    fun deleteCallsEntryApiAndInvokesDeleted() {
        val deleteAck = buildJsonObject { put("DeleteMediaListEntry", buildJsonObject {}) }
        val graphQL = FakeGraphQL(listOf(deleteAck))
        val editor = editorWired(graphQL, existingEntry())

        editor.load()
        var deleted = false
        editor.delete { deleted = true }

        assertTrue(deleted)
        val variables = graphQL.requests.single().variables
        assertEquals(7, variables?.get("id")?.toString()?.toInt())
    }

    @Test
    fun favouriteRollsBackOnFailure() {
        val graphQL = FakeGraphQL(listOf(buildJsonObject {}), failureAt = 0)
        val editor = editorWired(graphQL, existingEntry())

        editor.load()
        editor.toggleFavourite()

        assertFalse(editor.isFavourite)
        assertIs<NetworkError.Http>(editor.error)
    }
}
