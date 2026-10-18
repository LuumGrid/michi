package com.luum.michi.app.discover.ui.explore.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.time.Duration.Companion.milliseconds
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.model.Page
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.medialist.domain.UserListOrder
import com.luum.michi.app.core.medialist.domain.UserListSort
import com.luum.michi.app.core.media.domain.MediaType
import com.luum.michi.app.core.medialist.domain.toMediaSort
import com.luum.michi.app.discover.domain.ExploreRepository
import com.luum.michi.app.discover.domain.model.ExploreCategory
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
    var focusSearchRequested by mutableStateOf(false)

    /**
     * True once the user submitted a non-empty query: it gates the Explore
     * surface (before it, DISCOVER shows the Dashboard). Cleared by
     * [enterBlankSearch] (empty field or closed search).
     */
    var hasSearched by mutableStateOf(false)
        private set

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

    /** Orden in-app espejo de las listas: solo TITLE/AVERAGE_SCORE/FAVORITES
     *  tienen dato local en ExploreResult; el resto lo ordena el servidor
     *  vía el MediaSort derivado en [updateSort]. */
    val visibleResults: List<ExploreResult>
        get() {
            val ordered = when (currentSortOption) {
                UserListSort.TITLE -> paginated.items.sortedBy { it.title.lowercase() }
                UserListSort.AVERAGE_SCORE,
                UserListSort.SCORE -> paginated.items.sortedBy { it.averageScore ?: -1 }
                UserListSort.FAVORITES -> paginated.items.sortedBy { it.favourites ?: -1 }
                else -> return paginated.items.toList()
            }
            return if (currentSortOrder == UserListOrder.DESCENDING) ordered.reversed() else ordered
        }

    /** Entrada estilo YT: vacía todo (es lo que este caso pide) y da foco al
     *  field, preservando filtros. */
    fun enterBlankSearch() {
        searchJob?.cancel()
        loadRequest.cancel()
        query = ""
        hasSearched = false
        invalidateSearch()
        paginated.clear()
        errorState = null
        actionErrorState = null
        loadingState = false
        focusSearchRequested = true
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
        searchJob?.cancel()
        query = trimmed
        hasSearched = true
        paginated.clear()
        load()
    }

    /**
     * Category chip change. With a submitted search it loads the new category
     * at once (no debounce); the previous results are cleared so the chip and
     * the content never disagree, and the first-load spinner can show.
     */
    fun changeCategory(newCategory: ExploreCategory) {
        if (newCategory == category) return
        category = newCategory
        sort = currentSortOption.toMediaSort(currentSortOrder, isAnime = category != ExploreCategory.MANGA)
        if (!hasSearched) return
        searchJob?.cancel()
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

    fun consumeFocusRequest() {
        focusSearchRequested = false
    }

    /** Espejo de las listas: guarda la selección y deriva el MediaSort del API.
     *  La persistencia entre reinicios es solo de sesión en Discover (cada
     *  superficie persiste su propio slot de SortPersistence: anime/manga
     *  no comparten memoria de sort). */
    fun updateSort(option: UserListSort, order: UserListOrder) {
        currentSortOption = option
        currentSortOrder = order
        sort = option.toMediaSort(order, isAnime = category != ExploreCategory.MANGA)
        loadingState = true
        invalidateSearch()
        searchJob?.cancel()
        searchJob = scope.launch {
            delay(300.milliseconds)
            load()
        }
    }

    private var searchJob: Job? = null
    private val loadRequest = LatestRequest()

    /** Igual que updateFilters + updateSort, pero sin debounce: para navegación
     *  programática ("Ver todo" desde Dashboard), donde la carga debe verse
     *  instantánea en vez de esperar el retraso pensado para tipeo. */
    fun applyPreset(
        category: ExploreCategory,
        sortOption: UserListSort,
        sortOrder: UserListOrder = UserListOrder.DESCENDING,
        season: String? = null,
        year: Int? = null,
    ) {
        searchJob?.cancel()
        query = ""
        this.category = category
        this.season = season
        genres = emptyList()
        formats = emptyList()
        this.year = year
        onList = null
        currentSortOption = sortOption
        currentSortOrder = sortOrder
        sort = sortOption.toMediaSort(sortOrder, isAnime = category != ExploreCategory.MANGA)
        load()
    }

    fun updateFilters(
        newQuery: String = query,
        newCategory: ExploreCategory = category,
        newSeason: String? = season,
        newGenres: List<String> = genres,
        newFormats: List<String> = formats,
        newYear: Int? = year,
        newOnList: Boolean? = onList,
    ) {
        query = newQuery
        category = newCategory
        season = newSeason
        genres = newGenres
        formats = newFormats
        year = newYear
        onList = newOnList
        // A category change flips the derived `isAnime`: recompute from the
        // current selection so a move to manga sends a manga MediaSort.
        sort = currentSortOption.toMediaSort(currentSortOrder, isAnime = category != ExploreCategory.MANGA)
        loadingState = true
        invalidateSearch()
        searchJob?.cancel()
        searchJob = scope.launch {
            delay(300.milliseconds)
            load()
        }
    }

    fun load() {
        // Newest load wins: a slower (stale) response cannot overwrite newer
        // results; the helper cancels the previous request. A plain `if
        // (loadingState) return` guard would break the debounced callers above,
        // which preset loadingState before their 300ms delay.
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
        // A new search presets loadingState before its debounce fires; asking
        // for page N+1 of the new filters against the old items would be wrong.
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
