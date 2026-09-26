package com.luum.michi.app.mediaDetail.ui.media.state

import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.mediaDetail.domain.media.MediaDetailRepository
import com.luum.michi.app.core.medialist.domain.MediaListEntryRepository
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetail
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetailType
import com.luum.michi.app.core.medialist.domain.MediaListStatus
import com.luum.michi.app.core.language.domain.LanguageStrings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class MediaEntryEditorState(
    private val entryRepository: MediaListEntryRepository,
    private val detailRepository: MediaDetailRepository,
    private val scope: CoroutineScope,
    private val strings: LanguageStrings,
    val mediaId: Int,
    private val initialStatusOverride: MediaListStatus? = null,
    private val initialProgressOverride: Int? = null,
    private val advancedScoringEnabled: Boolean = false,
    private val advancedScoringAnimeNames: List<String> = emptyList(),
    private val advancedScoringMangaNames: List<String> = emptyList(),
) {
    var detail: MediaDetail? by mutableStateOf(null)
        private set
    var isLoadingDetail by mutableStateOf(true)
        private set
    var loadError: NetworkError? by mutableStateOf(null)
        private set

    var status by mutableStateOf(MediaListStatus.PLANNING)
        private set
    var progress by mutableStateOf(0)
        private set
    var progressVolumes by mutableStateOf(0)
        private set
    var score by mutableStateOf(0f)
        private set
    var advancedScoreValues by mutableStateOf(emptyList<Float>())
        private set
    var notes by mutableStateOf("")
        private set
    var repeat by mutableStateOf(0)
        private set
    var priority by mutableStateOf(0)
        private set
    var isPrivate by mutableStateOf(false)
        private set
    var hiddenFromStatusLists by mutableStateOf(false)
        private set
    var startedAtMillis: Long? by mutableStateOf(null)
        private set
    var completedAtMillis: Long? by mutableStateOf(null)
        private set
    var isFavourite by mutableStateOf(false)
        private set
    var isTogglingFavourite by mutableStateOf(false)
        private set

    var isSaving by mutableStateOf(false)
        private set
    var isDeleting by mutableStateOf(false)
        private set
    var error: NetworkError? by mutableStateOf(null)
        private set

    val isManga: Boolean get() = detail?.type == MediaDetailType.MANGA
    val advancedScoringNames: List<String>
        get() = if (isManga) advancedScoringMangaNames else advancedScoringAnimeNames
    val showAdvancedScoring: Boolean
        get() = advancedScoringEnabled && advancedScoringNames.isNotEmpty()
    val maxProgress: Int? get() = detail?.let { if (it.type == MediaDetailType.MANGA) it.chapters else it.episodes }
    val maxProgressVolumes: Int? get() = detail?.volumes
    val isExisting: Boolean get() = detail?.viewerEntry != null

    /**
     * Explicit load (no auto-load in init): Root calls this from a
     * LaunchedEffect when the editor opens; retries call it again.
     */
    fun load() {
        isLoadingDetail = true
        loadError = null
        scope.launch {
            when (val result = detailRepository.loadDetail(mediaId, strings = strings)) {
                is NetworkResult.Success -> {
                    detail = result.value
                    isFavourite = result.value.isFavourite
                    val existing = result.value.viewerEntry
                    if (existing != null) {
                        status = initialStatusOverride ?: (existing.status ?: MediaListStatus.CURRENT)
                        progress = initialProgressOverride ?: existing.progress
                        progressVolumes = existing.progressVolumes ?: 0
                        score = existing.score
                        notes = existing.notes
                        repeat = existing.repeat
                        priority = existing.priority
                        isPrivate = existing.isPrivate
                        hiddenFromStatusLists = existing.hiddenFromStatusLists
                        startedAtMillis = existing.startedAtMillis
                        completedAtMillis = existing.completedAtMillis
                        advancedScoreValues = advancedScoringNames.map { name ->
                            existing.advancedScores[name] ?: 0f
                        }
                    } else {
                        status = initialStatusOverride ?: MediaListStatus.PLANNING
                        progress = initialProgressOverride ?: 0
                        advancedScoreValues = List(advancedScoringNames.size) { 0f }
                    }
                }
                is NetworkResult.Failure -> loadError = result.error
            }
            isLoadingDetail = false
        }
    }

    fun updateStatus(value: MediaListStatus) { status = value }

    fun updateProgress(value: Int) {
        val max = maxProgress
        progress = if (max != null) value.coerceIn(0, max) else value.coerceAtLeast(0)
    }
    fun incrementProgress() = updateProgress(progress + 1)
    fun decrementProgress() = updateProgress(progress - 1)

    fun updateProgressVolumes(value: Int) {
        val max = maxProgressVolumes
        progressVolumes = if (max != null) value.coerceIn(0, max) else value.coerceAtLeast(0)
    }
    fun incrementProgressVolumes() = updateProgressVolumes(progressVolumes + 1)
    fun decrementProgressVolumes() = updateProgressVolumes(progressVolumes - 1)

    fun updateScore(value: Float) { score = value.coerceIn(0f, 10f) }
    fun updateNotes(value: String) { notes = value }

    fun updateAdvancedScore(index: Int, value: Float) {
        val names = advancedScoringNames
        if (index !in names.indices) return
        advancedScoreValues = advancedScoreValues.alignTo(names.size).toMutableList().also {
            it[index] = value.coerceIn(0f, 100f)
        }
    }

    fun incrementRepeat() { repeat++ }
    fun decrementRepeat() { if (repeat > 0) repeat-- }
    fun updateRepeat(value: Int) { repeat = value.coerceAtLeast(0) }

    fun incrementPriority() { if (priority < 5) priority++ }
    fun decrementPriority() { if (priority > 0) priority-- }
    fun updatePriority(value: Int) { priority = value.coerceIn(0, 5) }

    fun updatePrivate(value: Boolean) { isPrivate = value }
    fun updateHiddenFromStatusLists(value: Boolean) { hiddenFromStatusLists = value }
    fun updateStartedAt(value: Long?) { startedAtMillis = value }
    fun updateCompletedAt(value: Long?) { completedAtMillis = value }

    fun toggleFavourite() {
        if (isTogglingFavourite || isLoadingDetail) return
        val previous = isFavourite
        isFavourite = !previous
        isTogglingFavourite = true
        scope.launch {
            when (val result = entryRepository.toggleFavourite(mediaId, isManga)) {
                is NetworkResult.Success -> {}
                is NetworkResult.Failure -> {
                    isFavourite = previous
                    error = result.error
                }
            }
            isTogglingFavourite = false
        }
    }

    fun save(onSaved: () -> Unit) {
        if (isSaving || isLoadingDetail) return
        isSaving = true
        error = null
        scope.launch {
            val result = entryRepository.saveEntry(
                mediaId = mediaId,
                status = status,
                progress = progress,
                progressVolumes = if (isManga) progressVolumes else null,
                score = score,
                advancedScores = if (showAdvancedScoring) {
                    advancedScoreValues.alignTo(advancedScoringNames.size)
                } else {
                    null
                },
                notes = notes,
                repeat = repeat,
                priority = priority,
                isPrivate = isPrivate,
                hiddenFromStatusLists = hiddenFromStatusLists,
                startedAtMillis = startedAtMillis,
                completedAtMillis = completedAtMillis,
            )
            when (result) {
                is NetworkResult.Success -> onSaved()
                is NetworkResult.Failure -> error = result.error
            }
            isSaving = false
        }
    }

    fun delete(onDeleted: () -> Unit) {
        if (isDeleting || isSaving || isLoadingDetail) return
        val entryId = detail?.viewerEntry?.id
        if (entryId == null || entryId == 0) return
        isDeleting = true
        error = null
        scope.launch {
            when (val result = entryRepository.deleteEntry(entryId)) {
                is NetworkResult.Success -> onDeleted()
                is NetworkResult.Failure -> error = result.error
            }
            isDeleting = false
        }
    }
}

