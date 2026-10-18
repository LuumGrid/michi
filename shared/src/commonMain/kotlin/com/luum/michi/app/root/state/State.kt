package com.luum.michi.app.root.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.luum.michi.app.core.navigation.domain.DetailDestination
import com.luum.michi.app.core.navigation.domain.DetailListKind
import com.luum.michi.app.core.navigation.domain.TabSection
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListSection
import com.luum.michi.app.core.medialist.domain.MediaListStatus
import com.luum.michi.app.core.media.domain.MediaType
import com.luum.michi.app.mediaList.domain.manga.model.MangaListSection

/**
 * Progressive back order shared by the toolbar back affordance, the search
 * scrim and the system back handler:
 * sheet -> above-detail (modal/settings/editor) -> detail -> search ->
 * Explore results -> below-detail overlay -> system.
 * The detail page renders over calendar/explore/notifications, so it closes
 * before them; the search and its results mode are tab-surface states, so they
 * close after the detail but before those below-detail overlays (one back per
 * visible layer).
 */
internal sealed interface BackStep {
    data object CloseSearch : BackStep
    data object CloseExploreResults : BackStep
    data object CloseFavoritesEdit : BackStep
    data object CloseSheet : BackStep
    data object CloseOverlay : BackStep
    data object CloseDetail : BackStep
    data object SystemDefault : BackStep
}

