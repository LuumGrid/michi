package com.luum.michi.app.root

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.media.domain.MediaSeasonYear
import com.luum.michi.app.core.media.domain.currentSeasonAndYear
import com.luum.michi.app.core.media.domain.label
import com.luum.michi.app.core.media.domain.next
import com.luum.michi.app.core.navigation.domain.DetailDestination
import com.luum.michi.app.core.navigation.domain.DetailListKind
import com.luum.michi.app.core.navigation.domain.TabSection
import com.luum.michi.app.core.navigation.domain.label
import com.luum.michi.app.discover.domain.model.DashboardSectionKind
import com.luum.michi.app.discover.domain.model.ExploreCategory
import com.luum.michi.app.discover.domain.model.ExploreFilters
import com.luum.michi.app.discover.domain.model.catalogKindOf
import com.luum.michi.app.discover.domain.model.selectedSeason
import com.luum.michi.app.discover.domain.model.title
import com.luum.michi.app.profile.domain.model.followsKindOfOrNull
import com.luum.michi.app.profile.ui.model.favoritesCategoryOfOrNull
import com.luum.michi.app.profile.ui.model.title
import com.luum.michi.app.root.state.RootOverlay
import com.luum.michi.app.root.state.State
import com.luum.michi.app.ui.components.Toolbar
import com.luum.michi.app.ui.components.ToolbarAction
import com.luum.michi.app.ui.components.ToolbarNavigation
import com.luum.michi.app.ui.components.ToolbarSearch
import com.luum.michi.app.ui.components.TopEdgeFade
import com.luum.michi.app.ui.icons.AppIcons

/**
 * The topbar: title, navigation, trailing actions and the in-context search,
 * over the edge fade. Action ids are mapped here.
 */
