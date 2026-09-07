package com.luum.michi.app.discover.presentation.explore

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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luum.michi.app.ui.language.LanguageProvider
import com.luum.michi.app.core.language.networkErrorMessage
import com.luum.michi.app.ui.components.floatingToolbarClearance
import com.luum.michi.app.ui.components.tabBarClearance
import com.luum.michi.app.discover.presentation.common.DiscoverResultCard
import com.luum.michi.app.discover.presentation.explore.state.ExploreStateHolder

@Composable
internal fun ExploreScreen(
    stateHolder: ExploreStateHolder,
    onOpenMedia: (Int) -> Unit,
    onEditMedia: (Int) -> Unit,
) {
    val strings = LanguageProvider.strings

    // Explore siempre muestra algo: si se entra con todo en blanco (sin preset
    // de Dashboard), carga Trending por default en vez de una pantalla vacía.
    LaunchedEffect(Unit) {
        if (stateHolder.results.isEmpty() && !stateHolder.isLoading) {
            stateHolder.load()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = floatingToolbarClearance()),
    ) {
        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            when {
                stateHolder.isLoading && stateHolder.results.isEmpty() ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                stateHolder.error != null && stateHolder.results.isEmpty() ->
                    CenteredMessage(
                        text = stateHolder.error?.let { strings.networkErrorMessage(it) } ?: "",
                        isError = true,
                    )
                stateHolder.visibleResults.isEmpty() ->
                    CenteredMessage(text = strings.searchNoResultsLabel)
                else -> {
                    val gridState = rememberLazyGridState()
                    LaunchedEffect(gridState, stateHolder.hasNextPage) {
                        snapshotFlow {
                            gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        }.collect { last ->
                            if (last >= stateHolder.visibleResults.size - 4 &&
                                stateHolder.hasNextPage &&
                                !stateHolder.isLoadingMore
                            ) {
                                stateHolder.loadMore()
                            }
                        }
                    }
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 12.dp,
                            end = 12.dp,
                            top = 6.dp,
                            bottom = tabBarClearance(),
                        ),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(stateHolder.visibleResults, key = { "${it.id}_${stateHolder.category.name}" }) { result ->
                            DiscoverResultCard(
                                result = result,
                                onClick = { onOpenMedia(result.id) },
                                onLongClick = { onEditMedia(result.id) },
                            )
                        }
                        if (stateHolder.isLoadingMore) {
                            item { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                            item { Box(modifier = Modifier.fillMaxWidth()) {} }
                        }
                    }
                }
            }
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
