package com.luum.michi.app.mediaDetail.ui.media.state

import com.luum.michi.app.core.domain.network.NetworkError
import com.luum.michi.app.core.domain.network.NetworkResult
import com.luum.michi.app.mediaDetail.domain.media.MediaDetailRepository
import com.luum.michi.app.core.domain.medialist.MediaListEntryRepository
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetail
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetailType
import com.luum.michi.app.core.domain.medialist.MediaListStatus
import com.luum.michi.app.core.domain.language.LanguageStrings
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
) {
    var detail: MediaDetail? = null
        private set
    var isLoadingDetail = true
        private set
    var loadError: NetworkError? = null
        private set

    var status = MediaListStatus.PLANNING
        private set
    var progress = 0
        private set
    var progressVolumes = 0
        private set
    var score = 0f
        private set
    var notes = ""
        private set
    var repeat = 0
        private set
    var priority = 0
        private set
    var isPrivate = false
        private set
    var hiddenFromStatusLists = false
        private set
    var startedAtMillis: Long? = null
        private set
    var completedAtMillis: Long? = null
        private set
    var isFavourite = false
        private set
    var isTogglingFavourite = false
        private set

    var isSaving = false
        private set
    var isDeleting = false
        private set
    var error: NetworkError? = null
        private set

    val isManga: Boolean get() = detail?.type == MediaDetailType.MANGA
    val maxProgress: Int? get() = detail?.let { if (it.type == MediaDetailType.MANGA) it.chapters else it.episodes }
    val maxProgressVolumes: Int? get() = detail?.volumes
    val isExisting: Boolean get() = detail?.viewerEntry != null

    init {
        loadDetail()
    }

    private fun loadDetail() {
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
                    } else {
                        status = initialStatusOverride ?: MediaListStatus.PLANNING
                        progress = initialProgressOverride ?: 0
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

    fun incrementRepeat() { repeat++ }
    fun decrementRepeat() { if (repeat > 0) repeat-- }

    fun incrementPriority() { if (priority < 5) priority++ }
    fun decrementPriority() { if (priority > 0) priority-- }

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
): MediaEntryEditorState {
    return MediaEntryEditorState(
        entryRepository = entryRepository,
        detailRepository = detailRepository,
        scope = scope,
        strings = strings,
        mediaId = mediaId,
        initialStatusOverride = initialStatusOverride,
        initialProgressOverride = initialProgressOverride,
    )
}
