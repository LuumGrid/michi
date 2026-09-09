package com.luum.michi.app.discover.ui.explore.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.model.UserListOrder
import com.luum.michi.app.core.model.UserListSort
import com.luum.michi.app.discover.domain.ExploreRepository
import com.luum.michi.app.discover.domain.model.ExploreCategory
import com.luum.michi.app.discover.domain.model.ExploreResult

internal class ExploreStateHolder(
    private val repository: ExploreRepository,
    private val scope: CoroutineScope,
    private val strings: LanguageStrings,
) {
    var query = ""
    var category = ExploreCategory.ANIME
    var season: String? = null
    var genres = emptyList<String>()
    var formats = emptyList<String>()
    var year: Int? = null
    var sort = "TRENDING_DESC"
    var onList: Boolean? = null
    var currentSortOption = UserListSort.TRENDING
    var currentSortOrder = UserListOrder.DESCENDING
    var isFilterPersisted = false
    var focusSearchRequested = false

    private val resultsBacking = mutableListOf<ExploreResult>()
    private var loadingState = false
    private var loadingMoreState = false
    private var errorState: NetworkError? = null
    private var hasNextPageState = false
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
     *  La persistencia entre reinicios es solo de sesión en Discover (el slot
     *  de FilterSettings es único y lo comparten anime/manga). */
    fun updateSort(option: UserListSort, order: UserListOrder, persist: Boolean) {
        currentSortOption = option
        currentSortOrder = order
        isFilterPersisted = persist
        sort = option.toMediaSort(order, category)
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
        isFilterPersisted = false
        sort = sortOption.toMediaSort(sortOrder, category)
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
            strings = strings,
        )
        ExploreCategory.CHARACTERS -> repository.searchCharacters(
            query = query.takeIf { it.isNotBlank() },
            page = page,
        )
        ExploreCategory.STAFF -> repository.searchStaff(
            query = query.takeIf { it.isNotBlank() },
            page = page,
        )
        ExploreCategory.STUDIOS -> repository.searchStudios(
            query = query.takeIf { it.isNotBlank() },
            page = page,
        )
    }
}

/** Deriva el MediaSort del API desde la selección del sheet compartido
 *  con anime/manga. Las claves sin equivalente usan la más cercana. */
internal fun UserListSort.toMediaSort(order: UserListOrder, category: ExploreCategory): String {
    val descending = order == UserListOrder.DESCENDING
    fun both(asc: String, desc: String) = if (descending) desc else asc
    return when (this) {
        UserListSort.FOLLOW_LIST -> both("POPULARITY", "POPULARITY_DESC")
        UserListSort.TITLE -> both("TITLE_ROMAJI", "TITLE_ROMAJI_DESC")
        UserListSort.SCORE -> both("SCORE", "SCORE_DESC")
        UserListSort.PROGRESS -> if (category == ExploreCategory.MANGA) {
            both("CHAPTERS", "CHAPTERS_DESC")
        } else {
            both("EPISODES", "EPISODES_DESC")
        }
        UserListSort.LAST_UPDATED -> both("UPDATED_AT", "UPDATED_AT_DESC")
        UserListSort.LAST_ADDED -> both("ID", "ID_DESC")
        UserListSort.START_DATE -> both("START_DATE", "START_DATE_DESC")
        UserListSort.COMPLETED_DATE -> both("END_DATE", "END_DATE_DESC")
        UserListSort.RELEASE_DATE -> both("START_DATE", "START_DATE_DESC")
        UserListSort.AVERAGE_SCORE -> both("SCORE", "SCORE_DESC")
        UserListSort.POPULARITY -> both("POPULARITY", "POPULARITY_DESC")
        UserListSort.FAVORITES -> both("FAVOURITES", "FAVOURITES_DESC")
        UserListSort.TRENDING -> both("TRENDING", "TRENDING_DESC")
        UserListSort.PRIORITY -> both("POPULARITY", "POPULARITY_DESC")
        UserListSort.NEXT_AIRING -> both("START_DATE", "START_DATE_DESC")
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
