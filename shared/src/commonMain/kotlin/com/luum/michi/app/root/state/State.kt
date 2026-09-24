package com.luum.michi.app.root.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.luum.michi.app.account.domain.model.AccountProfileDraft
import com.luum.michi.app.core.navigation.domain.DetailDestination
import com.luum.michi.app.core.navigation.domain.TabSection
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListSection
import com.luum.michi.app.core.session.domain.Viewer
import com.luum.michi.app.core.medialist.domain.MediaListStatus
import com.luum.michi.app.mediaList.domain.manga.model.MangaListSection

/**
 * Progressive back order shared by the toolbar back affordance, the search
 * scrim and (once entries are wired) the system back handler:
 * search -> sheet -> overlay -> detail -> system.
 */
internal sealed interface BackStep {
    data object CloseSearch : BackStep
    data object CloseSheet : BackStep
    data object CloseOverlay : BackStep
    data object CloseDetail : BackStep
    data object SystemDefault : BackStep
}

internal class State(
    initialProfile: AccountProfileDraft,
    initialTab: TabSection = TabSection.DISCOVER,
) {
    var selectedTab: TabSection by mutableStateOf(initialTab)
    var selectedAnimeSection by mutableStateOf(AnimeListSection.ALL)
    var selectedMangaSection by mutableStateOf(MangaListSection.ALL)
    var currentProfile: AccountProfileDraft by mutableStateOf(initialProfile)
    private val detailStack = mutableStateListOf<DetailDestination>()
    var editorMediaId: Int? by mutableStateOf(null)
    var editorInitialStatus: MediaListStatus? by mutableStateOf(null)
    var editorInitialProgress: Int? by mutableStateOf(null)
    var isExploreOpen by mutableStateOf(false)
    var isCalendarOpen by mutableStateOf(false)
    var isNotificationsOpen by mutableStateOf(false)
    var isSectionFilterOpen by mutableStateOf(false)
    var isListSortOpen by mutableStateOf(false)
    var isExploreFilterOpen by mutableStateOf(false)
    var isShareProfileOpen by mutableStateOf(false)
    var isSettingsOpen by mutableStateOf(false)

    /** In-context search: one active tab at a time, query preserved per tab. */
    var searchActiveTab: TabSection? by mutableStateOf(null)
    private val searchQueries = mutableStateMapOf<TabSection, String>()

    val isSearchActive: Boolean get() = searchActiveTab != null

    val currentDetail: DetailDestination? get() = detailStack.lastOrNull()
    val isDetailOpen: Boolean get() = detailStack.isNotEmpty()
    // Compat: several places compare against the open media id
    val selectedMediaId: Int? get() = (currentDetail as? DetailDestination.Media)?.id

    val isEditorOpen: Boolean
        get() = editorMediaId != null

    /** Only ACCOUNT has no search surface; every other tab searches its own scope. */
    fun supportsSearch(tab: TabSection): Boolean = tab != TabSection.ACCOUNT

    fun searchQuery(tab: TabSection): String = searchQueries[tab].orEmpty()

    fun openSearch(tab: TabSection) {
        if (supportsSearch(tab)) searchActiveTab = tab
    }

    fun updateSearch(tab: TabSection, query: String) {
        searchQueries[tab] = query
    }

    fun closeSearch() {
        searchActiveTab = null
    }

    fun selectTab(tab: TabSection) {
        selectedTab = tab
        // Queries are preserved per tab in the map, but the search surface
        // always closes: it belongs to the tab where it opened and must
        // never show another tab's query.
        searchActiveTab = null
    }

    fun nextBackStep(): BackStep = when {
        isSearchActive -> BackStep.CloseSearch
        isExploreFilterOpen || isSectionFilterOpen || isListSortOpen -> BackStep.CloseSheet
        isExploreOpen || isCalendarOpen || isNotificationsOpen || isEditorOpen ||
            isShareProfileOpen || isSettingsOpen -> BackStep.CloseOverlay
        isDetailOpen -> BackStep.CloseDetail
        else -> BackStep.SystemDefault
    }

    /**
     * Applies [nextBackStep]. Returns false only for [BackStep.SystemDefault],
     * so platform entries know when to let the system handle back.
     */
    fun applyBackStep(): Boolean = when (nextBackStep()) {
        BackStep.CloseSearch -> {
            closeSearch()
            true
        }
        BackStep.CloseSheet -> {
            isExploreFilterOpen = false
            isSectionFilterOpen = false
            isListSortOpen = false
            true
        }
        BackStep.CloseOverlay -> {
            closeExplore()
            closeCalendar()
            closeNotifications()
            closeEditor()
            closeShareProfile()
            closeSettings()
            true
        }
        BackStep.CloseDetail -> {
            closeDetail()
            true
        }
        BackStep.SystemDefault -> false
    }

    private fun push(dest: DetailDestination) {
        if (detailStack.lastOrNull() != dest) detailStack.add(dest)
    }

    fun openMedia(id: Int) = push(DetailDestination.Media(id))
    fun openCharacter(id: Int) = push(DetailDestination.Character(id))
    fun openStaff(id: Int) = push(DetailDestination.Staff(id))
    fun openStudio(id: Int) = push(DetailDestination.Studio(id))
    fun closeDetail() { if (detailStack.isNotEmpty()) detailStack.removeAt(detailStack.lastIndex) }

    fun openEditor(id: Int) {
        editorMediaId = id
        editorInitialStatus = null
        editorInitialProgress = null
    }

    fun openEditorForCompletion(id: Int, progress: Int) {
        editorMediaId = id
        editorInitialStatus = MediaListStatus.COMPLETED
        editorInitialProgress = progress
    }

    fun closeEditor() {
        editorMediaId = null
        editorInitialStatus = null
        editorInitialProgress = null
    }

    fun openExplore() {
        isExploreOpen = true
    }

    fun closeExplore() {
        isExploreOpen = false
    }

    fun openCalendar() {
        isCalendarOpen = true
    }

    fun closeCalendar() {
        isCalendarOpen = false
    }

    fun openNotifications() {
        isNotificationsOpen = true
    }

    fun closeNotifications() {
        isNotificationsOpen = false
    }

    fun openSectionFilter() {
        isSectionFilterOpen = true
    }

    fun closeSectionFilter() {
        isSectionFilterOpen = false
    }

    fun openListSort() {
        isListSortOpen = true
    }

    fun closeListSort() {
        isListSortOpen = false
    }

    fun openExploreFilter() {
        isExploreFilterOpen = true
    }

    fun closeExploreFilter() {
        isExploreFilterOpen = false
    }

    fun openShareProfile() {
        isShareProfileOpen = true
    }

    fun closeShareProfile() {
        isShareProfileOpen = false
    }

    fun openSettings() {
        isSettingsOpen = true
    }

    fun closeSettings() {
        isSettingsOpen = false
    }
}

/** Pure-logic factory (placeholder for UI wiring). */
internal fun createState(
    viewer: Viewer,
    initialTab: TabSection = TabSection.DISCOVER,
): State {
    return State(
        initialProfile = viewer.toAccountProfileDraft(),
        initialTab = initialTab,
    )
}

/** Composition factory: shell preview entry (empty profile until session routing lands). */
@Composable
internal fun rememberState(
    initialTab: TabSection = TabSection.DISCOVER,
): State = remember {
    State(
        initialProfile = AccountProfileDraft(
            username = "",
            displayName = "",
            avatarUrl = null,
            bannerUrl = null,
            bio = "",
        ),
        initialTab = initialTab,
    )
}

private fun Viewer.toAccountProfileDraft(): AccountProfileDraft = AccountProfileDraft(
    username = name,
    displayName = name,
    avatarUrl = avatarUrl,
    bannerUrl = bannerUrl,
    bio = about.orEmpty(),
)
