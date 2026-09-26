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
import com.luum.michi.app.mediaDetail.repository.media.MediaDetailRepositoryImpl
import com.luum.michi.app.mediaDetail.repository.media.MediaListEntryRepositoryImpl
import com.luum.michi.app.mediaDetail.ui.media.state.MediaEntryEditorState
import com.luum.michi.app.mediaList.FakeGraphQL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonNull
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
    advancedScoringEnabled: Boolean = false,
    advancedScoringAnimeNames: List<String> = emptyList(),
): MediaEntryEditorState = MediaEntryEditorState(
    entryRepository = MediaListEntryRepositoryImpl(graphQL),
    detailRepository = StubDetailRepository(editorDetail(viewerEntry)),
    scope = CoroutineScope(Dispatchers.Unconfined),
    strings = EnglishStrings,
    mediaId = mediaId,
    advancedScoringEnabled = advancedScoringEnabled,
    advancedScoringAnimeNames = advancedScoringAnimeNames,
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

    @Test
    fun advancedScoresHydrateAlignedToCategoryNames() {
        val entry = existingEntry().copy(advancedScores = mapOf("Story" to 80f, "Animation" to 70f, "Extra" to 60f))
        val editor = editorWired(
            FakeGraphQL(listOf(buildJsonObject {})),
            entry,
            advancedScoringEnabled = true,
            advancedScoringAnimeNames = listOf("Story", "Animation"),
        )

        editor.load()

        assertTrue(editor.showAdvancedScoring)
        assertEquals(listOf(80f, 70f), editor.advancedScoreValues)
    }

    @Test
    fun advancedScoresPadShortServerArrays() {
        val entry = existingEntry().copy(advancedScores = mapOf("Story" to 80f))
        val editor = editorWired(
            FakeGraphQL(listOf(buildJsonObject {})),
            entry,
            advancedScoringEnabled = true,
            advancedScoringAnimeNames = listOf("Story", "Animation"),
        )

        editor.load()

        assertEquals(listOf(80f, 0f), editor.advancedScoreValues)
    }

    @Test
    fun advancedScoresHiddenWithoutFlagOrNames() {
        val enabledNoNames = editorWired(
            FakeGraphQL(listOf(buildJsonObject {})),
            existingEntry().copy(advancedScores = mapOf("Story" to 80f)),
            advancedScoringEnabled = true,
        )
        val disabledWithNames = editorWired(
            FakeGraphQL(listOf(buildJsonObject {})),
            existingEntry().copy(advancedScores = mapOf("Story" to 80f)),
            advancedScoringEnabled = false,
            advancedScoringAnimeNames = listOf("Story"),
        )

        enabledNoNames.load()
        disabledWithNames.load()

        assertFalse(enabledNoNames.showAdvancedScoring)
        assertFalse(disabledWithNames.showAdvancedScoring)
    }

    @Test
    fun saveSendsAdvancedScoresOnlyWhenShown() {
        val saveAck = buildJsonObject {
            put("SaveMediaListEntry", buildJsonObject { put("id", 7) })
        }
        val graphQL = FakeGraphQL(listOf(saveAck))
        val editor = editorWired(
            graphQL,
            existingEntry().copy(advancedScores = mapOf("Story" to 80f, "Animation" to 70f)),
            advancedScoringEnabled = true,
            advancedScoringAnimeNames = listOf("Story", "Animation"),
        )

        editor.load()
        editor.updateAdvancedScore(0, 85f)
        editor.updateAdvancedScore(1, 150f)
        editor.updateAdvancedScore(9, 50f)
        var saved = false
        editor.save { saved = true }

        assertTrue(saved)
        val sent = graphQL.requests.single().variables?.get("advancedScores")
        assertEquals("[85.0,100.0]", sent.toString())
    }

    @Test
    fun saveOmitsAdvancedScoresWhenHidden() {        val saveAck = buildJsonObject {
            put("SaveMediaListEntry", buildJsonObject { put("id", 7) })
        }
        val graphQL = FakeGraphQL(listOf(saveAck))
        val editor = editorWired(graphQL, existingEntry())

        editor.load()
        var saved = false
        editor.save { saved = true }

        assertTrue(saved)
        assertNull(graphQL.requests.single().variables?.get("advancedScores"))
    }

    @Test
    fun explicitNullAdvancedScoresDecodeToEmpty() {
        // Live shape: the server sends explicit nulls for unset fields.
        // A non-null List default only covers missing keys — explicit null
        // used to throw and break the whole detail load.
        val page = buildJsonObject {
            put("Media", buildJsonObject {
                put("id", 1)
                put("mediaListEntry", buildJsonObject {
                    put("id", 7)
                    put("advancedScores", JsonNull)
                })
            })
        }
        val repository = MediaDetailRepositoryImpl(FakeGraphQL(listOf(page)))

        val result = runBlocking { repository.loadDetail(1, "JAPANESE", EnglishStrings) }

        assertIs<NetworkResult.Success<MediaDetail>>(result)
        assertEquals(emptyMap(), result.value.viewerEntry?.advancedScores)
    }

    @Test
    fun populatedObjectAdvancedScoresDecodeToMap() {
        // Live shape: Json object keyed by category name, NOT an array.
        // Decoding it as List used to throw and break the whole detail load.
        val page = buildJsonObject {
            put("Media", buildJsonObject {
                put("id", 1)
                put("mediaListEntry", buildJsonObject {
                    put("id", 7)
                    put("advancedScores", buildJsonObject {
                        put("Story", 80)
                        put("Animation", 70)
                    })
                })
            })
        }
        val repository = MediaDetailRepositoryImpl(FakeGraphQL(listOf(page)))

        val result = runBlocking { repository.loadDetail(1, "JAPANESE", EnglishStrings) }

        assertIs<NetworkResult.Success<MediaDetail>>(result)
        assertEquals(
            mapOf("Story" to 80f, "Animation" to 70f),
            result.value.viewerEntry?.advancedScores,
        )
    }
}