@Composable
internal fun RootToolbar(
    state: State,
    tab: TabSection,
    search: ToolbarSearch?,
    searchTab: TabSection?,
    holders: RootHolders,
    isGuest: Boolean,
    strings: LanguageStrings,
) {
    // The heart reads the holder that owns the active destination (media keeps
    // its flag on the detail snapshot; a genre has no favourite).
    val isDetailFavourite = holders.detailHolderFor(state.currentDetail)?.isFavourite == true
    val focusManager = LocalFocusManager.current
    // Explore results mode: the raw flag survives tab changes; only the
    // tab-scoped getter drives the toolbar, so ANIME/MANGA keep their own bar.
    val isExploreResults = state.isExploreResultsVisible
    val exploreMediaCategory = holders.explore.category == ExploreCategory.ANIME ||
        holders.explore.category == ExploreCategory.MANGA
    val exploreFiltersActive = holders.explore.appliedFilters != ExploreFilters.Empty
    // A season catalog's title follows the chip the user selected (the holder
    // owns the bound filter); null for every other destination.
    val catalogSeason = holders.catalog.filter.selectedSeason()
    Box(modifier = Modifier.fillMaxWidth()) {
        TopEdgeFade(modifier = Modifier.matchParentSize())
        Toolbar(
            title = detailToolbarTitle(state.currentDetail, strings, catalogSeason)
                ?: if (state.isDetailOpen) {
                    ""
                } else if (isExploreResults) {
                    // Results mode reads the sent query, not the field text.
                    holders.explore.query
                } else {
                    toolbarTitle(state, tab, strings)
                },
            navigation = if (search != null || state.isDetailOpen || isExploreResults ||
                state.isOpen(RootOverlay.Settings) || state.isOpen(RootOverlay.Calendar) ||
                state.isOpen(RootOverlay.Notifications)
            ) {
                ToolbarNavigation.Back(strings.backButton)
            } else {
                ToolbarNavigation.None
            },
            actions = toolbarActions(
                tab = tab,
                isSearching = search != null,
                isSettingsOpen = state.isOpen(RootOverlay.Settings),
                isCalendarOpen = state.isOpen(RootOverlay.Calendar),
                isDetailOpen = state.isDetailOpen,
                isMediaDetail = state.currentDetail is DetailDestination.Media,
                isStudioDetail = state.currentDetail is DetailDestination.Studio,
                isGenreDetail = state.currentDetail is DetailDestination.Genre,
                isCatalogDetail = state.currentDetail is DetailDestination.Catalog,
                isProfileFavorites = state.currentDetail is DetailDestination.ProfileFavorites,
                isProfileFavoritesEditing = state.favoritesEditing,
                isProfileListPage = state.currentDetail is DetailDestination.ProfileStatistics ||
                    state.currentDetail is DetailDestination.ProfileFollows ||
                    state.currentDetail is DetailDestination.ProfileReviews,
                isDetailList = state.currentDetail is DetailDestination.DetailList,
                isDetailFavourite = isDetailFavourite,
                isGuest = isGuest,
                isNotificationsOpen = state.isOpen(RootOverlay.Notifications),
                unreadNotificationCount = holders.notifications?.unreadCount ?: 0,
                isExploreResults = isExploreResults,
                isExploreMediaCategory = exploreMediaCategory,
                hasExploreFilters = exploreFiltersActive,
                strings = strings,
            ),
            search = search,
            backContentDescription = strings.backButton,
            clearContentDescription = strings.clearSearchAction,
            onNavigation = { state.applyBackStep() },
            onAction = { id ->
                when (id) {
                    ACTION_SEARCH -> state.openSearch(tab)
                    ACTION_SETTINGS -> state.open(RootOverlay.Settings)
                    // Filter targets the list sheet on ANIME/MANGA and the
                    // Explore sheet only while the results toolbar owns DISCOVER.
                    ACTION_FILTER -> if (state.isExploreResultsVisible) {
                        state.open(RootOverlay.ExploreFilter)
                    } else {
                        state.open(RootOverlay.SectionFilter)
                    }
                    ACTION_SORT -> when (state.currentDetail) {
                        // The studio, genre and catalog grids are ordered by the
                        // server, so the sheet targets their holder.
                        is DetailDestination.Studio,
                        is DetailDestination.Genre,
                        is DetailDestination.Catalog -> state.open(RootOverlay.DetailSort)
                        else -> state.open(RootOverlay.ListSort)
                    }
                    ACTION_CALENDAR -> state.open(RootOverlay.Calendar)
                    ACTION_CALENDAR_FILTER -> state.open(RootOverlay.CalendarFilter)
                    ACTION_FAVORITES_EDIT -> state.toggleFavoritesEditing()
                    ACTION_NOTIFICATIONS -> state.open(RootOverlay.Notifications)
                    ACTION_EDIT -> if (!isGuest) {
                        holders.mediaDetail.detail?.id?.let { state.openEditor(it) }
                    }
                    ACTION_FAVOURITE -> if (!isGuest) {
                        holders.detailHolderFor(state.currentDetail)?.toggleFavourite()
                    }
                    ACTION_MORE -> state.open(RootOverlay.DetailMenu)
                }
            },
            onSearchChange = { query ->
                searchTab?.let { state.updateSearch(it, query) }
            },
            onSearchSubmit = {
                // Only DISCOVER searches the server (Explore). The lists filter
                // locally as you type, so their submit stays a no-op. Submitting
                // collapses the field into results mode.
                if (searchTab == TabSection.DISCOVER) {
                    val submitted = state.searchQuery(TabSection.DISCOVER)
                    if (submitted.isNotBlank()) {
                        holders.explore.submitSearch(submitted)
                        state.enterExploreResults(holders.explore.query)
                        state.closeSearch()
                        focusManager.clearFocus()
                    }
                }
            },
            onSearchClose = { state.applyBackStep() },
        )
    }
}

private const val ACTION_SEARCH = "search"
private const val ACTION_SETTINGS = "settings"
private const val ACTION_FILTER = "filter"
private const val ACTION_SORT = "sort"
private const val ACTION_CALENDAR = "calendar"
private const val ACTION_CALENDAR_FILTER = "calendar-filter"
private const val ACTION_FAVORITES_EDIT = "favorites-edit"
private const val ACTION_NOTIFICATIONS = "notifications"
private const val ACTION_EDIT = "edit"
private const val ACTION_FAVOURITE = "favourite"
private const val ACTION_MORE = "more"

/** Shared capsule group for the list tools (reference: one capsule, lone search). */
private const val GROUP_LIST_TOOLS = "list-tools"