/** Pure-logic factory (placeholder for UI wiring). */
internal fun createMediaEntryEditorState(
    mediaId: Int,
    entryRepository: MediaListEntryRepository,
    detailRepository: MediaDetailRepository,
    scope: CoroutineScope,
    strings: LanguageStrings,
    initialStatusOverride: MediaListStatus? = null,
    initialProgressOverride: Int? = null,
    advancedScoringEnabled: Boolean = false,
    advancedScoringAnimeNames: List<String> = emptyList(),
    advancedScoringMangaNames: List<String> = emptyList(),
): MediaEntryEditorState {
    return MediaEntryEditorState(
        entryRepository = entryRepository,
        detailRepository = detailRepository,
        scope = scope,
        strings = strings,
        mediaId = mediaId,
        initialStatusOverride = initialStatusOverride,
        initialProgressOverride = initialProgressOverride,
        advancedScoringEnabled = advancedScoringEnabled,
        advancedScoringAnimeNames = advancedScoringAnimeNames,
        advancedScoringMangaNames = advancedScoringMangaNames,
    )
}

/** Pads with 0 and truncates to the category count: server arrays may be shorter or stale-long. */
private fun List<Float>.alignTo(size: Int): List<Float> {
    if (size <= 0) return emptyList()
    return List(size) { index -> getOrNull(index) ?: 0f }
}
