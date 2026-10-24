package com.luum.michi.app.discover.ui.explore.state

import kotlinx.coroutines.CoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.model.Page
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.medialist.domain.UserListOrder
import com.luum.michi.app.core.medialist.domain.UserListSort
import com.luum.michi.app.core.media.domain.MediaType
import com.luum.michi.app.core.media.domain.mediaFormatsForType
import com.luum.michi.app.core.medialist.domain.toMediaSort
import com.luum.michi.app.discover.domain.ExploreRepository
import com.luum.michi.app.discover.domain.model.ExploreCategory
import com.luum.michi.app.discover.domain.model.ExploreFilters
import com.luum.michi.app.discover.domain.model.ExploreResult
import com.luum.michi.app.ui.state.LatestRequest
import com.luum.michi.app.ui.state.PagedList

internal class ExploreStateHolder(
    private val repository: ExploreRepository,
    private val scope: CoroutineScope,
    /** Read once per request, never cached: a language change localizes the next
     *  search (the `meta` lines come from the repository, which receives it). */
    private val stringsProvider: () -> LanguageStrings,
    initialHideAdult: Boolean = true,
) {
    var query by mutableStateOf("")
    var category by mutableStateOf(ExploreCategory.ANIME)
    var season by mutableStateOf<String?>(null)
    var genres by mutableStateOf(emptyList<String>())
    var formats by mutableStateOf(emptyList<String>())
    var year by mutableStateOf<Int?>(null)
    var sort by mutableStateOf("TRENDING_DESC")
    var onList by mutableStateOf<Boolean?>(null)
    var hideAdult by mutableStateOf(initialHideAdult)
    var currentSortOption by mutableStateOf(UserListSort.TRENDING)
    var currentSortOrder by mutableStateOf(UserListOrder.DESCENDING)

    /**
     * True once the user submitted a non-empty query: it gates the Explore
     * surface (before it, DISCOVER shows the Dashboard). Cleared by
     * [enterBlankSearch] (empty field or closed search).
     */
    var hasSearched by mutableStateOf(false)
        private set

    /** The filters currently applied to the search (what the sheet starts from). */
    val appliedFilters: ExploreFilters
        get() = ExploreFilters(season = season, genres = genres, formats = formats, year = year, onList = onList)

    private var loadingState by mutableStateOf(false)
    private var errorState by mutableStateOf<NetworkError?>(null)
    /** Non-blocking failure of a page load, shown as a compact row. */
    private var actionErrorState by mutableStateOf<NetworkError?>(null)

    /** Pages of the current search: append/dedup/cancel live in [PagedList]. A
     *  new search invalidates its in-flight page with [generation]. */
    private val paginated = PagedList<ExploreResult> { a, b -> (a + b).distinctBy { it.id } }

    /** Bumped on every new search: a page from a previous one never lands. */
    private var generation = 0

    val results: List<ExploreResult> get() = paginated.items
    val isLoading: Boolean get() = loadingState
    val isLoadingMore: Boolean get() = paginated.isLoadingMore
    val hasNextPage: Boolean get() = paginated.hasNextPage
    /** Current page of the results, the auto-paging effect's key. */
    val currentPage: Int get() = paginated.currentPage
    val error: NetworkError? get() = errorState
    /** A failed page load (2+): it must not replace the loaded results with the
     *  blocking error panel. Dismissible. */
    val actionError: NetworkError? get() = actionErrorState

    fun clearActionError() {
        actionErrorState = null
    }

    /** Entrada estilo YT: vacía todo (es lo que este caso pide), preservando filtros. */
    fun enterBlankSearch() {
        loadRequest.cancel()
        query = ""
        hasSearched = false
        invalidateSearch()
        paginated.clear()
        errorState = null
        actionErrorState = null
        loadingState = false
    }

    /**
     * Submits a query (the keyboard Search action). Explore never loads while
     * typing: only this sets the query and fetches, so the budget is spent on
     * intent. A blank submit is a no-op; re-submitting the same query for the
     * same category while a result is already shown (or loading) is deduped.
     * A different query clears the previous results so the first-load spinner
     * (and its AniList wait notice) can show.
     */
    fun submitSearch(newQuery: String) {
        val trimmed = newQuery.trim()
        if (trimmed.isEmpty()) return
        if (hasSearched && trimmed == query &&
            (paginated.items.isNotEmpty() || loadingState)
        ) {
            return
        }
        query = trimmed
        hasSearched = true
        paginated.clear()
        load()
    }

    /**
     * Category chip change. With a submitted search it loads the new category
     * at once; the previous results are cleared so the chip and the content
     * never disagree. Season is an anime-only filter, so leaving anime drops it
     * while the filters that apply to both catalogs stay.
     */
    fun changeCategory(newCategory: ExploreCategory) {
        if (newCategory == category) return
        category = newCategory
        if (newCategory != ExploreCategory.ANIME) season = null
        // Formats are type-specific (anime has no Manga/Novel/One shot and manga
        // has no TV/Movie/…): drop every format the new category cannot have, and
        // clear them entirely for the people/studio categories that take none.
        val isAnime = newCategory == ExploreCategory.ANIME
        val isMedia = isAnime || newCategory == ExploreCategory.MANGA
        formats = if (isMedia) {
            val valid = mediaFormatsForType(isAnime).mapTo(mutableSetOf()) { it.name }
            formats.filter { it in valid }
        } else {
            emptyList()
        }
        sort = currentSortOption.toMediaSort(currentSortOrder, isAnime = category != ExploreCategory.MANGA)
        if (!hasSearched) return
        paginated.clear()
        load()
    }

    /**
     * 18+ gate: flipping it re-runs the last submitted query when Explore is
     * visible (never the text left in the field), so the setting applies
     * without leaving the results.
     */
    fun updateHideAdult(value: Boolean) {
        if (value == hideAdult) return
        hideAdult = value
        if (hasSearched) load()
    }

    /** Applies the sheet's draft: no-op when nothing changed, otherwise clears
     *  the old results (so the first-load spinner can show) and reloads. */
    fun applyFilters(filters: ExploreFilters) {
        if (filters == appliedFilters) return
        season = filters.season
        genres = filters.genres
        formats = filters.formats
        year = filters.year
        onList = filters.onList
        sort = currentSortOption.toMediaSort(currentSortOrder, isAnime = category != ExploreCategory.MANGA)
        if (!hasSearched) return
        paginated.clear()
        load()
    }

    /** Sort change: server order, so a single request re-queries from page 1.
     *  Picking what is already active is a no-op. */
    fun updateSort(option: UserListSort, order: UserListOrder) {
        if (option == currentSortOption && order == currentSortOrder) return
        currentSortOption = option
        currentSortOrder = order
        sort = option.toMediaSort(order, isAnime = category != ExploreCategory.MANGA)
        if (!hasSearched) return
        paginated.clear()
        load()
    }

    private val loadRequest = LatestRequest()

    fun load() {
        // Newest load wins: a slower (stale) response cannot overwrite newer
        // results; the helper cancels the previous request.
        invalidateSearch()
        loadingState = true
        errorState = null
        actionErrorState = null
        loadRequest.launch(
            scope = scope,
            onSettled = { loadingState = false },
        ) { isCurrent ->
            when (val result = fetchPage(1)) {
                is NetworkResult.Success -> if (isCurrent()) paginated.set(result.value)
                is NetworkResult.Failure -> if (isCurrent()) {
                    errorState = result.error
                    paginated.clear()
                }
            }
        }
    }

    fun loadMore() {
        // While a page 1 is in flight (a new search just cleared the results),
        // asking for page N+1 of the old items would be wrong.
        if (loadingState) return
        val currentGeneration = generation
        paginated.loadMore(
            scope = scope,
            isCurrent = { generation == currentGeneration },
            fetch = { page -> fetchPage(page) },
            onLoaded = { actionErrorState = null },
            onError = { actionErrorState = it },
        )
    }

    /**
     * A new search supersedes the running one: an in-flight page (and its
     * spinner) belongs to the old query and is dropped. Items stay on screen
     * while typing — only [enterBlankSearch] empties them, and a successful
     * page 1 replaces them via [PagedList.set].
     */
    private fun invalidateSearch() {
        generation++
        paginated.cancel()
    }

    private suspend fun fetchPage(page: Int): NetworkResult<Page<ExploreResult>> {
        // One language per request: the provider is read once, no mixing.
        val strings = stringsProvider()
        return when (category) {
            ExploreCategory.ANIME -> repository.searchMedia(
                type = MediaType.ANIME,
                query = query.takeIf { it.isNotBlank() },
                genres = genres,
                formats = formats,
                year = year,
                sort = sort,
                page = page,
                season = season,
                onList = onList,
                hideAdult = hideAdult,
                strings = strings,
            )
            ExploreCategory.MANGA -> repository.searchMedia(
                type = MediaType.MANGA,
                query = query.takeIf { it.isNotBlank() },
                genres = genres,
                formats = formats,
                year = year,
                sort = sort,
                page = page,
                onList = onList,
                hideAdult = hideAdult,
                strings = strings,
            )
            ExploreCategory.CHARACTERS -> repository.searchCharacters(
                query = query.takeIf { it.isNotBlank() },
                page = page,
                strings = strings,
            )
            ExploreCategory.STAFF -> repository.searchStaff(
                query = query.takeIf { it.isNotBlank() },
                page = page,
                strings = strings,
            )
            ExploreCategory.STUDIOS -> repository.searchStudios(
                query = query.takeIf { it.isNotBlank() },
                page = page,
                strings = strings,
            )
        }
    }
}

/** Pure-logic factory used by RootHolders. */
internal fun createExploreStateHolder(
    repository: ExploreRepository,
    scope: CoroutineScope,
    stringsProvider: () -> LanguageStrings,
    initialHideAdult: Boolean,
): ExploreStateHolder {
    return ExploreStateHolder(repository, scope, stringsProvider, initialHideAdult)
}
