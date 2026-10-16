package com.luum.michi.app.profile.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.luum.michi.app.profile.domain.ProfileRepository
import com.luum.michi.app.profile.domain.model.ProfileFavorite
import com.luum.michi.app.profile.domain.model.ProfileFavoritesCategory
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.ui.state.PagedList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Paginated state for the "see more" favourites grid screen. All five categories
 * share one [ProfileFavorite] list; a category switch is a new search, so the
 * loaded pages are dropped and a page still in flight never lands (generation).
 */
internal class ProfileFavoritesGridStateHolder(
    private val repository: ProfileRepository,
    private val scope: CoroutineScope,
) {
    private var loadingState by mutableStateOf(false)
    private var errorState by mutableStateOf<NetworkError?>(null)

    private val paginated = PagedList<ProfileFavorite> { a, b -> (a + b).distinctBy { it.id } }

    /** Bumped on every new (userId, category): a page from the previous one never lands. */
    private var generation = 0

    private var loadedUserId: Int? = null
    private var loadedCategory: ProfileFavoritesCategory? = null
    private var loadJob: Job? = null

    val items: List<ProfileFavorite> get() = paginated.items
    val isLoading: Boolean get() = loadingState
    val isLoadingMore: Boolean get() = paginated.isLoadingMore
    val hasNextPage: Boolean get() = paginated.hasNextPage
    val error: NetworkError? get() = errorState

    fun load(userId: Int, category: ProfileFavoritesCategory) {
        if (loadedUserId == userId && loadedCategory == category) return
        loadedUserId = userId
        loadedCategory = category
        loadJob?.cancel()
        generation++
        paginated.clear()
        errorState = null
        loadingState = true
        // LAZY then start, so the identity check below is valid on an eagerly
        // dispatched scope.
        val request = scope.launch(start = CoroutineStart.LAZY) {
            try {
                when (val result = repository.loadFavoritesPage(userId, category, page = 1)) {
                    is NetworkResult.Success -> if (loadJob === coroutineContext[Job]) {
                        paginated.set(result.value)
                    }
                    is NetworkResult.Failure -> if (loadJob === coroutineContext[Job]) {
                        errorState = result.error
                        // A failed first page leaves nothing to page from: forget the
                        // markers so a retry via load() is not swallowed by the early-return.
                        loadedUserId = null
                        loadedCategory = null
                    }
                }
            } finally {
                // Only the current request owns the spinner.
                if (loadJob === coroutineContext[Job]) loadingState = false
            }
        }
        loadJob = request
        request.start()
    }

    fun loadMore() {
        val userId = loadedUserId ?: return
        val category = loadedCategory ?: return
        // A new search is already on its way: page N+1 of it cannot be asked yet.
        if (loadingState) return
        val currentGeneration = generation
        paginated.loadMore(
            scope = scope,
            isCurrent = { generation == currentGeneration },
            fetch = { page -> repository.loadFavoritesPage(userId, category, page) },
            onError = { errorState = it },
        )
    }
}

/** Pure-logic factory (placeholder for UI wiring). */
internal fun createProfileFavoritesGridStateHolder(
    repository: ProfileRepository,
    scope: CoroutineScope,
): ProfileFavoritesGridStateHolder {
    return ProfileFavoritesGridStateHolder(repository, scope)
}
