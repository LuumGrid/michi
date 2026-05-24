package com.luum.michi.app.mediaDetail.presentation.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.mediaDetail.data.MediaDetailRepository
import com.luum.michi.app.mediaDetail.presentation.model.MediaCharacterEntry
import com.luum.michi.app.mediaDetail.presentation.model.MediaDetail
import com.luum.michi.app.mediaDetail.presentation.model.MediaStaffEntry
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

    var voiceLanguage by mutableStateOf("JAPANESE")
        private set
    var characters by mutableStateOf<List<MediaCharacterEntry>>(emptyList())
        private set
    var charactersHasNextPage by mutableStateOf(false)
        private set
    var charactersCurrentPage by mutableStateOf(1)
        private set
    var isLoadingCharacters by mutableStateOf(false)
        private set

    var staff by mutableStateOf<List<MediaStaffEntry>>(emptyList())
        private set
    var staffHasNextPage by mutableStateOf(false)
        private set
    var staffCurrentPage by mutableStateOf(1)
        private set
    var isLoadingStaff by mutableStateOf(false)
        private set

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
        characters = emptyList()
        staff = emptyList()
        charactersHasNextPage = false
        staffHasNextPage = false
        charactersCurrentPage = 1
        staffCurrentPage = 1
        currentJob = scope.launch {
            when (val result = repository.loadDetail(mediaId, voiceLanguage)) {
                is NetworkResult.Success -> {
                    detailState = result.value
                    characters = result.value.characters.items
                    charactersHasNextPage = result.value.characters.hasNextPage
                    charactersCurrentPage = result.value.characters.currentPage
                    staff = result.value.staff.items
                    staffHasNextPage = result.value.staff.hasNextPage
                    staffCurrentPage = result.value.staff.currentPage
                }
                is NetworkResult.Failure -> errorState = result.error.toString()
            }
            loadingState = false
        }
    }

    fun refresh() {
        val id = currentMediaId ?: return
        currentJob?.cancel()
        currentJob = scope.launch {
            when (val result = repository.loadDetail(id, voiceLanguage)) {
                is NetworkResult.Success -> {
                    detailState = result.value
                    characters = result.value.characters.items
                    charactersHasNextPage = result.value.characters.hasNextPage
                    charactersCurrentPage = result.value.characters.currentPage
                    staff = result.value.staff.items
                    staffHasNextPage = result.value.staff.hasNextPage
                    staffCurrentPage = result.value.staff.currentPage
                }
                is NetworkResult.Failure -> errorState = result.error.toString()
            }
        }
    }

    fun selectVoiceLanguage(language: String) {
        if (language == voiceLanguage) return
        voiceLanguage = language
        val id = currentMediaId ?: return
        scope.launch {
            isLoadingCharacters = true
            when (val result = repository.loadCharactersPage(id, page = 1, voiceLanguage = language)) {
                is NetworkResult.Success -> {
                    characters = result.value.items
                    charactersHasNextPage = result.value.hasNextPage
                    charactersCurrentPage = result.value.currentPage
                }
                is NetworkResult.Failure -> errorState = result.error.toString()
            }
            isLoadingCharacters = false
        }
    }

    fun loadMoreCharacters() {
        if (isLoadingCharacters || !charactersHasNextPage) return
        val id = currentMediaId ?: return
        val nextPage = charactersCurrentPage + 1
        scope.launch {
            isLoadingCharacters = true
            when (val result = repository.loadCharactersPage(id, page = nextPage, voiceLanguage = voiceLanguage)) {
                is NetworkResult.Success -> {
                    characters = characters + result.value.items
                    charactersHasNextPage = result.value.hasNextPage
                    charactersCurrentPage = result.value.currentPage
                }
                is NetworkResult.Failure -> errorState = result.error.toString()
            }
            isLoadingCharacters = false
        }
    }

    fun loadMoreStaff() {
        if (isLoadingStaff || !staffHasNextPage) return
        val id = currentMediaId ?: return
        val nextPage = staffCurrentPage + 1
        scope.launch {
            isLoadingStaff = true
            when (val result = repository.loadStaffPage(id, page = nextPage)) {
                is NetworkResult.Success -> {
                    staff = staff + result.value.items
                    staffHasNextPage = result.value.hasNextPage
                    staffCurrentPage = result.value.currentPage
                }
                is NetworkResult.Failure -> errorState = result.error.toString()
            }
            isLoadingStaff = false
        }
    }

    fun clear() {
        currentJob?.cancel()
        currentJob = null
        currentMediaId = null
        detailState = null
        loadingState = false
        errorState = null
        characters = emptyList()
        staff = emptyList()
        charactersHasNextPage = false
        staffHasNextPage = false
        charactersCurrentPage = 1
        staffCurrentPage = 1
    }
}

@Composable
internal fun rememberMediaDetailStateHolder(
    repository: MediaDetailRepository,
): MediaDetailStateHolder {
    val scope = rememberCoroutineScope()
    return remember(repository) { MediaDetailStateHolder(repository, scope) }
}
