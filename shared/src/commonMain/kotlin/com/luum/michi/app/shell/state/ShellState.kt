package com.luum.michi.app.shell.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.luum.michi.app.account.presentation.model.AccountFavoritesCategory
import com.luum.michi.app.account.presentation.model.AccountProfileDraft
import com.luum.michi.app.animation.presentation.model.AnimationListSection
import com.luum.michi.app.core.platform.PlatformBackHandler
import com.luum.michi.app.core.session.Viewer
import com.luum.michi.app.mediaDetail.presentation.model.MediaListStatus
import com.luum.michi.app.reading.presentation.model.ReadingListSection
import com.luum.michi.app.shell.components.ShellBottomTab

internal enum class ShellAccountRoute {
    ACCOUNT,
    SETTINGS,
    EDIT_PROFILE,
    SHARE_PROFILE,
    STATS,
    FAVORITES,
}

internal class ShellState(
    initialProfile: AccountProfileDraft,
    initialTab: ShellBottomTab = ShellBottomTab.HOME,
) {
    var selectedTab by mutableStateOf(initialTab)
    var selectedAnimationSection by mutableStateOf(AnimationListSection.ALL)
    var selectedReadingSection by mutableStateOf(ReadingListSection.ALL)
    var accountRoute by mutableStateOf(ShellAccountRoute.ACCOUNT)
    var topBarBackHandler by mutableStateOf<PlatformBackHandler?>(null)
    var isSearchActive by mutableStateOf(false)
    var searchQuery by mutableStateOf("")
    var currentProfile by mutableStateOf(initialProfile)
    private val detailStack = mutableStateListOf<DetailDestination>()
    var editorMediaId by mutableStateOf<Int?>(null)
    var editorInitialStatus by mutableStateOf<MediaListStatus?>(null)
    var editorInitialProgress by mutableStateOf<Int?>(null)
    var isExploreOpen by mutableStateOf(false)
    var isCalendarOpen by mutableStateOf(false)
    var isSeasonalOpen by mutableStateOf(false)
    var isNotificationsOpen by mutableStateOf(false)
    var favoritesCategory by mutableStateOf(AccountFavoritesCategory.ANIME)

    val currentDetail: DetailDestination? get() = detailStack.lastOrNull()
    val isDetailOpen: Boolean get() = detailStack.isNotEmpty()
    // Compat: muchos sitios comparan con el id de la obra abierta
    val selectedMediaId: Int? get() = (currentDetail as? DetailDestination.Media)?.id

    val isEditorOpen: Boolean
        get() = editorMediaId != null

    val isAccountDetail: Boolean
        get() = selectedTab == ShellBottomTab.ACCOUNT && accountRoute != ShellAccountRoute.ACCOUNT

    val isSearchTab: Boolean
        get() = selectedTab == ShellBottomTab.HOME ||
            selectedTab == ShellBottomTab.ANIMATION ||
            selectedTab == ShellBottomTab.READING

    fun closeSearch() {
        isSearchActive = false
        searchQuery = ""
    }

    fun openSearch() {
        isSearchActive = true
    }

    fun selectTab(tab: ShellBottomTab) {
        selectedTab = tab
        closeSearch()
        if (tab != ShellBottomTab.ACCOUNT) {
            accountRoute = ShellAccountRoute.ACCOUNT
            topBarBackHandler = null
        }
    }

    fun searchGlobally() {
        selectedTab = ShellBottomTab.HOME
        accountRoute = ShellAccountRoute.ACCOUNT
        topBarBackHandler = null
        isExploreOpen = true
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
        accountRoute = ShellAccountRoute.FAVORITES
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

    fun openSeasonal() {
        isSeasonalOpen = true
    }

    fun closeSeasonal() {
        isSeasonalOpen = false
    }

    fun openNotifications() {
        isNotificationsOpen = true
    }

    fun closeNotifications() {
        isNotificationsOpen = false
    }

    fun handleAccountBack() {
        val handler = topBarBackHandler
        if (handler != null) handler() else accountRoute = ShellAccountRoute.ACCOUNT
    }
}

@Composable
internal fun rememberShellState(
    viewer: Viewer,
    initialTab: ShellBottomTab = ShellBottomTab.HOME,
): ShellState {
    return remember(viewer.id) {
        ShellState(
            initialProfile = viewer.toAccountProfileDraft(),
            initialTab = initialTab,
        )
    }
}

private fun Viewer.toAccountProfileDraft(): AccountProfileDraft = AccountProfileDraft(
    username = name,
    displayName = name,
    avatarUrl = avatarUrl,
    bannerUrl = bannerUrl,
    bio = about.orEmpty(),
)
