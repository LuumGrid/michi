package com.luum.michi.app.mediaDetail

import com.luum.michi.app.core.language.domain.EnglishStrings
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.medialist.domain.MediaListStatus
import com.luum.michi.app.core.medialist.domain.MediaListViewerEntry
import com.luum.michi.app.core.model.MediaWorkStatus
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.mediaDetail.domain.media.MediaDetailRepository
import com.luum.michi.app.mediaDetail.domain.media.model.MediaCharactersPage
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetail
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetailType
import com.luum.michi.app.mediaDetail.domain.media.model.MediaRecommendationsPage
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class StubDetailRepository(
    private val detail: MediaDetail,
) : MediaDetailRepository {
    override suspend fun loadDetail(
        mediaId: Int,
        strings: LanguageStrings,
    ): NetworkResult<MediaDetail> = NetworkResult.Success(detail)

    override suspend fun loadCharactersPage(
        mediaId: Int,
        page: Int,
    ): NetworkResult<MediaCharactersPage> =
        NetworkResult.Failure(NetworkError.Http(500, null))

    override suspend fun loadStaffPage(
        mediaId: Int,
        page: Int,
    ): NetworkResult<MediaStaffPage> =
        NetworkResult.Failure(NetworkError.Http(500, null))

    override suspend fun loadRecommendations(
        mediaId: Int,
        page: Int,
    ): NetworkResult<MediaRecommendationsPage> =
        NetworkResult.Failure(NetworkError.Http(500, null))
}

private fun editorDetail(viewerEntry: MediaListViewerEntry?) = MediaDetail(
    id = 101,
    type = MediaDetailType.ANIME,
    title = "Editor Anime",
    cover = null,
    bannerUrl = null,
    paletteHex = null,
    format = "TV",
    status = MediaWorkStatus.RELEASING,
    episodes = 12,
    chapters = null,
    volumes = null,
    duration = null,
    genres = emptyList(),
    studios = emptyList(),
    producers = emptyList(),
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
    characters = MediaCharactersPage(emptyList(), hasNextPage = false, currentPage = 1),
    staff = MediaStaffPage(emptyList(), hasNextPage = false, currentPage = 1),
    titleRomaji = null,
    titleNative = null,
    synonyms = emptyList(),
    tags = emptyList(),
    externalLinks = emptyList(),
    trailer = null,
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
    scoreMax: Float = 10f,
    initialStatusOverride: MediaListStatus? = null,
    initialProgressOverride: Int? = null,
    todayMillis: () -> Long = { TODAY },
): MediaEntryEditorState = MediaEntryEditorState(
    entryRepository = MediaListEntryRepositoryImpl(graphQL),
    detailRepository = StubDetailRepository(editorDetail(viewerEntry)),
    scope = CoroutineScope(Dispatchers.Unconfined),
    stringsProvider = { EnglishStrings },
    mediaId = mediaId,
    advancedScoringEnabled = advancedScoringEnabled,
    advancedScoringAnimeNames = advancedScoringAnimeNames,
    scoreMax = scoreMax,
    initialStatusOverride = initialStatusOverride,
    initialProgressOverride = initialProgressOverride,
    todayMillis = todayMillis,
)

