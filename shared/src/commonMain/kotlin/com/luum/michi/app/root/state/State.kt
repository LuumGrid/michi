package com.luum.michi.app.root.state

import com.luum.michi.app.account.domain.model.AccountFavoritesCategory
import com.luum.michi.app.account.domain.model.AccountProfileDraft
import com.luum.michi.app.core.navigation.domain.DetailDestination
import com.luum.michi.app.core.navigation.domain.TabSection
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListSection
import com.luum.michi.app.core.navigation.domain.BackHandler
import com.luum.michi.app.core.session.domain.Viewer
import com.luum.michi.app.core.medialist.domain.MediaListStatus
import com.luum.michi.app.mediaList.domain.manga.model.MangaListSection

internal enum class AccountRoute {
    ACCOUNT,
    SETTINGS,
    STATS,
    FAVORITES,
}

internal class State(
    initialProfile: AccountProfileDraft,
    initialTab: TabSection = TabSection.DISCOVER,
) {
    var selectedTab: TabSection = initialTab
    var selectedAnimeSection = AnimeListSection.ALL
    var selectedMangaSection = MangaListSection.ALL
    var accountRoute = AccountRoute.ACCOUNT
    var toolbarBackHandler: BackHandler? = null
    var currentProfile: AccountProfileDraft = initialProfile
    private val detailStack = mutableListOf<DetailDestination>()
    var editorMediaId: Int? = null
    var editorInitialStatus: MediaListStatus? = null
    var editorInitialProgress: Int? = null
    var isExploreOpen = false
    var isCalendarOpen = false
    var isNotificationsOpen = false
    var isSectionFilterOpen = false
    var isExploreFilterOpen = false
    var isShareProfileOpen = false
    var isAccountSettingsOpen = false
    var favoritesCategory = AccountFavoritesCategory.ANIME

    val currentDetail: DetailDestination? get() = detailStack.lastOrNull()
    val isDetailOpen: Boolean get() = detailStack.isNotEmpty()
    // Compat: muchos sitios comparan con el id de la obra abierta
    val selectedMediaId: Int? get() = (currentDetail as? DetailDestination.Media)?.id

    val isEditorOpen: Boolean
        get() = editorMediaId != null

    val isAccountDetail: Boolean
        get() = selectedTab == TabSection.ACCOUNT && accountRoute != AccountRoute.ACCOUNT

    fun selectTab(tab: TabSection) {
        selectedTab = tab
        if (tab != TabSection.ACCOUNT) {
            accountRoute = AccountRoute.ACCOUNT
            toolbarBackHandler = null
        }
    }

    private fun push(dest: DetailDestination) {
        if (detailStack.lastOrNull() != dest) detailStack.add(dest)
    }

    fun openMedia(id: Int) = push(DetailDestination.Media(id))
    fun openCharacter(id: Int) = push(DetailDestination.Character(id))
    fun openStaff(id: Int) = push(DetailDestination.Staff(id))
    fun openStudio(id: Int) = push(DetailDestination.Studio(id))
    fun closeDetail() { if (detailStack.isNotEmpty()) detailStack.removeAt(detailStack.lastIndex) }

    fun openFavoritesGrid(category: AccountFavoritesCategory) {
        favoritesCategory = category
        accountRoute = AccountRoute.FAVORITES
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

    fun openAccountSettings() {
        isAccountSettingsOpen = true
    }

    fun closeAccountSettings() {
        isAccountSettingsOpen = false
    }

    fun handleAccountBack() {
        val handler = toolbarBackHandler
        if (handler != null) handler() else accountRoute = AccountRoute.ACCOUNT
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

private fun Viewer.toAccountProfileDraft(): AccountProfileDraft = AccountProfileDraft(
    username = name,
    displayName = name,
    avatarUrl = avatarUrl,
    bannerUrl = bannerUrl,
    bio = about.orEmpty(),
)
