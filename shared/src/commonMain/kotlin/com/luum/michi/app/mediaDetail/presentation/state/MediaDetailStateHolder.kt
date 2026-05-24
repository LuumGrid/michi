package com.luum.michi.app.mediaDetail.presentation.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.mediaDetail.data.MediaDetailRepository
import com.luum.michi.app.mediaDetail.presentation.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class MediaDetailStateHolder(
    private val repository: MediaDetailRepository,
    private val scope: CoroutineScope,
    private val viewerId: Int,
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

    // New paginated and state resources
    var reviews by mutableStateOf<List<MediaReviewEntry>>(emptyList())
        private set
    var reviewsHasNextPage by mutableStateOf(false)
        private set
    var reviewsCurrentPage by mutableStateOf(1)
        private set
    var isLoadingReviews by mutableStateOf(false)
        private set

    var threads by mutableStateOf<List<MediaThreadEntry>>(emptyList())
        private set
    var threadsHasNextPage by mutableStateOf(false)
        private set
    var threadsCurrentPage by mutableStateOf(1)
        private set
    var isLoadingThreads by mutableStateOf(false)
        private set

    var followingEntries by mutableStateOf<List<MediaFollowingEntry>>(emptyList())
        private set
    var isLoadingFollowing by mutableStateOf(false)
        private set

    var activities by mutableStateOf<List<MediaActivityEntry>>(emptyList())
        private set
    var activitiesHasNextPage by mutableStateOf(false)
        private set
    var activitiesCurrentPage by mutableStateOf(1)
        private set
    var isLoadingActivities by mutableStateOf(false)
        private set
    var activitiesScope by mutableStateOf("Global")
        private set

    var recommendations by mutableStateOf<List<MediaRecommendationEntry>>(emptyList())
        private set
    var isLoadingRecommendations by mutableStateOf(false)
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

        reviews = emptyList()
        reviewsHasNextPage = false
        reviewsCurrentPage = 1
        threads = emptyList()
        threadsHasNextPage = false
        threadsCurrentPage = 1
        followingEntries = emptyList()
        activities = emptyList()
        activitiesHasNextPage = false
        activitiesCurrentPage = 1
        activitiesScope = "Global"
        recommendations = emptyList()

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

    // Load reviews
    fun loadReviews() {
        if (isLoadingReviews || reviews.isNotEmpty()) return
        val id = currentMediaId ?: return
        scope.launch {
            isLoadingReviews = true
            when (val result = repository.loadReviewsPage(id, page = 1)) {
                is NetworkResult.Success -> {
                    reviews = result.value.items
                    reviewsHasNextPage = result.value.hasNextPage
                    reviewsCurrentPage = result.value.currentPage
                }
                is NetworkResult.Failure -> errorState = result.error.toString()
            }
            isLoadingReviews = false
        }
    }

    fun loadMoreReviews() {
        if (isLoadingReviews || !reviewsHasNextPage) return
        val id = currentMediaId ?: return
        val nextPage = reviewsCurrentPage + 1
        scope.launch {
            isLoadingReviews = true
            when (val result = repository.loadReviewsPage(id, page = nextPage)) {
                is NetworkResult.Success -> {
                    reviews = reviews + result.value.items
                    reviewsHasNextPage = result.value.hasNextPage
                    reviewsCurrentPage = result.value.currentPage
                }
                is NetworkResult.Failure -> errorState = result.error.toString()
            }
            isLoadingReviews = false
        }
    }

    // Load threads
    fun loadThreads() {
        if (isLoadingThreads || threads.isNotEmpty()) return
        val id = currentMediaId ?: return
        scope.launch {
            isLoadingThreads = true
            when (val result = repository.loadThreadsPage(id, page = 1)) {
                is NetworkResult.Success -> {
                    threads = result.value.items
                    threadsHasNextPage = result.value.hasNextPage
                    threadsCurrentPage = result.value.currentPage
                }
                is NetworkResult.Failure -> errorState = result.error.toString()
            }
            isLoadingThreads = false
        }
    }

    fun loadMoreThreads() {
        if (isLoadingThreads || !threadsHasNextPage) return
        val id = currentMediaId ?: return
        val nextPage = threadsCurrentPage + 1
        scope.launch {
            isLoadingThreads = true
            when (val result = repository.loadThreadsPage(id, page = nextPage)) {
                is NetworkResult.Success -> {
                    threads = threads + result.value.items
                    threadsHasNextPage = result.value.hasNextPage
                    threadsCurrentPage = result.value.currentPage
                }
                is NetworkResult.Failure -> errorState = result.error.toString()
            }
            isLoadingThreads = false
        }
    }

    // Load following entries
    fun loadFollowing() {
        if (isLoadingFollowing || followingEntries.isNotEmpty()) return
        val id = currentMediaId ?: return
        scope.launch {
            isLoadingFollowing = true
            when (val result = repository.loadFollowingEntries(id)) {
                is NetworkResult.Success -> {
                    followingEntries = result.value
                }
                is NetworkResult.Failure -> errorState = result.error.toString()
            }
            isLoadingFollowing = false
        }
    }

    // Load activities
    fun selectActivitiesScope(scopeName: String) {
        if (activitiesScope == scopeName) return
        activitiesScope = scopeName
        activities = emptyList()
        activitiesHasNextPage = false
        activitiesCurrentPage = 1
        loadMoreActivities(isFirstPage = true)
    }

    fun loadMoreActivities(isFirstPage: Boolean = false) {
        if (isLoadingActivities) return
        if (!isFirstPage && !activitiesHasNextPage) return
        val id = currentMediaId ?: return
        val nextPage = if (isFirstPage) 1 else activitiesCurrentPage + 1
        scope.launch {
            isLoadingActivities = true
            val uId = if (activitiesScope == "Self") viewerId else null
            val isFoll = if (activitiesScope == "Following") true else null
            when (val result = repository.loadActivitiesPage(id, page = nextPage, userId = uId, isFollowing = isFoll)) {
                is NetworkResult.Success -> {
                    activities = if (isFirstPage) result.value.items else activities + result.value.items
                    activitiesHasNextPage = result.value.hasNextPage
                    activitiesCurrentPage = result.value.currentPage
                }
                is NetworkResult.Failure -> errorState = result.error.toString()
            }
            isLoadingActivities = false
        }
    }

    // Load recommendations
    fun loadRecommendations() {
        if (isLoadingRecommendations || recommendations.isNotEmpty()) return
        val id = currentMediaId ?: return
        scope.launch {
            isLoadingRecommendations = true
            when (val result = repository.loadRecommendations(id)) {
                is NetworkResult.Success -> {
                    recommendations = result.value
                }
                is NetworkResult.Failure -> errorState = result.error.toString()
            }
            isLoadingRecommendations = false
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

        reviews = emptyList()
        reviewsHasNextPage = false
        reviewsCurrentPage = 1
        threads = emptyList()
        threadsHasNextPage = false
        threadsCurrentPage = 1
        followingEntries = emptyList()
        activities = emptyList()
        activitiesHasNextPage = false
        activitiesCurrentPage = 1
        activitiesScope = "Global"
        recommendations = emptyList()
    }
}

@Composable
internal fun rememberMediaDetailStateHolder(
    repository: MediaDetailRepository,
    viewerId: Int,
): MediaDetailStateHolder {
    val scope = rememberCoroutineScope()
    return remember(repository, viewerId) { MediaDetailStateHolder(repository, scope, viewerId) }
}
