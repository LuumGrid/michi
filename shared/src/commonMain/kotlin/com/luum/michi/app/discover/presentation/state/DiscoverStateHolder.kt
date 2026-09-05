package com.luum.michi.app.discover.presentation.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import com.luum.michi.app.core.network.NetworkError
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.core.platform.model.UserListOrder
import com.luum.michi.app.core.platform.model.UserListSort
import com.luum.michi.app.discover.domain.DiscoverRepository
import com.luum.michi.app.search.domain.model.SearchResult

internal enum class DiscoverCategory {
    ANIME,
    MANGA,
    CHARACTERS,
    STAFF,
    STUDIOS
}

internal class DiscoverStateHolder(
    private val repository: DiscoverRepository,
    private val scope: CoroutineScope,
) {
    var query by mutableStateOf("")
    var category by mutableStateOf(DiscoverCategory.ANIME)
    var season by mutableStateOf<String?>(null)
    var genre by mutableStateOf("All")
    var format by mutableStateOf("All")
    var year by mutableStateOf<Int?>(null)
    var sort by mutableStateOf("POPULARITY_DESC")
    var onList by mutableStateOf<Boolean?>(null)
    var currentSortOption by mutableStateOf(UserListSort.POPULARITY)
    var currentSortOrder by mutableStateOf(UserListOrder.DESCENDING)
    var isFilterPersisted by mutableStateOf(false)
    var focusSearchRequested by mutableStateOf(false)

    private val resultsBacking = mutableStateListOf<SearchResult>()
    private var loadingState by mutableStateOf(false)
    private var loadingMoreState by mutableStateOf(false)
    private var errorState by mutableStateOf<NetworkError?>(null)
    private var hasNextPageState by mutableStateOf(false)
    private var currentPage by mutableStateOf(1)

    val results: List<SearchResult> get() = resultsBacking
    val isLoading: Boolean get() = loadingState
    val isLoadingMore: Boolean get() = loadingMoreState
    val hasNextPage: Boolean get() = hasNextPageState
    val error: NetworkError? get() = errorState

    /** Orden in-app espejo de las listas: solo TITLE/AVERAGE_SCORE/FAVORITES
     *  tienen dato local en SearchResult; el resto lo ordena el servidor
     *  vía el MediaSort derivado en [updateSort]. */
    val visibleResults: List<SearchResult>
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
        query = ""
        resultsBacking.clear()
        errorState = null
        hasNextPageState = false
        currentPage = 1
        focusSearchRequested = true
    }

    fun consumeFocusRequest() {
        focusSearchRequested = false
    }

    /** Espejo de las listas: guarda la selección y deriva el MediaSort del API.
     *  La persistencia entre reinicios es solo de sesión en Discover (el slot
     *  de PlatformFilterSettings es único y lo comparten anime/manga). */
    fun updateSort(option: UserListSort, order: UserListOrder, persist: Boolean) {
        currentSortOption = option
        currentSortOrder = order
        isFilterPersisted = persist
        sort = option.toMediaSort(order, category)
        searchJob?.cancel()
        searchJob = scope.launch {
            delay(300.milliseconds)
            load()
        }
    }

    private var searchJob: Job? = null

    fun updateFilters(
        newQuery: String = query,
        newCategory: DiscoverCategory = category,
        newSeason: String? = season,
        newGenre: String = genre,
        newFormat: String = format,
        newYear: Int? = year,
        newOnList: Boolean? = onList,
    ) {
        query = newQuery
        category = newCategory
        season = newSeason
        genre = newGenre
        format = newFormat
        year = newYear
        onList = newOnList

        searchJob?.cancel()
        searchJob = scope.launch {
            delay(300.milliseconds)
            load()
        }
    }

    fun load() {
        currentPage = 1
        hasNextPageState = false
        loadingState = true
        errorState = null
        scope.launch {
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
            loadingState = false
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
        DiscoverCategory.ANIME -> repository.searchCatalog(
            query = query.takeIf { it.isNotBlank() },
            genre = genre.takeIf { it != "All" && it != "Todos" },
            format = format.takeIf { it != "All" && it != "Todos" },
            year = year,
            sort = sort,
            page = page,
            season = season,
            onList = onList,
        )
        DiscoverCategory.MANGA -> repository.searchManga(
            query = query.takeIf { it.isNotBlank() },
            genre = genre.takeIf { it != "All" && it != "Todos" },
            format = format.takeIf { it != "All" && it != "Todos" },
            year = year,
            sort = sort,
            page = page,
            onList = onList,
        )
        DiscoverCategory.CHARACTERS -> repository.searchCharacters(
            query = query.takeIf { it.isNotBlank() },
            page = page,
        )
        DiscoverCategory.STAFF -> repository.searchStaff(
            query = query.takeIf { it.isNotBlank() },
            page = page,
        )
        DiscoverCategory.STUDIOS -> repository.searchStudios(
            query = query.takeIf { it.isNotBlank() },
            page = page,
        )
    }

    fun isEntitySearch(): Boolean =
        category == DiscoverCategory.CHARACTERS ||
        category == DiscoverCategory.STAFF ||
        category == DiscoverCategory.STUDIOS
}

/** Deriva el MediaSort del API desde la selección del sheet compartido
 *  con anime/manga. Las claves sin equivalente usan la más cercana. */
internal fun UserListSort.toMediaSort(order: UserListOrder, category: DiscoverCategory): String {
    val descending = order == UserListOrder.DESCENDING
    fun both(asc: String, desc: String) = if (descending) desc else asc
    return when (this) {
        UserListSort.FOLLOW_LIST -> both("POPULARITY", "POPULARITY_DESC")
        UserListSort.TITLE -> both("TITLE_ROMAJI", "TITLE_ROMAJI_DESC")
        UserListSort.SCORE -> both("SCORE", "SCORE_DESC")
        UserListSort.PROGRESS -> if (category == DiscoverCategory.MANGA) {
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

@Composable
internal fun rememberDiscoverStateHolder(
    repository: DiscoverRepository,
): DiscoverStateHolder {
    val scope = rememberCoroutineScope()
    return remember(repository) {
        DiscoverStateHolder(repository, scope)
    }
}
