package com.luum.michi.app.discover.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.language.networkErrorMessage
import com.luum.michi.app.core.platform.PlatformIcons
import com.luum.michi.app.core.platform.components.PlatformFloatingSearchContainer
import com.luum.michi.app.core.platform.components.floatingToolbarClearance
import com.luum.michi.app.core.platform.components.tabBarClearance
import com.luum.michi.app.discover.presentation.state.DiscoverStateHolder
import com.luum.michi.app.search.presentation.components.SearchResultCard
import com.luum.michi.app.shell.components.ShellSearchField

@Composable
internal fun DiscoverScreen(
    stateHolder: DiscoverStateHolder,
    onOpenMedia: (Int) -> Unit,
    onEditMedia: (Int) -> Unit,
) {
    val strings = LanguageProvider.strings
    val isEmptySearch = stateHolder.query.isBlank() &&
        stateHolder.results.isEmpty() &&
        !stateHolder.isLoading &&
        stateHolder.error == null

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
                isEmptySearch ->
                    DiscoverEmptySearch(
                        stateHolder = stateHolder,
                        placeholder = strings.discoverSearchPlaceholder,
                        hint = strings.searchEmptyQueryHint,
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
                            SearchResultCard(
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

/** Estado vacío estilo YT: no renderiza nada, solo espera la búsqueda del usuario. */
@Composable
private fun DiscoverEmptySearch(
    stateHolder: DiscoverStateHolder,
    placeholder: String,
    hint: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(72.dp))
        Icon(
            painter = PlatformIcons.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(56.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = hint,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))
        PlatformFloatingSearchContainer(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = PlatformIcons.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(24.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(modifier = Modifier.weight(1f)) {
                    ShellSearchField(
                        query = stateHolder.query,
                        onQueryChange = { stateHolder.updateFilters(newQuery = it) },
                        placeholder = placeholder,
                        autoFocus = false,
                        externalFocusRequest = stateHolder.focusSearchRequested,
                        onFocusConsumed = stateHolder::consumeFocusRequest,
                    )
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
