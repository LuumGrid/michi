package com.luum.michi.app.search.presentation.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    var results by mutableStateOf<List<SearchResult>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private var currentJob: Job? = null

    fun selectType(value: SearchType) {
        if (type == value) return
        type = value
    }

    fun submit(query: String) {
        currentJob?.cancel()
        if (query.isBlank()) {
            results = emptyList()
            isLoading = false
            error = null
            return
        }
        isLoading = true
        error = null
        currentJob = scope.launch {
            when (val result = repository.search(query, type)) {
                is NetworkResult.Success -> {
                    results = result.value
                    error = null
                }
                is NetworkResult.Failure -> {
                    results = emptyList()
                    error = result.error.toString()
                }
            }
            isLoading = false
        }
    }

    fun reset() {
        currentJob?.cancel()
        currentJob = null
        results = emptyList()
        isLoading = false
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
