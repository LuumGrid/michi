package com.luum.michi.app.root

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.navigation.domain.TabSection
import com.luum.michi.app.core.navigation.domain.label
import com.luum.michi.app.discover.ui.dashboard.DashboardScreen
import com.luum.michi.app.mediaList.ui.anime.AnimeListScreen
import com.luum.michi.app.mediaList.ui.common.MediaListScreenSettings
import com.luum.michi.app.mediaList.ui.manga.MangaListScreen
import com.luum.michi.app.root.state.RootOverlay
import com.luum.michi.app.root.state.State
import com.luum.michi.app.settings.domain.model.ScoreFormat
import com.luum.michi.app.settings.domain.model.formatScoreValue
import com.luum.michi.app.settings.ui.state.SettingsState
import com.luum.michi.app.ui.components.BottomEdgeFade
import com.luum.michi.app.ui.components.MessagePanel
import com.luum.michi.app.ui.components.TabBar
import com.luum.michi.app.ui.components.TabItem
import com.luum.michi.app.ui.components.tabFadeSpec
import com.luum.michi.app.ui.icons.AppIcons

/** The 4 top-level destinations. Root chooses the icons; ui stays dumb. */
@Composable
internal fun tabs(strings: LanguageStrings): List<TabItem> = listOf(
    TabItem(
        section = TabSection.DISCOVER,
        label = TabSection.DISCOVER.label(strings),
        icon = AppIcons.Discover,
        selectedIcon = AppIcons.DiscoverSelected,
    ),
    TabItem(
        section = TabSection.ANIME,
        label = TabSection.ANIME.label(strings),
        icon = AppIcons.Anime,
        selectedIcon = AppIcons.AnimeSelected,
    ),
    TabItem(
        section = TabSection.MANGA,
        label = TabSection.MANGA.label(strings),
        icon = AppIcons.Manga,
        selectedIcon = AppIcons.MangaSelected,
    ),
    TabItem(
        section = TabSection.PROFILE,
        label = TabSection.PROFILE.label(strings),
        icon = AppIcons.Profile,
        selectedIcon = AppIcons.ProfileSelected,
    ),
)

/**
 * Explicit opt-in per surface: the tab bar shows unless a surface asks to
 * hide it (full-screen surfaces like Settings). Default is visible.
 */
private fun showsTabBar(tab: TabSection, state: State): Boolean =
    !state.isOpen(RootOverlay.Settings) && !state.isOpen(RootOverlay.Calendar) && !state.isDetailOpen

/** The bottom tab bar, over the edge fade, hidden by full-screen surfaces. */
@Composable
internal fun RootTabBar(tab: TabSection, state: State, strings: LanguageStrings) {
    AnimatedVisibility(
        visible = showsTabBar(tab, state),
        enter = slideInVertically(animationSpec = tabFadeSpec()) { it } +
            fadeIn(animationSpec = tabFadeSpec()),
        exit = slideOutVertically(animationSpec = tabFadeSpec()) { it } +
            fadeOut(animationSpec = tabFadeSpec()),
        label = "tab-bar-visibility",
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            BottomEdgeFade(modifier = Modifier.matchParentSize())
            TabBar(
                selected = tab,
                tabs = tabs(strings),
                onSelect = { state.selectTab(it) },
            )
        }
    }
}

/**
 * The tab content router: the two lists, the dashboard and the guest/placeholder
 * panels. The query belongs to the tab where search opened, so [tabQuery] only
 * filters the matching list.
 */