internal class State(
    initialTab: TabSection = TabSection.DISCOVER,
    initialAnimeSection: AnimeListSection = AnimeListSection.ALL,
    initialMangaSection: MangaListSection = MangaListSection.ALL,
) {
    var selectedTab: TabSection by mutableStateOf(initialTab)
    var selectedAnimeSection by mutableStateOf(initialAnimeSection)
    var selectedMangaSection by mutableStateOf(initialMangaSection)
    private val detailStack = mutableStateListOf<DetailDestination>()
    // Monotonic identity per stack entry: each detail page keeps its own
    // scroll state, and a re-push of the same media is a different entry.
    private val entryIds = mutableStateListOf<Long>()
    private var nextEntryId = 1L
    var editorMediaId: Int? by mutableStateOf(null)
    var editorInitialStatus: MediaListStatus? by mutableStateOf(null)
    var editorInitialProgress: Int? by mutableStateOf(null)

    /**
     * Overlays and sheets open right now. Not exclusive within a layer: SignIn
     * opens from Settings, and one back closes a whole layer at once (see
     * [nextBackStep]). The editor is not part of the set: it carries data and
     * keeps its own fields.
     */
    var openOverlays: Set<RootOverlay> by mutableStateOf(emptySet())
        private set

    fun isOpen(overlay: RootOverlay): Boolean = overlay in openOverlays

    fun open(overlay: RootOverlay) {
        openOverlays = openOverlays + overlay
    }

    fun close(overlay: RootOverlay) {
        openOverlays = openOverlays - overlay
    }

    /** In-context search: one active tab at a time, query preserved per tab. */
    var searchActiveTab: TabSection? by mutableStateOf(null)
    private val searchQueries = mutableStateMapOf<TabSection, String>()

    /**
     * Explore results mode: the user submitted a DISCOVER query, so the field
     * collapsed and the toolbar shows the results controls. It survives a tab
     * change — visibility is tab-scoped ([isExploreResultsVisible]) — and only
     * leaving the mode (back) or emptying the field clears it; the holder reset
     * rides on [exitExploreResults].
     */
    var exploreResults: Boolean by mutableStateOf(false)
        private set

    /** The last sent DISCOVER query, restored into the field when it closes. */
    private var exploreSentQuery: String = ""

    /**
     * The favourites "View all" page's edit mode. Root owns it because it is a
     * page state with a back step: back exits it before the page, and leaving
     * the page or the tab drops it.
     */
    var favoritesEditing: Boolean by mutableStateOf(false)
        private set

    fun toggleFavoritesEditing() {
        favoritesEditing = !favoritesEditing
    }

    fun exitFavoritesEdit() {
        favoritesEditing = false
    }

    val isSearchActive: Boolean get() = searchActiveTab != null

    /**
     * Whether the in-context search field is actually painted. It is a tab-surface
     * affordance, so any surface that draws over it — a detail page, the editor or
     * an [OverlayLayer.ABOVE_DETAIL] overlay (settings, sign-in, profile share) —
     * replaces it. The search is NOT closed (tab + query stay), so it just
     * reappears on back.
     */
    val isSearchVisible: Boolean
        get() = isSearchActive && !isDetailOpen && !isEditorOpen &&
            openOverlays.none { it.layer == OverlayLayer.ABOVE_DETAIL }

    /**
     * Whether the Explore results toolbar owns the DISCOVER tab right now: the
     * field is collapsed, no detail draws over it, and the user is on DISCOVER.
     * The raw [exploreResults] flag survives a tab change; only this getter is
     * tab-scoped, so ANIME/MANGA never inherit Explore's title, actions or back.
     */
    val isExploreResultsVisible: Boolean
        get() = exploreResults && selectedTab == TabSection.DISCOVER && !isSearchActive && !isDetailOpen

    val currentDetail: DetailDestination? get() = detailStack.lastOrNull()
    val isDetailOpen: Boolean get() = detailStack.isNotEmpty()

    /** Identity of the top detail entry (0 when the stack is empty). */
    val currentDetailEntryId: Long get() = entryIds.lastOrNull() ?: 0L

    /** Live entry identities, in stack order: Root keys and prunes per-entry state. */
    val detailEntryIds: List<Long> get() = entryIds

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
        // Results mode: an unpublished edit is discarded when the field closes,
        // so the title, the field and the results agree on the sent query.
        if (exploreResults) searchQueries[TabSection.DISCOVER] = exploreSentQuery
    }

    /** Enters Explore results mode after a DISCOVER submit. */
    fun enterExploreResults(sentQuery: String) {
        exploreResults = true
        exploreSentQuery = sentQuery
        searchQueries[TabSection.DISCOVER] = sentQuery
    }

    /** Leaves Explore results mode and forgets the sent query. */
    fun exitExploreResults() {
        exploreResults = false
        exploreSentQuery = ""
        searchQueries[TabSection.DISCOVER] = ""
    }

    fun selectTab(tab: TabSection) {
        selectedTab = tab
        // Queries are preserved per tab in the map, but the search surface
        // always closes (and restores its sent query in results mode): it
        // belongs to the tab where it opened and must never show another tab's
        // query. Explore results mode itself is kept.
        closeSearch()
        // The favourites edit mode belongs to its page: leaving the tab exits it.
        exitFavoritesEdit()
    }

    fun nextBackStep(): BackStep = when {
        openOverlays.any { it.layer == OverlayLayer.SHEET } -> BackStep.CloseSheet
        // Surfaces drawn ABOVE the detail page (modal sheets + settings), plus
        // the editor, which is not part of the overlay set.
        isEditorOpen || openOverlays.any { it.layer == OverlayLayer.ABOVE_DETAIL } -> BackStep.CloseOverlay
        // The favourites "View all" page's edit mode sits on top of its own page:
        // back leaves the mode before it closes the page.
        favoritesEditing && currentDetail is DetailDestination.ProfileFavorites -> BackStep.CloseFavoritesEdit
        // The detail page sits over the tab content (search included) and over
        // the below-detail overlays, so it closes before them.
        isDetailOpen -> BackStep.CloseDetail
        // The search belongs to the tab surface: it closes after the detail,
        // but before the overlays drawn below the detail. The results mode sits
        // right behind the field.
        isSearchActive -> BackStep.CloseSearch
        isExploreResultsVisible -> BackStep.CloseExploreResults
        openOverlays.any { it.layer == OverlayLayer.BELOW_DETAIL } -> BackStep.CloseOverlay
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
        BackStep.CloseExploreResults -> {
            exitExploreResults()
            true
        }
        BackStep.CloseFavoritesEdit -> {
            exitFavoritesEdit()
            true
        }
        BackStep.CloseSheet -> {
            openOverlays = openOverlays.filter { it.layer != OverlayLayer.SHEET }.toSet()
            true
        }
        BackStep.CloseOverlay -> {
            // No sheet can be open here (the sheet branch runs first), so this
            // closes both the above-detail and below-detail layers, exactly as
            // the previous per-flag close calls did.
            openOverlays = openOverlays.filter { it.layer == OverlayLayer.SHEET }.toSet()
            closeEditor()
            true
        }
        BackStep.CloseDetail -> {
            closeDetail()
            true
        }
        BackStep.SystemDefault -> false
    }

    private fun push(dest: DetailDestination) {
        close(RootOverlay.DetailMenu)
        close(RootOverlay.DetailSort)
        if (detailStack.lastOrNull() != dest) {
            detailStack.add(dest)
            entryIds.add(nextEntryId++)
        }
    }

    fun openMedia(id: Int) {
        // A detail supersedes the settings surface (a deep link can arrive
        // while Settings is open): close it so the detail is actually visible.
        close(RootOverlay.Settings)
        push(DetailDestination.Media(id))
    }
    fun openCharacter(id: Int) = push(DetailDestination.Character(id))
    fun openStaff(id: Int) = push(DetailDestination.Staff(id))
    fun openStudio(id: Int) = push(DetailDestination.Studio(id))
    fun openGenre(name: String, type: MediaType) = push(DetailDestination.Genre(name, type))
    fun openCatalog(kindId: String) = push(DetailDestination.Catalog(kindId))
    fun openProfileFavorites(categoryId: String) =
        push(DetailDestination.ProfileFavorites(categoryId))
    fun openDetailList(kind: DetailListKind, parentId: Int) =
        push(DetailDestination.DetailList(kind, parentId))
    fun closeDetail() {
        close(RootOverlay.DetailMenu)
        close(RootOverlay.DetailSort)
        // The page that owns the edit mode is leaving: drop it.
        exitFavoritesEdit()
        if (detailStack.isNotEmpty()) {
            detailStack.removeAt(detailStack.lastIndex)
            entryIds.removeAt(entryIds.lastIndex)
        }
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
        exploreResults = false
        exploreSentQuery = ""
        favoritesEditing = false
        openOverlays = emptySet()
    }
}

/** Composition factory: the shell's startup state, seeded from "Pestaña al abrir". */
@Composable
internal fun rememberState(
    startDestination: StartDestination = StartDestination(),
): State = remember {
    State(
        initialTab = startDestination.tab,
        initialAnimeSection = startDestination.animeSection,
        initialMangaSection = startDestination.mangaSection,
    )
}
