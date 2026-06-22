package com.luum.michi.app.explore.presentation.state

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
import com.luum.michi.app.explore.data.ExploreRepository
import com.luum.michi.app.search.presentation.model.SearchResult

internal enum class ExploreCategory {
    ANIMATION,
    READING,
    CHARACTERS,
    STAFF,
    STUDIOS
}

internal class ExploreStateHolder(
    private val repository: ExploreRepository,
    private val scope: CoroutineScope,
) {
    var query by mutableStateOf("")
    var category by mutableStateOf(ExploreCategory.ANIMATION)
    var genre by mutableStateOf("All")
    var format by mutableStateOf("All")
    var year by mutableStateOf<Int?>(null)
    var sort by mutableStateOf("POPULARITY_DESC")
    var onList by mutableStateOf<Boolean?>(null)

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

    private var searchJob: Job? = null

    fun updateFilters(
        newQuery: String = query,
        newCategory: ExploreCategory = category,
        newGenre: String = genre,
        newFormat: String = format,
        newYear: Int? = year,
        newSort: String = sort,
        newOnList: Boolean? = onList,
    ) {
        query = newQuery
        category = newCategory
        genre = newGenre
        format = newFormat
        year = newYear
        sort = newSort
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
        ExploreCategory.ANIMATION -> repository.searchCatalog(
            query = query.takeIf { it.isNotBlank() },
            genre = genre.takeIf { it != "All" && it != "Todos" },
            format = format.takeIf { it != "All" && it != "Todos" },
            year = year,
            sort = sort,
            page = page,
            onList = onList,
        )
        ExploreCategory.READING -> repository.searchManga(
            query = query.takeIf { it.isNotBlank() },
            genre = genre.takeIf { it != "All" && it != "Todos" },
            format = format.takeIf { it != "All" && it != "Todos" },
            year = year,
            sort = sort,
            page = page,
            onList = onList,
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

    fun isEntitySearch(): Boolean =
        category == ExploreCategory.CHARACTERS ||
        category == ExploreCategory.STAFF ||
        category == ExploreCategory.STUDIOS
}

@Composable
internal fun rememberExploreStateHolder(
    repository: ExploreRepository,
): ExploreStateHolder {
    val scope = rememberCoroutineScope()
    return remember(repository) {
        ExploreStateHolder(repository, scope)
    }
}
