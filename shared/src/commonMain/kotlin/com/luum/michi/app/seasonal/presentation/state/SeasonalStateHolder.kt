package com.luum.michi.app.seasonal.presentation.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.luum.michi.app.core.media.MediaSeason
import com.luum.michi.app.core.media.MediaSeasonYear
import com.luum.michi.app.core.media.currentSeasonAndYear
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.explore.data.ExploreRepository
import com.luum.michi.app.search.presentation.model.SearchResult
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class SeasonalStateHolder(
    private val repository: ExploreRepository,
    private val scope: CoroutineScope,
    initial: MediaSeasonYear = currentSeasonAndYear(),
) {
    var season by mutableStateOf(initial.season)
        private set
    var year by mutableStateOf(initial.year)
        private set
    var sort by mutableStateOf("POPULARITY_DESC")
        private set
    var onList by mutableStateOf<Boolean?>(null)
        private set

    private var loadJob: Job? = null

    private val resultsBacking = mutableStateListOf<SearchResult>()
    private var loadingState by mutableStateOf(false)
    private var loadingMoreState by mutableStateOf(false)
    private var errorState by mutableStateOf<String?>(null)
    private var hasNextPageState by mutableStateOf(false)
    private var currentPage by mutableStateOf(1)

    val results: List<SearchResult> get() = resultsBacking
    val isLoading: Boolean get() = loadingState
    val isLoadingMore: Boolean get() = loadingMoreState
    val hasNextPage: Boolean get() = hasNextPageState
    val error: String? get() = errorState

    fun setSeasonYear(value: MediaSeasonYear) {
        season = value.season
        year = value.year
        load()
    }

    fun selectSeason(value: MediaSeason) {
        season = value
        load()
    }

    fun selectYear(value: Int) {
        year = value
        load()
    }

    fun selectSort(value: String) {
        sort = value
        load()
    }

    fun selectOnList(value: Boolean?) {
        onList = value
        load()
    }

    fun applySortAndOnList(newSort: String, newOnList: Boolean?) {
        sort = newSort
        onList = newOnList
        load()
    }

    fun load() {
        loadJob?.cancel()
        currentPage = 1
        hasNextPageState = false
        loadingState = true
        errorState = null
        loadJob = scope.launch {
            val result = fetchPage(1)
            when (result) {
                is NetworkResult.Success -> {
                    resultsBacking.clear()
                    resultsBacking.addAll(result.value.results)
                    hasNextPageState = result.value.hasNextPage
                    currentPage = 1
                }
                is NetworkResult.Failure -> {
                    errorState = result.error.toString()
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
                    errorState = result.error.toString()
                }
            }
            loadingMoreState = false
        }
    }

    private suspend fun fetchPage(page: Int) = repository.searchCatalog(
        query = null,
        genre = null,
        format = null,
        year = year,
        sort = sort,
        page = page,
        season = season.name,
        onList = onList,
    )
}

@Composable
internal fun rememberSeasonalStateHolder(
    repository: ExploreRepository,
): SeasonalStateHolder {
    val scope = rememberCoroutineScope()
    return remember(repository) {
        SeasonalStateHolder(repository, scope)
    }
}
