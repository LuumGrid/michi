package com.luum.michi.app.root.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.luum.michi.app.profile.domain.model.ProfileDraft
import com.luum.michi.app.core.navigation.domain.DetailDestination
import com.luum.michi.app.core.navigation.domain.DetailListKind
import com.luum.michi.app.core.navigation.domain.TabSection
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListSection
import com.luum.michi.app.core.medialist.domain.MediaListStatus
import com.luum.michi.app.core.media.domain.MediaType
import com.luum.michi.app.mediaList.domain.manga.model.MangaListSection

/**
 * Progressive back order shared by the toolbar back affordance, the search
 * scrim and (once entries are wired) the system back handler:
 * search -> sheet -> modal/settings -> detail -> overlay -> system.
 * The detail page renders over calendar/explore/notifications, so it closes
 * before them (one back per visible layer).
 */
internal sealed interface BackStep {
    data object CloseSearch : BackStep
    data object CloseSheet : BackStep
    data object CloseOverlay : BackStep
    data object CloseDetail : BackStep
    data object SystemDefault : BackStep
}

internal class State(
    initialProfile: ProfileDraft,
    initialTab: TabSection = TabSection.DISCOVER,
    initialAnimeSection: AnimeListSection = AnimeListSection.ALL,
    initialMangaSection: MangaListSection = MangaListSection.ALL,
) {
    var selectedTab: TabSection by mutableStateOf(initialTab)
    var selectedAnimeSection by mutableStateOf(initialAnimeSection)
    var selectedMangaSection by mutableStateOf(initialMangaSection)
    var currentProfile: ProfileDraft by mutableStateOf(initialProfile)
    private val detailStack = mutableStateListOf<DetailDestination>()
    // Monotonic identity per stack entry: each detail page keeps its own
    // scroll state, and a re-push of the same media is a different entry.
    private val entryIds = mutableStateListOf<Long>()
    private var nextEntryId = 1L
    var editorMediaId: Int? by mutableStateOf(null)
    var editorInitialStatus: MediaListStatus? by mutableStateOf(null)
    var editorInitialProgress: Int? by mutableStateOf(null)
    var isExploreOpen by mutableStateOf(false)
    var isCalendarOpen by mutableStateOf(false)
    var isNotificationsOpen by mutableStateOf(false)
    var isSectionFilterOpen by mutableStateOf(false)
    var isCalendarFilterOpen by mutableStateOf(false)
    var isDetailMenuOpen by mutableStateOf(false)
    /** Sort sheet of the open detail page (studio today). Closes with the page. */
    var isDetailSortOpen by mutableStateOf(false)
    var isListSortOpen by mutableStateOf(false)
    var isExploreFilterOpen by mutableStateOf(false)
    var isShareProfileOpen by mutableStateOf(false)
    var isSettingsOpen by mutableStateOf(false)
    var isSignInOpen by mutableStateOf(false)

    /** In-context search: one active tab at a time, query preserved per tab. */
    var searchActiveTab: TabSection? by mutableStateOf(null)
    private val searchQueries = mutableStateMapOf<TabSection, String>()

    val isSearchActive: Boolean get() = searchActiveTab != null

    val currentDetail: DetailDestination? get() = detailStack.lastOrNull()
    val isDetailOpen: Boolean get() = detailStack.isNotEmpty()

    /** Identity of the top detail entry (0 when the stack is empty). */
    val currentDetailEntryId: Long get() = entryIds.lastOrNull() ?: 0L

    /** Live entry identities, in stack order: Root keys and prunes per-entry state. */
    val detailEntryIds: List<Long> get() = entryIds
    // Compat: several places compare against the open media id
    val selectedMediaId: Int? get() = (currentDetail as? DetailDestination.Media)?.id

    val isEditorOpen: Boolean
        get() = editorMediaId != null

    /** Only PROFILE has no search surface; every other tab searches its own scope. */
    fun supportsSearch(tab: TabSection): Boolean = tab != TabSection.PROFILE

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
        isExploreFilterOpen || isSectionFilterOpen || isListSortOpen || isCalendarFilterOpen ||
            isDetailMenuOpen || isDetailSortOpen -> BackStep.CloseSheet
        // Surfaces drawn ABOVE the detail page (modal sheets + settings).
        isEditorOpen || isShareProfileOpen || isSettingsOpen || isSignInOpen -> BackStep.CloseOverlay
        // The detail page sits over the tab content and over the overlays
        // below, so it closes before them (one back per visible layer).
        isDetailOpen -> BackStep.CloseDetail
        isExploreOpen || isCalendarOpen || isNotificationsOpen -> BackStep.CloseOverlay
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
            isCalendarFilterOpen = false
            isDetailMenuOpen = false
            isDetailSortOpen = false
            true
        }
        BackStep.CloseOverlay -> {
            closeExplore()
            closeCalendar()
            closeNotifications()
            closeEditor()
            closeShareProfile()
            closeSettings()
            closeSignIn()
            true
        }
        BackStep.CloseDetail -> {
            closeDetail()
            true
        }
        BackStep.SystemDefault -> false
    }

    private fun push(dest: DetailDestination) {
        isDetailMenuOpen = false
        isDetailSortOpen = false
        if (detailStack.lastOrNull() != dest) {
            detailStack.add(dest)
            entryIds.add(nextEntryId++)
        }
    }

    fun openMedia(id: Int) = push(DetailDestination.Media(id))
    fun openCharacter(id: Int) = push(DetailDestination.Character(id))
    fun openStaff(id: Int) = push(DetailDestination.Staff(id))
    fun openStudio(id: Int) = push(DetailDestination.Studio(id))
    fun openGenre(name: String, type: MediaType) = push(DetailDestination.Genre(name, type))
    fun openDetailList(kind: DetailListKind, parentId: Int) =
        push(DetailDestination.DetailList(kind, parentId))
    fun closeDetail() {
        isDetailMenuOpen = false
        isDetailSortOpen = false
        if (detailStack.isNotEmpty()) {
            detailStack.removeAt(detailStack.lastIndex)
            entryIds.removeAt(entryIds.lastIndex)
        }
    }

    fun openDetailMenu() {
        isDetailMenuOpen = true
    }

    fun closeDetailMenu() {
        isDetailMenuOpen = false
    }

    fun openDetailSort() {
        isDetailSortOpen = true
    }

    fun closeDetailSort() {
        isDetailSortOpen = false
    }

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

    fun openCalendarFilter() {
        isCalendarFilterOpen = true
    }

    fun closeCalendarFilter() {
        isCalendarFilterOpen = false
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

    fun openSignIn() {
        isSignInOpen = true
    }

    fun closeSignIn() {
        isSignInOpen = false
    }

    /**
     * Drops every transient navigation surface when the session changes user
     * (login, logout, account switch): the detail stack, searches, the editor
     * and the overlays/sheets. Tab and section selection are kept — they are
     * not session data.
     */
    fun reset() {
        detailStack.clear()
        entryIds.clear()
        editorMediaId = null
        editorInitialStatus = null
        editorInitialProgress = null
        searchActiveTab = null
        searchQueries.clear()
        isExploreOpen = false
        isCalendarOpen = false
        isNotificationsOpen = false
        isSectionFilterOpen = false
        isCalendarFilterOpen = false
        isDetailMenuOpen = false
        isDetailSortOpen = false
        isListSortOpen = false
        isExploreFilterOpen = false
        isShareProfileOpen = false
        isSettingsOpen = false
        isSignInOpen = false
    }
}

/** Composition factory: shell preview entry (empty profile until session routing lands). */
@Composable
internal fun rememberState(
    startDestination: StartDestination = StartDestination(),
): State = remember {
    State(
        initialProfile = ProfileDraft(
            username = "",
            displayName = "",
            avatarUrl = null,
            bannerUrl = null,
            bio = "",
        ),
        initialTab = startDestination.tab,
        initialAnimeSection = startDestination.animeSection,
        initialMangaSection = startDestination.mangaSection,
    )
}
