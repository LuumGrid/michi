package com.luum.michi.app.explore.presentation.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.explore.data.ExploreRepository
import com.luum.michi.app.search.presentation.model.SearchResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    private var resultsState by mutableStateOf<List<SearchResult>>(emptyList())
    private var loadingState by mutableStateOf(false)
    private var errorState by mutableStateOf<String?>(null)

    val results: List<SearchResult> get() = resultsState
    val isLoading: Boolean get() = loadingState
    val error: String? get() = errorState

    private var searchJob: Job? = null

    fun updateFilters(
        newQuery: String = query,
        newCategory: ExploreCategory = category,
        newGenre: String = genre,
        newFormat: String = format,
        newYear: Int? = year,
        newSort: String = sort,
    ) {
        val queryChanged = newQuery != query
        val categoryChanged = newCategory != category

        query = newQuery
        category = newCategory
        genre = newGenre
        format = newFormat
        year = newYear
        sort = newSort

        searchJob?.cancel()
        searchJob = scope.launch {
            delay(300)
            load()
        }
    }

    fun load() {
        loadingState = true
        errorState = null
        scope.launch {
            val result = when (category) {
                ExploreCategory.ANIMATION -> repository.searchCatalog(
                    query = query.takeIf { it.isNotBlank() },
                    genre = genre.takeIf { it != "All" && it != "Todos" },
                    format = format.takeIf { it != "All" && it != "Todos" },
                    year = year,
                    sort = sort,
                )
                ExploreCategory.READING -> repository.searchManga(
                    query = query.takeIf { it.isNotBlank() },
                    genre = genre.takeIf { it != "All" && it != "Todos" },
                    format = format.takeIf { it != "All" && it != "Todos" },
                    year = year,
                    sort = sort,
                )
                ExploreCategory.CHARACTERS -> repository.searchCharacters(query = query.takeIf { it.isNotBlank() })
                ExploreCategory.STAFF -> repository.searchStaff(query = query.takeIf { it.isNotBlank() })
                ExploreCategory.STUDIOS -> repository.searchStudios(query = query.takeIf { it.isNotBlank() })
            }

            when (result) {
                is NetworkResult.Success -> resultsState = result.value
                is NetworkResult.Failure -> {
                    errorState = result.error.toString()
                    resultsState = emptyList()
                }
            }
            loadingState = false
        }
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
