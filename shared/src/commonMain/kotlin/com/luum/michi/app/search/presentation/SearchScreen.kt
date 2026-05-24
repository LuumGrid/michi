package com.luum.michi.app.search.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.components.PlatformChips
import com.luum.michi.app.search.presentation.components.SearchResultCard
import com.luum.michi.app.search.presentation.model.SearchResult
import com.luum.michi.app.search.presentation.model.SearchType
import com.luum.michi.app.search.presentation.model.label
import com.luum.michi.app.search.presentation.state.SearchStateHolder
import kotlinx.coroutines.delay

private val SearchTypeOptions: List<SearchType> = listOf(
    SearchType.ALL,
    SearchType.ANIME,
    SearchType.MANGA,
)

private const val SearchDebounceMillis: Long = 300

@Composable
internal fun SearchScreen(
    query: String,
    stateHolder: SearchStateHolder,
    onOpenMedia: (Int) -> Unit,
    onEditMedia: (Int) -> Unit,
) {
    val strings = LanguageProvider.strings

    LaunchedEffect(query, stateHolder.type) {
        if (query.isBlank()) {
            stateHolder.reset()
            return@LaunchedEffect
        }
        delay(SearchDebounceMillis)
        stateHolder.submit(query)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        PlatformChips(
            items = SearchTypeOptions,
            selectedItem = stateHolder.type,
            onSelect = stateHolder::selectType,
            label = { it.label(strings) },
            modifier = Modifier.fillMaxWidth(),
        )

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                query.isBlank() -> CenteredMessage(text = strings.searchEmptyQueryHint)
                stateHolder.isLoading && stateHolder.results.isEmpty() ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                stateHolder.error != null && stateHolder.results.isEmpty() ->
                    CenteredMessage(text = stateHolder.error ?: "", isError = true)
                stateHolder.results.isEmpty() -> CenteredMessage(text = strings.searchNoResultsLabel)
                else -> SearchResultsGrid(
                    results = stateHolder.results,
                    onOpenMedia = onOpenMedia,
                    onEditMedia = onEditMedia,
                )
            }
        }
    }
}

@Composable
private fun SearchResultsGrid(
    results: List<SearchResult>,
    onOpenMedia: (Int) -> Unit,
    onEditMedia: (Int) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(results, key = { it.id }) { result ->
            SearchResultCard(
                result = result,
                onClick = { onOpenMedia(result.id) },
                onLongClick = { onEditMedia(result.id) },
            )
        }
    }
}

@Composable
private fun CenteredMessage(text: String, isError: Boolean = false) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
