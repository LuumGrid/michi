package com.luum.michi.app.search.presentation.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.search.data.SearchRepository
import com.luum.michi.app.search.presentation.model.SearchResult
import com.luum.michi.app.search.presentation.model.SearchType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class SearchStateHolder(
    private val repository: SearchRepository,
    private val scope: CoroutineScope,
) {
    var type by mutableStateOf(SearchType.ALL)
        private set

    private val resultsBacking = mutableStateListOf<SearchResult>()
    val results: List<SearchResult> get() = resultsBacking

    var isLoading by mutableStateOf(false)
        private set
    var isLoadingMore by mutableStateOf(false)
        private set
    var hasNextPage by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private var currentQuery: String = ""
    private var currentPage: Int = 1
    private var currentJob: Job? = null
    private var loadMoreJob: Job? = null

    fun selectType(value: SearchType) {
        if (type == value) return
        type = value
    }

    fun submit(query: String) {
        currentJob?.cancel()
        loadMoreJob?.cancel()
        if (query.isBlank()) {
            resultsBacking.clear()
            isLoading = false
            isLoadingMore = false
            hasNextPage = false
            error = null
            return
        }
        currentQuery = query
        currentPage = 1
        hasNextPage = false
        isLoading = true
        error = null
        currentJob = scope.launch {
            when (val result = repository.search(query, type, page = 1)) {
                is NetworkResult.Success -> {
                    resultsBacking.clear()
                    resultsBacking.addAll(result.value.results)
                    hasNextPage = result.value.hasNextPage
                    error = null
                }
                is NetworkResult.Failure -> {
                    resultsBacking.clear()
                    error = result.error.toString()
                }
            }
            isLoading = false
        }
    }

    fun loadMore() {
        if (isLoadingMore || !hasNextPage) return
        val nextPage = currentPage + 1
        isLoadingMore = true
        loadMoreJob = scope.launch {
            when (val result = repository.search(currentQuery, type, page = nextPage)) {
                is NetworkResult.Success -> {
                    resultsBacking.addAll(result.value.results)
                    hasNextPage = result.value.hasNextPage
                    currentPage = nextPage
                }
                is NetworkResult.Failure -> {
                    error = result.error.toString()
                }
            }
            isLoadingMore = false
        }
    }

    fun reset() {
        currentJob?.cancel()
        loadMoreJob?.cancel()
        currentJob = null
        loadMoreJob = null
        resultsBacking.clear()
        isLoading = false
        isLoadingMore = false
        hasNextPage = false
        error = null
    }
}

@Composable
internal fun rememberSearchStateHolder(
    repository: SearchRepository,
): SearchStateHolder {
    val scope = rememberCoroutineScope()
    return remember(repository) { SearchStateHolder(repository, scope) }
}
