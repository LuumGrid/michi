package com.luum.michi.app.discover.ui.explore.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.time.Duration.Companion.milliseconds
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.model.UserListOrder
import com.luum.michi.app.core.model.UserListSort
import com.luum.michi.app.core.model.toMediaSort
import com.luum.michi.app.discover.domain.ExploreRepository
import com.luum.michi.app.discover.domain.model.ExploreCategory
import com.luum.michi.app.discover.domain.model.ExploreResult

internal class ExploreStateHolder(
    private val repository: ExploreRepository,
    private val scope: CoroutineScope,
    private val strings: LanguageStrings,
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

    private val resultsBacking = mutableStateListOf<ExploreResult>()
    private var loadingState by mutableStateOf(false)
    private var loadingMoreState by mutableStateOf(false)
    private var errorState by mutableStateOf<NetworkError?>(null)
    private var hasNextPageState by mutableStateOf(false)
    private var currentPage = 1

    val results: List<ExploreResult> get() = resultsBacking
    val isLoading: Boolean get() = loadingState
    val isLoadingMore: Boolean get() = loadingMoreState
    val hasNextPage: Boolean get() = hasNextPageState
    val error: NetworkError? get() = errorState

    /** Orden in-app espejo de las listas: solo TITLE/AVERAGE_SCORE/FAVORITES
     *  tienen dato local en ExploreResult; el resto lo ordena el servidor
     *  vía el MediaSort derivado en [updateSort]. */
    val visibleResults: List<ExploreResult>
        get() {
            val ordered = when (currentSortOption) {
                UserListSort.TITLE -> resultsBacking.sortedBy { it.title.lowercase() }
                UserListSort.AVERAGE_SCORE,
                UserListSort.SCORE -> resultsBacking.sortedBy { it.averageScore ?: -1 }
                UserListSort.FAVORITES -> resultsBacking.sortedBy { it.favourites ?: -1 }
                else -> return resultsBacking.toList()
            }
            return if (currentSortOrder == UserListOrder.DESCENDING) ordered.reversed() else ordered
        }

    /** Entrada estilo YT: vacía todo y pide foco al field, preservando filtros. */
    fun enterBlankSearch() {
        searchJob?.cancel()
        loadJob?.cancel()
        query = ""
        resultsBacking.clear()
        errorState = null
        loadingState = false
        hasNextPageState = false
        currentPage = 1
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
        loadingState = true
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
        currentPage = 1
        hasNextPageState = false
        loadingState = true
        errorState = null
        loadJob = scope.launch {
            try {
                val result = fetchPage(1)
                when (result) {
                    is NetworkResult.Success -> {
                        resultsBacking.clear()
                        resultsBacking.addAll(result.value.results)
                        hasNextPageState = result.value.hasNextPage
                        currentPage = 1
                    }
                    is NetworkResult.Failure -> {
                        errorState = result.error
                        resultsBacking.clear()
                    }
                }
            } finally {
                loadingState = false
            }
        }
    }

    fun loadMore() {
        if (loadingMoreState || !hasNextPageState) return
        val nextPage = currentPage + 1
        loadingMoreState = true
        scope.launch {
            val result = fetchPage(nextPage)
            when (result) {
                is NetworkResult.Success -> {
                    resultsBacking.addAll(result.value.results)
                    hasNextPageState = result.value.hasNextPage
                    currentPage = nextPage
                }
                is NetworkResult.Failure -> {
                    errorState = result.error
                }
            }
            loadingMoreState = false
        }
    }

    private suspend fun fetchPage(page: Int) = when (category) {
        ExploreCategory.ANIME -> repository.searchCatalog(
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
        ExploreCategory.MANGA -> repository.searchManga(
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

/** Pure-logic factory (placeholder for UI wiring). */
internal fun createExploreStateHolder(
    repository: ExploreRepository,
    scope: CoroutineScope,
    strings: LanguageStrings,
): ExploreStateHolder {
    return ExploreStateHolder(repository, scope, strings)
}
