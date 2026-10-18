package com.luum.michi.app.root

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.language.domain.networkErrorMessage
import com.luum.michi.app.core.navigation.domain.DetailDestination
import com.luum.michi.app.core.navigation.domain.DetailListKind
import com.luum.michi.app.core.platform.domain.UrlOpener
import com.luum.michi.app.discover.domain.model.catalogFilter
import com.luum.michi.app.discover.domain.model.catalogKindOf
import com.luum.michi.app.discover.domain.model.genreCatalogFilter
import com.luum.michi.app.discover.ui.catalog.CatalogDetailScreen
import com.luum.michi.app.discover.ui.genre.GenreDetailScreen
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetailType
import com.luum.michi.app.mediaDetail.ui.character.CharacterDetailScreen
import com.luum.michi.app.mediaDetail.ui.components.DetailListLayout
import com.luum.michi.app.mediaDetail.ui.components.DetailListScreen
import com.luum.michi.app.mediaDetail.ui.components.toRailItem
import com.luum.michi.app.mediaDetail.ui.media.MediaDetailScreen
import com.luum.michi.app.mediaDetail.ui.staff.StaffDetailScreen
import com.luum.michi.app.mediaDetail.ui.studio.StudioDetailScreen
import com.luum.michi.app.profile.domain.model.ProfileFavoritesCategory
import com.luum.michi.app.profile.ui.favorites.ProfileFavoritesGridScreen
import com.luum.michi.app.profile.ui.model.favoritesCategoryOfOrNull
import com.luum.michi.app.root.state.State

/**
 * The full-screen detail page rendered over the current tab: one branch per
 * destination, each owning its retry and its cross-feature navigation.
 */
@Composable
internal fun RootDetailHost(
    destination: DetailDestination?,
    holders: RootHolders,
    state: State,
    strings: LanguageStrings,
    bottomPadding: Dp,
    urlOpener: UrlOpener,
) {
    when (destination) {
        is DetailDestination.Media -> MediaDetailScreen(
            holder = holders.mediaDetail,
            strings = strings,
            bottomPadding = bottomPadding,
            onRetry = {
                if (holders.mediaDetail.detail == null) {
                    holders.mediaDetail.load(destination.id)
                } else {
                    holders.mediaDetail.refresh()
                }
            },
            onOpenMedia = { state.openMedia(it) },
            onOpenCharacter = { state.openCharacter(it) },
            onOpenStaff = { state.openStaff(it) },
            onOpenStudio = { state.openStudio(it) },
            onOpenGenre = { name, type -> state.openGenre(name, type) },
            onOpenUrl = { urlOpener.open(it) },
            onViewAll = { state.openDetailList(it, destination.id) },
        )
        is DetailDestination.Character -> CharacterDetailScreen(
            holder = holders.character,
            strings = strings,
            bottomPadding = bottomPadding,
            onRetry = {
                if (holders.character.detail == null) {
                    holders.character.load(destination.id)
                } else {
                    holders.character.refresh()
                }
            },
            onOpenUrl = { urlOpener.open(it) },
            onOpenMedia = { state.openMedia(it) },
            onOpenStaff = { state.openStaff(it) },
            onViewAll = { state.openDetailList(it, destination.id) },
        )
        is DetailDestination.Staff -> StaffDetailScreen(
            holder = holders.staff,
            strings = strings,
            bottomPadding = bottomPadding,
            onRetry = {
                if (holders.staff.detail == null) {
                    holders.staff.load(destination.id)
                } else {
                    holders.staff.refresh()
                }
            },
            onOpenUrl = { urlOpener.open(it) },
            onOpenMedia = { state.openMedia(it) },
            onOpenCharacter = { state.openCharacter(it) },
            onViewAll = { state.openDetailList(it, destination.id) },
        )
        is DetailDestination.Studio -> StudioDetailScreen(
            holder = holders.studio,
            strings = strings,
            bottomPadding = bottomPadding,
            onRetry = {
                if (holders.studio.detail == null) {
                    holders.studio.load(destination.id)
                } else {
                    holders.studio.refresh()
                }
            },
            onOpenMedia = { state.openMedia(it) },
        )
        is DetailDestination.Genre -> {
            val filter = genreCatalogFilter(destination.name, destination.type)
            GenreDetailScreen(
                holder = holders.catalog,
                strings = strings,
                bottomPadding = bottomPadding,
                onRetry = {
                    if (holders.catalog.pageLoaded) {
                        holders.catalog.refresh()
                    } else {
                        holders.catalog.load(filter)
                    }
                },
                onOpenMedia = { state.openMedia(it) },
            )
        }
        is DetailDestination.Catalog -> {
            val kind = catalogKindOf(destination.kindId)
            if (kind != null) {
                val filter = kind.catalogFilter()
                CatalogDetailScreen(
                    holder = holders.catalog,
                    kind = kind,
                    strings = strings,
                    bottomPadding = bottomPadding,
                    onRetry = {
                        if (holders.catalog.pageLoaded) {
                            holders.catalog.refresh()
                        } else {
                            holders.catalog.load(filter)
                        }
                    },
                    onOpenMedia = { state.openMedia(it) },
                )
            }
        }
        is DetailDestination.DetailList -> DetailListHost(
            destination = destination,
            holders = holders,
            state = state,
            strings = strings,
            bottomPadding = bottomPadding,
        )
        is DetailDestination.ProfileFavorites -> {
            val category = favoritesCategoryOfOrNull(destination.categoryId)
            val holder = holders.profileFavorites
            if (category != null && holder != null) {
                ProfileFavoritesGridScreen(
                    holder = holder,
                    category = category,
                    strings = strings,
                    bottomPadding = bottomPadding,
                    onRetry = { holder.refresh() },
                    onOpen = { id -> state.openProfileFavorite(category, id) },
                )
            }
        }
        null -> {}
    }
}