@Composable
internal fun RootTabContent(
    activeTab: TabSection,
    state: State,
    holders: RootHolders,
    settingsState: SettingsState,
    viewerId: Int?,
    isSearching: Boolean,
    activeQuery: String,
    tabQuery: String,
    strings: LanguageStrings,
    bottomPadding: Dp,
) {
    // Card smiley pill: translated here so settings types never cross
    // into mediaList (same rule as the editor score lambdas).
    val editorScoreFormat = settingsState.scoreFormat
    val isSmileyFormat: Boolean = editorScoreFormat == ScoreFormat.POINT_3_SMILEYS
    val formatSmiley: (Float) -> String = remember {
        { value: Float -> formatScoreValue(ScoreFormat.POINT_3_SMILEYS, value) }
    }

    // View settings per tab, built once (the lambda above is remembered, so the
    // data-class equals stays stable). The sheet open flags stay out: they flip
    // on every open/close and are not "how the list looks".
    val persistSort = settingsState.persistListSort
    val animeListSettings = remember(
        tabQuery,
        persistSort,
        settingsState.splitCompletedAnime,
        settingsState.displayAdultContent,
        isSmileyFormat,
        formatSmiley,
    ) {
        MediaListScreenSettings(
            query = tabQuery,
            sortPersist = persistSort,
            splitCompleted = settingsState.splitCompletedAnime,
            hideAdult = !settingsState.displayAdultContent,
            isSmileyFormat = isSmileyFormat,
            formatSmiley = formatSmiley,
        )
    }
    val mangaListSettings = remember(
        tabQuery,
        persistSort,
        settingsState.splitCompletedManga,
        settingsState.displayAdultContent,
        isSmileyFormat,
        formatSmiley,
    ) {
        MediaListScreenSettings(
            query = tabQuery,
            sortPersist = persistSort,
            splitCompleted = settingsState.splitCompletedManga,
            hideAdult = !settingsState.displayAdultContent,
            isSmileyFormat = isSmileyFormat,
            formatSmiley = formatSmiley,
        )
    }

    if (activeTab == TabSection.ANIME && holders.anime != null && viewerId != null) {
        // Lists own their query: with or without text the screen
        // renders (full section under the scrim when empty).
        AnimeListScreen(
            holder = holders.anime,
            selected = state.selectedAnimeSection,
            onSelectSection = { state.selectedAnimeSection = it },
            onReload = {
                holders.anime.load(
                    viewerId,
                    forceRefresh = true,
                    splitCompleted = settingsState.splitCompletedAnime,
                )
            },
            bottomPadding = bottomPadding,
            isRefreshing = holders.anime.isRefreshing,
            settings = animeListSettings,
            isFilterOpen = state.isOpen(RootOverlay.SectionFilter),
            onDismissFilter = { state.close(RootOverlay.SectionFilter) },
            isSortOpen = state.isOpen(RootOverlay.ListSort),
            onDismissSort = { state.close(RootOverlay.ListSort) },
            onOpenDetail = { state.openMedia(it) },
            onEditEntry = { state.openEditor(it) },
            onCompleteEntry = { id, progress -> state.openEditorForCompletion(id, progress) },
        )
    } else if (activeTab == TabSection.MANGA && holders.manga != null && viewerId != null) {
        MangaListScreen(
            holder = holders.manga,
            selected = state.selectedMangaSection,
            onSelectSection = { state.selectedMangaSection = it },
            onReload = {
                holders.manga.load(
                    viewerId,
                    forceRefresh = true,
                    splitCompleted = settingsState.splitCompletedManga,
                )
            },
            bottomPadding = bottomPadding,
            isRefreshing = holders.manga.isRefreshing,
            settings = mangaListSettings,
            isFilterOpen = state.isOpen(RootOverlay.SectionFilter),
            onDismissFilter = { state.close(RootOverlay.SectionFilter) },
            isSortOpen = state.isOpen(RootOverlay.ListSort),
            onDismissSort = { state.close(RootOverlay.ListSort) },
            onOpenDetail = { state.openMedia(it) },
            onEditEntry = { state.openEditor(it) },
            onCompleteEntry = { id, progress -> state.openEditorForCompletion(id, progress) },
        )
    } else if (isSearching && activeQuery.isNotEmpty()) {
        // DISCOVER (and guests): server search lands in a later step.
        MessagePanel(
            title = strings.searchNoResultsLabel,
            message = null,
            icon = AppIcons.Search,
        )
    } else if (activeTab == TabSection.DISCOVER) {
        // Discover dashboard: public catalog, guests included.
        // The branch above still owns the active-query state
        // (Explore search lands later).
        DashboardScreen(
            holder = holders.dashboard,
            onOpenMedia = { state.openMedia(it) },
            hideAdult = !settingsState.displayAdultContent,
            bottomPadding = bottomPadding,
        )
    } else if (
        (activeTab == TabSection.ANIME || activeTab == TabSection.MANGA ||
            activeTab == TabSection.PROFILE) && viewerId == null
    ) {
        // Gated tabs without a session: sign-in prompt opening
        // the modal (the only login surface besides first launch).
        val tabItem = tabs(strings).first { it.section == activeTab }
        MessagePanel(
            title = activeTab.label(strings),
            message = strings.authGuestPrompt,
            icon = tabItem.icon,
            actionLabel = strings.settingsSignInTitle,
            onAction = { state.open(RootOverlay.SignIn) },
        )
    } else {
        val tabItem = tabs(strings).first { it.section == activeTab }
        MessagePanel(
            title = activeTab.label(strings),
            message = null,
            icon = tabItem.icon,
        )
    }
}