internal fun toolbarActions(
    tab: TabSection,
    isSearching: Boolean,
    isSettingsOpen: Boolean,
    isCalendarOpen: Boolean,
    isDetailOpen: Boolean,
    isMediaDetail: Boolean,
    isStudioDetail: Boolean,
    isGenreDetail: Boolean,
    isCatalogDetail: Boolean = false,
    isProfileFavorites: Boolean = false,
    // The statistics/follows/reviews pages carry only their back chevron.
    isProfileListPage: Boolean = false,
    isDetailList: Boolean,
    isDetailFavourite: Boolean,
    isGuest: Boolean,
    // The favourites "View all" page's edit toggle (pencil / check).
    isProfileFavoritesEditing: Boolean = false,
    // PROFILE's notifications bell (session only): badge = unread > 0.
    isNotificationsOpen: Boolean = false,
    unreadNotificationCount: Int = 0,
    // Explore results mode owns DISCOVER: filter+sort (media only) + magnifier.
    isExploreResults: Boolean = false,
    isExploreMediaCategory: Boolean = false,
    hasExploreFilters: Boolean = false,
    strings: LanguageStrings,
): List<ToolbarAction> {
    if (isSearching || isSettingsOpen || isNotificationsOpen) return emptyList()
    // Detail overlay: edit / favourite / overflow trailing the back chevron.
    // The heart swaps to its filled twin when the entry is favourited.
    // Character/staff/studio details have no list entry, so only the heart.
    if (isDetailOpen) {
        val sort = ToolbarAction(
            id = ACTION_SORT,
            icon = AppIcons.Sort,
            contentDescription = strings.sortAction,
        )
        // A "View all" list has nothing but its back chevron.
        if (isDetailList || isProfileListPage) return emptyList()
        // The favourites grid carries only its edit toggle (pencil / check).
        if (isProfileFavorites) {
            return listOf(
                ToolbarAction(
                    id = ACTION_FAVORITES_EDIT,
                    icon = if (isProfileFavoritesEditing) AppIcons.Check else AppIcons.Edit,
                    contentDescription = if (isProfileFavoritesEditing) {
                        strings.profileFavoritesDoneAction
                    } else {
                        strings.profileEditFavoritesAction
                    },
                ),
            )
        }
        // A catalog page is not an entity: it only carries the sort.
        if (isCatalogDetail) return listOf(sort)
        val favourite = ToolbarAction(
            id = ACTION_FAVOURITE,
            icon = if (isDetailFavourite) AppIcons.FavoriteFilled else AppIcons.Favorite,
            contentDescription = strings.favouriteLabel,
        )
        val more = ToolbarAction(
            id = ACTION_MORE,
            icon = AppIcons.More,
            contentDescription = strings.moreAction,
        )
        // A guest has no list entry to edit and no session to favourite with:
        // only the overflow, plus the sort a studio/genre grid owns.
        if (isGuest) {
            return if (isGenreDetail || isStudioDetail) listOf(sort, more) else listOf(more)
        }
        // Media adds the editor pencil, studio and genre their media sort; the
        // other scopes have no list entry. All open the overflow (AniList link +
        // share). A genre has no entity to favourite, so it skips the heart.
        if (isGenreDetail) {
            return listOf(
                sort,
                more,
            )
        }
        if (isStudioDetail) {
            return listOf(
                sort,
                favourite,
                more,
            )
        }
        if (!isMediaDetail) return listOf(favourite, more)
        return listOf(
            ToolbarAction(
                id = ACTION_EDIT,
                icon = AppIcons.Edit,
                contentDescription = strings.editEntryAction,
            ),
            favourite,
            more,
        )
    }
    // Calendar overlay: lone filter next to the back chevron. Settings
    // hides everything, but the day list needs its filters in reach.
    if (isCalendarOpen) {
        return listOf(
            ToolbarAction(
                id = ACTION_CALENDAR_FILTER,
                icon = AppIcons.Filter,
                contentDescription = strings.filterAction,
            ),
        )
    }
    // Explore results mode: the toolbar owns DISCOVER. Media categories get the
    // filter+sort capsule (filter badged while filters are active) and the lone
    // search magnifier; people/studios only the magnifier. No calendar here.
    if (isExploreResults) {
        return buildList {
            if (isExploreMediaCategory) {
                add(
                    ToolbarAction(
                        id = ACTION_FILTER,
                        icon = AppIcons.Filter,
                        contentDescription = strings.filterAction,
                        group = GROUP_LIST_TOOLS,
                        badge = hasExploreFilters,
                    ),
                )
                add(
                    ToolbarAction(
                        id = ACTION_SORT,
                        icon = AppIcons.Sort,
                        contentDescription = strings.sortAction,
                        group = GROUP_LIST_TOOLS,
                    ),
                )
            }
            add(
                ToolbarAction(
                    id = ACTION_SEARCH,
                    icon = AppIcons.Search,
                    contentDescription = strings.searchTitle,
                ),
            )
        }
    }
    // Gated tabs without a session keep the calendar only (public airing
    // data, no login needed): filter/sort/search operate on lists that
    // don't exist. The calendar button opens the schedule overlay, which
    // guests render with null user fields.
    val isGatedGuest = isGuest && (tab == TabSection.ANIME || tab == TabSection.MANGA)
    return buildList {
        if (tab != TabSection.PROFILE) {
            // Ungrouped on purpose: lone circle left of the tools capsule.
            add(
                ToolbarAction(
                    id = ACTION_CALENDAR,
                    icon = AppIcons.Calendar,
                    contentDescription = strings.calendarAction,
                ),
            )
        }
        if (!isGatedGuest && (tab == TabSection.ANIME || tab == TabSection.MANGA)) {
            add(
                ToolbarAction(
                    id = ACTION_FILTER,
                    icon = AppIcons.Filter,
                    contentDescription = strings.filterAction,
                    group = GROUP_LIST_TOOLS,
                ),
            )
            add(
                ToolbarAction(
                    id = ACTION_SORT,
                    icon = AppIcons.Sort,
                    contentDescription = strings.sortAction,
                    group = GROUP_LIST_TOOLS,
                ),
            )
        }
        if (tab != TabSection.PROFILE) {
            if (!isGatedGuest) {
                add(
                    ToolbarAction(
                        id = ACTION_SEARCH,
                        icon = AppIcons.Search,
                        contentDescription = strings.searchTitle,
                    ),
                )
            }
        } else {
            // PROFILE: the notifications bell and the gear, each its own circle.
            if (!isGuest) {
                add(
                    ToolbarAction(
                        id = ACTION_NOTIFICATIONS,
                        icon = AppIcons.Notifications,
                        contentDescription = strings.notificationsLabel,
                        badge = unreadNotificationCount > 0,
                    ),
                )
            }
            add(
                ToolbarAction(
                    id = ACTION_SETTINGS,
                    icon = AppIcons.Settings,
                    contentDescription = strings.settingsAction,
                ),
            )
        }
    }
}

