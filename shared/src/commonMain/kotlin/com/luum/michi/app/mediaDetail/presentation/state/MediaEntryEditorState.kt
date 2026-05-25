package com.luum.michi.app.mediaDetail.presentation.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.mediaDetail.data.MediaDetailRepository
import com.luum.michi.app.mediaDetail.data.MediaListEntryRepository
import com.luum.michi.app.mediaDetail.presentation.model.MediaDetail
import com.luum.michi.app.mediaDetail.presentation.model.MediaDetailType
import com.luum.michi.app.mediaDetail.presentation.model.MediaListStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class MediaEntryEditorState(
    private val entryRepository: MediaListEntryRepository,
    private val detailRepository: MediaDetailRepository,
    private val scope: CoroutineScope,
    val mediaId: Int,
    private val initialStatusOverride: MediaListStatus? = null,
    private val initialProgressOverride: Int? = null,
) {
    var detail by mutableStateOf<MediaDetail?>(null)
        private set
    var isLoadingDetail by mutableStateOf(true)
        private set
    var loadError by mutableStateOf<String?>(null)
        private set

    var status by mutableStateOf(MediaListStatus.PLANNING)
        private set
    var progress by mutableStateOf(0)
        private set
    var progressVolumes by mutableStateOf(0)
        private set
    var score by mutableStateOf(0f)
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
    var startedAtMillis by mutableStateOf<Long?>(null)
        private set
    var completedAtMillis by mutableStateOf<Long?>(null)
        private set
    var isFavourite by mutableStateOf(false)
        private set
    var isTogglingFavourite by mutableStateOf(false)
        private set

    var isSaving by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
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
            when (val result = detailRepository.loadDetail(mediaId)) {
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
                is NetworkResult.Failure -> loadError = result.error.toString()
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
                    error = result.error.toString()
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
                is NetworkResult.Failure -> error = result.error.toString()
            }
            isSaving = false
        }
    }
}

@Composable
internal fun rememberMediaEntryEditorState(
    mediaId: Int,
    entryRepository: MediaListEntryRepository,
    detailRepository: MediaDetailRepository,
    initialStatusOverride: MediaListStatus? = null,
    initialProgressOverride: Int? = null,
): MediaEntryEditorState {
    val scope = rememberCoroutineScope()
    return remember(mediaId) {
        MediaEntryEditorState(
            entryRepository = entryRepository,
            detailRepository = detailRepository,
            scope = scope,
            mediaId = mediaId,
            initialStatusOverride = initialStatusOverride,
            initialProgressOverride = initialProgressOverride,
        )
    }
}
