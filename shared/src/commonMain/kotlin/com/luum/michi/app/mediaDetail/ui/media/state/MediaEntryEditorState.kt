package com.luum.michi.app.mediaDetail.ui.media.state

import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.mediaDetail.domain.media.MediaDetailRepository
import com.luum.michi.app.core.medialist.domain.MediaListEntryRepository
import com.luum.michi.app.mediaDetail.domain.media.model.MediaEntryEditorData
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetailType
import com.luum.michi.app.core.medialist.domain.MediaListStatus
import com.luum.michi.app.core.model.todayCalendarMillis
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class MediaEntryEditorState(
    private val entryRepository: MediaListEntryRepository,
    private val detailRepository: MediaDetailRepository,
    private val scope: CoroutineScope,
    val mediaId: Int,
    private val initialStatusOverride: MediaListStatus? = null,
    private val initialProgressOverride: Int? = null,
    private val advancedScoringEnabled: Boolean = false,
    private val advancedScoringAnimeNames: List<String> = emptyList(),
    private val advancedScoringMangaNames: List<String> = emptyList(),
    private val scoreMax: Float = 10f,
    // Tells the detail holder a favourite toggled here, so closing the editor
    // never leaves the detail page's heart stale.
    private val onFavouriteChanged: (mediaId: Int, value: Boolean) -> Unit = { _, _ -> },
    // Injectable so status automations (started/completed = today) are
    // deterministic in tests; same UTC-midnight millis the date pickers speak.
    private val todayMillis: () -> Long = { todayCalendarMillis() },
) {
    var data: MediaEntryEditorData? by mutableStateOf(null)
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

    /** Save/delete share one guard: neither can run while the other is in flight. */
    val isBusy: Boolean get() = isSaving || isDeleting

    val isManga: Boolean get() = data?.type == MediaDetailType.MANGA
    val advancedScoringNames: List<String>
        get() = if (isManga) advancedScoringMangaNames else advancedScoringAnimeNames
    val showAdvancedScoring: Boolean
        get() = advancedScoringEnabled && advancedScoringNames.isNotEmpty()
    val maxProgress: Int? get() = data?.let { if (it.type == MediaDetailType.MANGA) it.chapters else it.episodes }
    val maxProgressVolumes: Int? get() = data?.volumes
    val isExisting: Boolean get() = data?.viewerEntry != null

    /** Editable fields as loaded (or as defaults for a new entry): the dirty-tracking baseline. */
    private var baseline: EditorSnapshot? by mutableStateOf(null)

    /** Any editable field differs from what was loaded. Drives the discard prompt. */
    val hasChanges: Boolean get() = baseline?.let { it != snapshot() } ?: false

    /**
     * Save gate: an existing entry needs a real edit; a new entry (the user
     * pressed "add to list") can always be saved as-is, e.g. straight to Planning.
     */
    val isDirty: Boolean get() = data != null && (!isExisting || hasChanges)

    /**
     * Explicit load (no auto-load in init): Root calls this from a
     * LaunchedEffect when the editor opens; retries call it again.
     */
    fun load() {
        isLoadingDetail = true
        loadError = null
        scope.launch {
            when (val result = detailRepository.loadEditorData(mediaId)) {
                is NetworkResult.Success -> {
                    data = result.value
                    isFavourite = result.value.isFavourite
                    val existing = result.value.viewerEntry
                    if (existing != null) {
                        status = existing.status ?: MediaListStatus.CURRENT
                        progress = existing.progress
                        progressVolumes = existing.progressVolumes ?: 0
                        score = existing.score
                        notes = existing.notes
                        repeat = existing.repeat
                        priority = existing.priority
                        isPrivate = existing.isPrivate
                        hiddenFromStatusLists = existing.hiddenFromStatusLists
                        startedAtMillis = existing.startedAtMillis
                        completedAtMillis = existing.completedAtMillis
                        // Server advanced scores are raw 0–100, independent of
                        // the user's score format (proven: format changes move
                        // the main score but never the advanced map). Internal
                        // draft stays 0–10; display/parsing go through the
                        // score-format lambdas, save scales back up.
                        advancedScoreValues = advancedScoringNames.map { name ->
                            ((existing.advancedScores[name] ?: 0f) / 10f).coerceIn(0f, 10f)
                        }
                        // Baseline is the stored entry: a status/progress override
                        // (e.g. open-for-completion) is an unsaved edit, so Save
                        // stays enabled for it.
                        baseline = snapshot()
                        // "Completar" flows through the same automation as picking
                        // it in the sheet: fill the totals and stamp the end date.
                        if (initialStatusOverride != null) {
                            initialProgressOverride?.let { progress = it }
                            updateStatus(initialStatusOverride)
                        }
                    } else {
                        status = MediaListStatus.PLANNING
                        progress = initialProgressOverride ?: 0
                        advancedScoreValues = List(advancedScoringNames.size) { 0f }
                        // Baseline is the untouched default (a new entry is always
                        // dirty); the override then flows through the same
                        // automation as picking it in the sheet, so COMPLETED
                        // fills the totals and stamps the end date and CURRENT
                        // stamps the start date.
                        baseline = snapshot()
                        initialStatusOverride?.let { updateStatus(it) }
                    }
                }
                is NetworkResult.Failure -> loadError = result.error
            }
            isLoadingDetail = false
        }
    }

    /**
     * Status change plus the small conveniences every list client has:
     * Completed fills progress to the known total (volumes too for manga)
     * and stamps today as the completion date; Current stamps today as the
     * start date. Dates already set are never overwritten.
     */
    fun updateStatus(value: MediaListStatus) {
        status = value
        when (value) {
            MediaListStatus.COMPLETED -> {
                maxProgress?.let { progress = it }
                if (isManga) maxProgressVolumes?.let { progressVolumes = it }
                if (completedAtMillis == null) completedAtMillis = todayMillis()
            }
            MediaListStatus.CURRENT -> {
                if (startedAtMillis == null) startedAtMillis = todayMillis()
            }
            else -> {}
        }
    }

    fun updateProgress(value: Int) {
        val max = maxProgress
        progress = if (max != null) value.coerceIn(0, max) else value.coerceAtLeast(0)
    }
    fun incrementProgress() = updateProgress(progress + 1)

    fun updateProgressVolumes(value: Int) {
        val max = maxProgressVolumes
        progressVolumes = if (max != null) value.coerceIn(0, max) else value.coerceAtLeast(0)
    }
    fun incrementProgressVolumes() = updateProgressVolumes(progressVolumes + 1)

    fun updateScore(value: Float) { score = value.coerceIn(0f, scoreMax) }
    fun updateNotes(value: String) { notes = value }

    fun updateAdvancedScore(index: Int, value: Float) {
        val names = advancedScoringNames
        if (index !in names.indices) return
        advancedScoreValues = advancedScoreValues.alignTo(names.size).toMutableList().also {
            it[index] = value.coerceIn(0f, 10f)
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

    private fun snapshot() = EditorSnapshot(
        status = status,
        progress = progress,
        progressVolumes = progressVolumes,
        score = score,
        advancedScoreValues = advancedScoreValues,
        notes = notes,
        repeat = repeat,
        priority = priority,
        isPrivate = isPrivate,
        hiddenFromStatusLists = hiddenFromStatusLists,
        startedAtMillis = startedAtMillis,
        completedAtMillis = completedAtMillis,
    )

    fun toggleFavourite() {
        if (isTogglingFavourite || isLoadingDetail || data == null) return
        val previous = isFavourite
        val target = !previous
        isFavourite = target
        isTogglingFavourite = true
        scope.launch {
            when (val result = entryRepository.toggleFavourite(mediaId, isManga)) {
                is NetworkResult.Success -> onFavouriteChanged(mediaId, target)
                is NetworkResult.Failure -> {
                    isFavourite = previous
                    error = result.error
                }
            }
            isTogglingFavourite = false
        }
    }

    fun save(onSaved: () -> Unit) {
        if (isBusy || data == null) return
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
                    advancedScoreValues.alignTo(advancedScoringNames.size).map { it * 10f }
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
        if (isBusy || data == null) return
        val entryId = data?.viewerEntry?.id
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

    /** Clears the last save/delete error (e.g. before opening the delete confirm,
     *  so a previous save failure never shows up on the delete sheet). */
    fun clearError() {
        error = null
    }
}

/** Value snapshot of every editable field (favourite toggles instantly and is not part of it). */
private data class EditorSnapshot(
    val status: MediaListStatus,
    val progress: Int,
    val progressVolumes: Int,
    val score: Float,
    val advancedScoreValues: List<Float>,
    val notes: String,
    val repeat: Int,
    val priority: Int,
    val isPrivate: Boolean,
    val hiddenFromStatusLists: Boolean,
    val startedAtMillis: Long?,
    val completedAtMillis: Long?,
)

/** Pure-logic factory (placeholder for UI wiring). */
internal fun createMediaEntryEditorState(
    mediaId: Int,
    entryRepository: MediaListEntryRepository,
    detailRepository: MediaDetailRepository,
    scope: CoroutineScope,
    initialStatusOverride: MediaListStatus? = null,
    initialProgressOverride: Int? = null,
    advancedScoringEnabled: Boolean = false,
    advancedScoringAnimeNames: List<String> = emptyList(),
    advancedScoringMangaNames: List<String> = emptyList(),
    scoreMax: Float = 10f,
    onFavouriteChanged: (mediaId: Int, value: Boolean) -> Unit = { _, _ -> },
    todayMillis: () -> Long = { todayCalendarMillis() },
): MediaEntryEditorState {
    return MediaEntryEditorState(
        entryRepository = entryRepository,
        detailRepository = detailRepository,
        scope = scope,
        mediaId = mediaId,
        initialStatusOverride = initialStatusOverride,
        initialProgressOverride = initialProgressOverride,
        advancedScoringEnabled = advancedScoringEnabled,
        advancedScoringAnimeNames = advancedScoringAnimeNames,
        advancedScoringMangaNames = advancedScoringMangaNames,
        scoreMax = scoreMax,
        onFavouriteChanged = onFavouriteChanged,
        todayMillis = todayMillis,
    )
}

/** Pads with 0 and truncates to the category count: server arrays may be shorter or stale-long. */
private fun List<Float>.alignTo(size: Int): List<Float> {
    if (size <= 0) return emptyList()
    return List(size) { index -> getOrNull(index) ?: 0f }
}
