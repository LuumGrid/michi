package com.luum.michi.app.mediaDetail.presentation.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.mediaDetail.data.MediaDetailRepository
import com.luum.michi.app.mediaDetail.presentation.model.MediaDetail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class MediaDetailStateHolder(
    private val repository: MediaDetailRepository,
    private val scope: CoroutineScope,
) {
    private var detailState by mutableStateOf<MediaDetail?>(null)
    private var loadingState by mutableStateOf(false)
    private var errorState by mutableStateOf<String?>(null)
    private var currentMediaId: Int? = null
    private var currentJob: Job? = null

    val detail: MediaDetail? get() = detailState
    val isLoading: Boolean get() = loadingState
    val error: String? get() = errorState

    fun load(mediaId: Int) {
        if (currentMediaId == mediaId && (detailState != null || loadingState)) return
        currentMediaId = mediaId
        currentJob?.cancel()
        detailState = null
        errorState = null
        loadingState = true
        currentJob = scope.launch {
            when (val result = repository.loadDetail(mediaId)) {
                is NetworkResult.Success -> detailState = result.value
                is NetworkResult.Failure -> errorState = result.error.toString()
            }
            loadingState = false
        }
    }

    fun refresh() {
        val id = currentMediaId ?: return
        currentJob?.cancel()
        currentJob = scope.launch {
            when (val result = repository.loadDetail(id)) {
                is NetworkResult.Success -> detailState = result.value
                is NetworkResult.Failure -> errorState = result.error.toString()
            }
        }
    }

    fun clear() {
        currentJob?.cancel()
        currentJob = null
        currentMediaId = null
        detailState = null
        loadingState = false
        errorState = null
    }
}

@Composable
internal fun rememberMediaDetailStateHolder(
    repository: MediaDetailRepository,
): MediaDetailStateHolder {
    val scope = rememberCoroutineScope()
    return remember(repository) { MediaDetailStateHolder(repository, scope) }
}