/** A favourites grid item opens the detail of its entity kind. */
private fun State.openProfileFavorite(category: ProfileFavoritesCategory, id: Int) {
    when (category) {
        ProfileFavoritesCategory.ANIME,
        ProfileFavoritesCategory.MANGA -> openMedia(id)
        ProfileFavoritesCategory.CHARACTERS -> openCharacter(id)
        ProfileFavoritesCategory.STAFF -> openStaff(id)
        ProfileFavoritesCategory.STUDIOS -> openStudio(id)
    }
}

/** The "View all" list: the parent's paginated section, one screen per kind. */
@Composable
private fun DetailListHost(
    destination: DetailDestination.DetailList,
    holders: RootHolders,
    state: State,
    strings: LanguageStrings,
    bottomPadding: Dp,
) {
    val listBottom = bottomPadding
    val isManga = holders.mediaDetail.detail?.type == MediaDetailType.MANGA
    val mediaError = holders.mediaDetail.actionError?.let { strings.networkErrorMessage(it) }
    val characterError = holders.character.actionError?.let { strings.networkErrorMessage(it) }
    val staffError = holders.staff.actionError?.let { strings.networkErrorMessage(it) }
    when (destination.kind) {
        DetailListKind.MEDIA_CHARACTERS -> DetailListScreen(
            items = holders.mediaDetail.characters.items.map { it.toRailItem(strings) },
            layout = DetailListLayout.GRID,
            onSelect = { state.openCharacter(it) },
            bottomPadding = listBottom,
            hasMore = holders.mediaDetail.characters.hasNextPage,
            isLoadingMore = holders.mediaDetail.characters.isLoadingMore,
            loadedPage = holders.mediaDetail.characters.currentPage,
            onLoadMore = { holders.mediaDetail.loadMoreCharacters() },
            actionError = mediaError,
            dismissLabel = strings.dismissAction,
            onDismissActionError = { holders.mediaDetail.clearActionError() },
        )
        DetailListKind.MEDIA_STAFF -> DetailListScreen(
            items = holders.mediaDetail.staff.items.map { it.toRailItem() },
            layout = DetailListLayout.GRID,
            onSelect = { state.openStaff(it) },
            bottomPadding = listBottom,
            hasMore = holders.mediaDetail.staff.hasNextPage,
            isLoadingMore = holders.mediaDetail.staff.isLoadingMore,
            loadedPage = holders.mediaDetail.staff.currentPage,
            onLoadMore = { holders.mediaDetail.loadMoreStaff() },
            actionError = mediaError,
            dismissLabel = strings.dismissAction,
            onDismissActionError = { holders.mediaDetail.clearActionError() },
        )
        DetailListKind.MEDIA_RELATED -> DetailListScreen(
            items = holders.mediaDetail.detail?.relations.orEmpty().map { it.toRailItem(strings, isManga) },
            layout = DetailListLayout.ROWS,
            onSelect = { state.openMedia(it) },
            bottomPadding = listBottom,
            // Relations are a single unpaged list.
            loadedPage = 0,
            actionError = mediaError,
            dismissLabel = strings.dismissAction,
            onDismissActionError = { holders.mediaDetail.clearActionError() },
        )
        DetailListKind.MEDIA_RECOMMENDATIONS -> DetailListScreen(
            items = holders.mediaDetail.recommendations.items.map { it.toRailItem(strings, isManga) },
            layout = DetailListLayout.ROWS,
            onSelect = { state.openMedia(it) },
            bottomPadding = listBottom,
            hasMore = holders.mediaDetail.recommendations.hasNextPage,
            isLoadingMore = holders.mediaDetail.recommendations.isLoadingMore,
            loadedPage = holders.mediaDetail.recommendations.currentPage,
            onLoadMore = { holders.mediaDetail.loadMoreRecommendations() },
            actionError = mediaError,
            dismissLabel = strings.dismissAction,
            onDismissActionError = { holders.mediaDetail.clearActionError() },
        )
        DetailListKind.CHARACTER_MEDIA -> DetailListScreen(
            items = holders.character.media.items.map { it.toRailItem(strings) },
            layout = DetailListLayout.ROWS,
            onSelect = { state.openMedia(it) },
            bottomPadding = listBottom,
            hasMore = holders.character.media.hasNextPage,
            isLoadingMore = holders.character.media.isLoadingMore,
            loadedPage = holders.character.media.currentPage,
            onLoadMore = { holders.character.loadMore() },
            actionError = characterError,
            dismissLabel = strings.dismissAction,
            onDismissActionError = { holders.character.clearActionError() },
        )
        DetailListKind.STAFF_MEDIA -> DetailListScreen(
            items = holders.staff.media.items.map { it.toRailItem() },
            layout = DetailListLayout.ROWS,
            onSelect = { state.openMedia(it) },
            bottomPadding = listBottom,
            hasMore = holders.staff.media.hasNextPage,
            isLoadingMore = holders.staff.media.isLoadingMore,
            loadedPage = holders.staff.media.currentPage,
            onLoadMore = { holders.staff.loadMoreMedia() },
            actionError = staffError,
            dismissLabel = strings.dismissAction,
            onDismissActionError = { holders.staff.clearActionError() },
        )
        DetailListKind.STAFF_CHARACTERS -> DetailListScreen(
            items = holders.staff.characters.items.map { it.toRailItem() },
            layout = DetailListLayout.GRID,
            onSelect = { state.openCharacter(it) },
            bottomPadding = listBottom,
            hasMore = holders.staff.characters.hasNextPage,
            isLoadingMore = holders.staff.characters.isLoadingMore,
            loadedPage = holders.staff.characters.currentPage,
            onLoadMore = { holders.staff.loadMoreCharacters() },
            actionError = staffError,
            dismissLabel = strings.dismissAction,
            onDismissActionError = { holders.staff.clearActionError() },
        )
    }
}
