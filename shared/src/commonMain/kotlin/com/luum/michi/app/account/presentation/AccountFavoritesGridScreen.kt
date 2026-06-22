package com.luum.michi.app.account.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import com.luum.michi.app.account.presentation.components.AccountFavoriteMediaCard
import com.luum.michi.app.account.presentation.components.AccountFavoritePersonCard
import com.luum.michi.app.account.presentation.components.AccountFavoriteStudioCard
import com.luum.michi.app.account.presentation.model.AccountFavoritesCategory
import com.luum.michi.app.account.presentation.state.AccountFavoritesGridStateHolder
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.language.networkErrorMessage
import com.luum.michi.app.core.platform.components.bottomNavBarClearance

@Composable
internal fun AccountFavoritesGridScreen(
    stateHolder: AccountFavoritesGridStateHolder,
    category: AccountFavoritesCategory,
    onOpenMedia: (Int) -> Unit,
    onEditMedia: (Int) -> Unit,
    onOpenCharacter: (Int) -> Unit,
    onOpenStaff: (Int) -> Unit,
    onOpenStudio: (Int) -> Unit,
) {
    val strings = LanguageProvider.strings

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        when {
            stateHolder.isLoading &&
                stateHolder.mediaItems.isEmpty() &&
                stateHolder.personItems.isEmpty() &&
                stateHolder.studioItems.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            stateHolder.error != null &&
                stateHolder.mediaItems.isEmpty() &&
                stateHolder.personItems.isEmpty() &&
                stateHolder.studioItems.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = stateHolder.error?.let { strings.networkErrorMessage(it) }.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            stateHolder.mediaItems.isEmpty() &&
                stateHolder.personItems.isEmpty() &&
                stateHolder.studioItems.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = strings.accountFavoritesGridEmptyLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            else -> {
                val gridState = rememberLazyGridState()
                val itemCount = stateHolder.mediaItems.size + stateHolder.personItems.size + stateHolder.studioItems.size
                LaunchedEffect(gridState, stateHolder.hasNextPage) {
                    snapshotFlow {
                        gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    }.collect { last ->
                        if (last >= itemCount - 4 && stateHolder.hasNextPage && !stateHolder.isLoadingMore) {
                            stateHolder.loadMore()
                        }
                    }
                }

                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = bottomNavBarClearance(),
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    when (category) {
                        AccountFavoritesCategory.ANIME, AccountFavoritesCategory.MANGA -> {
                            items(stateHolder.mediaItems, key = { it.id }) { media ->
                                AccountFavoriteMediaCard(
                                    media = media,
                                    onClick = { onOpenMedia(media.id) },
                                    onLongClick = { onEditMedia(media.id) },
                                )
                            }
                        }

                        AccountFavoritesCategory.CHARACTERS -> {
                            items(stateHolder.personItems, key = { it.id }) { person ->
                                AccountFavoritePersonCard(
                                    person = person,
                                    onClick = { onOpenCharacter(person.id) },
                                )
                            }
                        }

                        AccountFavoritesCategory.STAFF -> {
                            items(stateHolder.personItems, key = { it.id }) { person ->
                                AccountFavoritePersonCard(
                                    person = person,
                                    onClick = { onOpenStaff(person.id) },
                                )
                            }
                        }

                        AccountFavoritesCategory.STUDIOS -> {
                            items(stateHolder.studioItems, key = { it.id }) { studio ->
                                AccountFavoriteStudioCard(
                                    studio = studio,
                                    onClick = { onOpenStudio(studio.id) },
                                )
                            }
                        }
                    }

                    if (stateHolder.isLoadingMore) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}
