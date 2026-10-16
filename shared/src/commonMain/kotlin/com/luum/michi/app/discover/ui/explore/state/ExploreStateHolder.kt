package com.luum.michi.app.discover.ui.explore.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
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
import com.luum.michi.app.ui.state.PagedList

internal class ExploreStateHolder(
    private val repository: ExploreRepository,
    private val scope: CoroutineScope,
    /** Read once per request, never cached: a language change localizes the next
     *  search (the `meta` lines come from the repository, which receives it). */
    private val stringsProvider: () -> LanguageStrings,
) {
    var query by mutableStateOf("")
    var category by mutableStateOf(ExploreCategory.ANIME)
    var season by mutableStateOf<String?>(null)
    var genres by mutableStateOf(emptyList<String>())
    var formats by mutableStateOf(emptyList<String>())
    var year by mutableStateOf<Int?>(null)
    var sort by mutableStateOf("TRENDING_DESC")
    var onList by mutableStateOf<Boolean?>(null)
    var hideAdult by mutableStateOf(true)
    var currentSortOption by mutableStateOf(UserListSort.TRENDING)
    var currentSortOrder by mutableStateOf(UserListOrder.DESCENDING)
    var isSortPersisted by mutableStateOf(false)
    var focusSearchRequested by mutableStateOf(false)

    private var loadingState by mutableStateOf(false)
    private var errorState by mutableStateOf<NetworkError?>(null)

    /** Pages of the current search: append/dedup/cancel live in [PagedList]. A
     *  new search invalidates its in-flight page with [generation]. */
    private val paginated = PagedList<ExploreResult> { a, b -> (a + b).distinctBy { it.id } }

    /** Bumped on every new search: a page from a previous one never lands. */
    private var generation = 0

    val results: List<ExploreResult> get() = paginated.items
    val isLoading: Boolean get() = loadingState
    val isLoadingMore: Boolean get() = paginated.isLoadingMore
    val hasNextPage: Boolean get() = paginated.hasNextPage
    val error: NetworkError? get() = errorState

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
        loadJob?.cancel()
        query = ""
        invalidateSearch()
        paginated.clear()
        errorState = null
        loadingState = false
        focusSearchRequested = true
    }

    fun consumeFocusRequest() {
        focusSearchRequested = false
    }

    /** Espejo de las listas: guarda la selección y deriva el MediaSort del API.
     *  La persistencia entre reinicios es solo de sesión en Discover (cada
     *  superficie persiste su propio slot de SortPersistence: anime/manga
     *  no comparten memoria de sort). */
    fun updateSort(option: UserListSort, order: UserListOrder, persist: Boolean) {
        currentSortOption = option
        currentSortOrder = order
        isSortPersisted = persist
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
    private var loadJob: Job? = null

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
        isSortPersisted = false
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
        // Newest load wins: cancel the previous request so a slower (stale)
        // response cannot overwrite newer results. A plain `if (loadingState)
        // return` guard would break the debounced callers above, which preset
        // loadingState before their 300ms delay.
        loadJob?.cancel()
        invalidateSearch()
        loadingState = true
        errorState = null
        // LAZY then start, like PagedList.request: the job is stored before the
        // body runs, so the identity check below is valid even on an eagerly
        // dispatched scope.
        val request = scope.launch(start = CoroutineStart.LAZY) {
            try {
                val result = fetchPage(1)
                if (loadJob === coroutineContext[Job]) {
                    when (result) {
                        is NetworkResult.Success -> paginated.set(result.value)
                        is NetworkResult.Failure -> {
                            errorState = result.error
                            paginated.clear()
                        }
                    }
                }
            } finally {
                // Only the current request owns the spinner: a cancelled load
                // must not turn off the one now in flight.
                if (loadJob === coroutineContext[Job]) loadingState = false
            }
        }
        loadJob = request
        request.start()
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
            onError = { errorState = it },
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

/** Pure-logic factory (placeholder for UI wiring). */
internal fun createExploreStateHolder(
    repository: ExploreRepository,
    scope: CoroutineScope,
    stringsProvider: () -> LanguageStrings,
): ExploreStateHolder {
    return ExploreStateHolder(repository, scope, stringsProvider)
}
