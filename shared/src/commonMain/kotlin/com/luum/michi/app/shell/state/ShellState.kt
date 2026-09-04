package com.luum.michi.app.shell.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.luum.michi.app.account.presentation.model.AccountFavoritesCategory
import com.luum.michi.app.account.presentation.model.AccountProfileDraft
import com.luum.michi.app.anime.presentation.model.AnimeListSection
import com.luum.michi.app.core.platform.PlatformBackHandler
import com.luum.michi.app.core.session.Viewer
import com.luum.michi.app.mediaDetail.presentation.model.MediaListStatus
import com.luum.michi.app.manga.presentation.model.MangaListSection
import com.luum.michi.app.shell.components.ShellTabSection

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
    initialTab: ShellTabSection = ShellTabSection.DISCOVER,
) {
    var selectedTab by mutableStateOf(initialTab)
    var selectedAnimeSection by mutableStateOf(AnimeListSection.ALL)
    var selectedMangaSection by mutableStateOf(MangaListSection.ALL)
    var accountRoute by mutableStateOf(ShellAccountRoute.ACCOUNT)
    var toolBarBackHandler by mutableStateOf<PlatformBackHandler?>(null)
    var currentProfile by mutableStateOf(initialProfile)
    private val detailStack = mutableStateListOf<DetailDestination>()
    var editorMediaId by mutableStateOf<Int?>(null)
    var editorInitialStatus by mutableStateOf<MediaListStatus?>(null)
    var editorInitialProgress by mutableStateOf<Int?>(null)
    var isDiscoverOpen by mutableStateOf(false)
    var isCalendarOpen by mutableStateOf(false)
    var isSeasonalOpen by mutableStateOf(false)
    var isNotificationsOpen by mutableStateOf(false)
    var isSectionFilterOpen by mutableStateOf(false)
    var isDiscoverFilterOpen by mutableStateOf(false)
    var favoritesCategory by mutableStateOf(AccountFavoritesCategory.ANIME)

    val currentDetail: DetailDestination? get() = detailStack.lastOrNull()
    val isDetailOpen: Boolean get() = detailStack.isNotEmpty()
    // Compat: muchos sitios comparan con el id de la obra abierta
    val selectedMediaId: Int? get() = (currentDetail as? DetailDestination.Media)?.id

    val isEditorOpen: Boolean
        get() = editorMediaId != null

    val isAccountDetail: Boolean
        get() = selectedTab == ShellTabSection.ACCOUNT && accountRoute != ShellAccountRoute.ACCOUNT

    fun selectTab(tab: ShellTabSection) {
        selectedTab = tab
        if (tab != ShellTabSection.ACCOUNT) {
            accountRoute = ShellAccountRoute.ACCOUNT
            toolBarBackHandler = null
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

    fun openDiscover() {
        isDiscoverOpen = true
    }

    fun closeDiscover() {
        isDiscoverOpen = false
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

    fun openSectionFilter() {
        isSectionFilterOpen = true
    }

    fun closeSectionFilter() {
        isSectionFilterOpen = false
    }

    fun openDiscoverFilter() {
        isDiscoverFilterOpen = true
    }

    fun closeDiscoverFilter() {
        isDiscoverFilterOpen = false
    }

    fun handleAccountBack() {
        val handler = toolBarBackHandler
        if (handler != null) handler() else accountRoute = ShellAccountRoute.ACCOUNT
    }
}

@Composable
internal fun rememberShellState(
    viewer: Viewer,
    initialTab: ShellTabSection = ShellTabSection.DISCOVER,
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