private fun toolbarTitle(
    state: State,
    tab: TabSection,
    strings: LanguageStrings,
): String = if (state.isOpen(RootOverlay.Settings)) {
    strings.settingsTitle
} else if (state.isOpen(RootOverlay.Calendar)) {
    strings.calendarTitle
} else if (state.isOpen(RootOverlay.Notifications)) {
    strings.notificationsLabel
} else {
    tab.label(strings)
}

/** Toolbar title for a destination that carries its name in the bar instead of
 *  the content: a "View all" list and a genre page (both are lists). Null for
 *  every other destination — media/character/staff/studio keep their title in
 *  the hero, and tabs use [toolbarTitle]. */
private fun detailToolbarTitle(
    destination: DetailDestination?,
    strings: LanguageStrings,
    catalogSeason: MediaSeasonYear?,
): String? =
    when (destination) {
        is DetailDestination.DetailList -> when (destination.kind) {
            DetailListKind.MEDIA_CHARACTERS -> strings.mediaDetailCharactersTitle
            DetailListKind.MEDIA_STAFF -> strings.mediaDetailTabStaff
            DetailListKind.MEDIA_RELATED -> strings.mediaDetailRelationsTitle
            DetailListKind.MEDIA_RECOMMENDATIONS -> strings.mediaDetailTabRecommendations
            DetailListKind.CHARACTER_MEDIA -> strings.mediaDetailAppearancesTitle
            DetailListKind.STAFF_MEDIA -> strings.mediaDetailRolesTitle
            DetailListKind.STAFF_CHARACTERS -> strings.mediaDetailCharactersTitle
        }
        is DetailDestination.Genre -> destination.name
        is DetailDestination.Catalog -> catalogKindOf(destination.kindId)?.let { kind ->
            val current = currentSeasonAndYear()
            if (kind == DashboardSectionKind.SEASONAL ||
                kind == DashboardSectionKind.UPCOMING_SEASON
            ) {
                catalogSeason?.label(strings) ?: kind.title(strings, current, current.next())
            } else {
                kind.title(strings, current, current.next())
            }
        }
        is DetailDestination.ProfileFavorites ->
            favoritesCategoryOfOrNull(destination.categoryId)?.title(strings)
        is DetailDestination.ProfileStatistics -> strings.profileStatisticsTitle
        is DetailDestination.ProfileFollows ->
            followsKindOfOrNull(destination.kindId)?.title(strings)
        is DetailDestination.ProfileReviews -> strings.profileReviewsTitle
        else -> null
    }