// UTC-midnight millis, like the date pickers speak (2026-01-15).
private const val TODAY = 1_768_435_200_000L

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
    fun saveWithAnEmptyServerEntryFails() {
        // `SaveMediaListEntry: null` (a malformed/refused response) must not be
        // mistaken for a saved entry with id 0.
        val emptyAck = buildJsonObject { put("SaveMediaListEntry", JsonNull) }
        val graphQL = FakeGraphQL(listOf(emptyAck))
        val editor = editorWired(graphQL, existingEntry())

        editor.load()
        var saved = false
        editor.save { saved = true }

        assertFalse(saved, "an empty response is not a save")
        assertNotNull(editor.error)
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
            scoreMax = 100f,
        )

        editor.load()

        assertTrue(editor.showAdvancedScoring)
        assertEquals(listOf(8f, 7f), editor.advancedScoreValues)
    }

    @Test
    fun advancedScoresPadShortServerArrays() {
        val entry = existingEntry().copy(advancedScores = mapOf("Story" to 80f))
        val editor = editorWired(
            FakeGraphQL(listOf(buildJsonObject {})),
            entry,
            advancedScoringEnabled = true,
            advancedScoringAnimeNames = listOf("Story", "Animation"),
            scoreMax = 100f,
        )

        editor.load()

        assertEquals(listOf(8f, 0f), editor.advancedScoreValues)
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
        editor.updateAdvancedScore(0, 8.5f)
        editor.updateAdvancedScore(1, 15f)
        editor.updateAdvancedScore(9, 5f)
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

        val result = runBlocking { repository.loadDetail(1, EnglishStrings) }

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

        val result = runBlocking { repository.loadDetail(1, EnglishStrings) }

        assertIs<NetworkResult.Success<MediaDetail>>(result)
        assertEquals(
            mapOf("Story" to 80f, "Animation" to 70f),
            result.value.viewerEntry?.advancedScores,
        )
    }

    @Test
    fun completedFillsProgressAndStampsToday() {
        val editor = editorWired(FakeGraphQL(listOf(buildJsonObject {})), existingEntry())

        editor.load()
        editor.updateStatus(MediaListStatus.COMPLETED)

        assertEquals(12, editor.progress, "progress fills to the episode total")
        assertEquals(TODAY, editor.completedAtMillis)
        assertNull(editor.startedAtMillis, "completing does not invent a start date")
    }

    @Test
    fun completedKeepsAnExistingCompletionDate() {
        val entry = existingEntry().copy(completedAtMillis = 123L)
        val editor = editorWired(FakeGraphQL(listOf(buildJsonObject {})), entry)

        editor.load()
        editor.updateStatus(MediaListStatus.COMPLETED)

        assertEquals(123L, editor.completedAtMillis)
    }

    @Test
    fun currentStampsStartOnlyWhenEmpty() {
        val fresh = editorWired(FakeGraphQL(listOf(buildJsonObject {})), viewerEntry = null)
        val started = editorWired(
            FakeGraphQL(listOf(buildJsonObject {})),
            existingEntry().copy(startedAtMillis = 99L),
        )

        fresh.load()
        started.load()
        fresh.updateStatus(MediaListStatus.CURRENT)
        started.updateStatus(MediaListStatus.CURRENT)

        assertEquals(TODAY, fresh.startedAtMillis)
        assertEquals(99L, started.startedAtMillis)
    }

    @Test
    fun otherStatusesLeaveProgressAndDatesAlone() {
        val editor = editorWired(FakeGraphQL(listOf(buildJsonObject {})), existingEntry())

        editor.load()
        editor.updateStatus(MediaListStatus.PAUSED)

        assertEquals(5, editor.progress)
        assertNull(editor.startedAtMillis)
        assertNull(editor.completedAtMillis)
    }

    @Test
    fun existingEntryIsCleanUntilEditedAndCleanAgainWhenReverted() {
        val editor = editorWired(FakeGraphQL(listOf(buildJsonObject {})), existingEntry())

        editor.load()
        assertFalse(editor.hasChanges)
        assertFalse(editor.isDirty, "nothing to save yet")

        editor.updateNotes("changed")
        assertTrue(editor.hasChanges)
        assertTrue(editor.isDirty)

        editor.updateNotes("note")
        assertFalse(editor.hasChanges)
        assertFalse(editor.isDirty)
    }

    @Test
    fun newEntryCanBeSavedAsIsButHasNoChangesToDiscard() {
        val editor = editorWired(FakeGraphQL(listOf(buildJsonObject {})), viewerEntry = null)

        editor.load()

        assertFalse(editor.hasChanges, "closing a pristine add flow must not prompt")
        assertTrue(editor.isDirty, "add-to-list saves straight to Planning")
    }

    @Test
    fun openForCompletionOverrideCountsAsAnUnsavedEdit() {
        val editor = editorWired(
            FakeGraphQL(listOf(buildJsonObject {})),
            existingEntry(),
            initialStatusOverride = MediaListStatus.COMPLETED,
            initialProgressOverride = 12,
        )

        editor.load()

        assertEquals(MediaListStatus.COMPLETED, editor.status)
        assertTrue(editor.hasChanges)
        assertTrue(editor.isDirty)
    }

    @Test
    fun saveSendsAutomatedProgressAndCompletionDate() {
        val saveAck = buildJsonObject {
            put("SaveMediaListEntry", buildJsonObject { put("id", 7) })
        }
        val graphQL = FakeGraphQL(listOf(saveAck))
        val editor = editorWired(graphQL, existingEntry())

        editor.load()
        editor.updateStatus(MediaListStatus.COMPLETED)
        var saved = false
        editor.save { saved = true }

        assertTrue(saved)
        val variables = graphQL.requests.single().variables
        assertEquals("12", variables?.get("progress")?.toString())
        assertTrue(variables?.get("completedAt") != null, "stamped completion date is sent")
    }
}
