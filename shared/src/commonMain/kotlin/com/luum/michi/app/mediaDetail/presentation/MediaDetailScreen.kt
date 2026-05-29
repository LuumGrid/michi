package com.luum.michi.app.mediaDetail.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.language.LanguageStrings
import com.luum.michi.app.core.platform.components.PlatformChips
import com.luum.michi.app.core.platform.components.PlatformListLoading
import com.luum.michi.app.core.platform.components.PlatformListMessage
import com.luum.michi.app.core.platform.components.PlatformListMessageTone
import com.luum.michi.app.mediaDetail.presentation.components.ActivitiesTab
import com.luum.michi.app.mediaDetail.presentation.components.CharactersTab
import com.luum.michi.app.mediaDetail.presentation.components.ConnectionsTab
import com.luum.michi.app.mediaDetail.presentation.components.FollowingTab
import com.luum.michi.app.mediaDetail.presentation.components.MediaDetailHeader
import com.luum.michi.app.mediaDetail.presentation.components.OverviewTab
import com.luum.michi.app.mediaDetail.presentation.components.ReviewsTab
import com.luum.michi.app.mediaDetail.presentation.components.StaffTab
import com.luum.michi.app.mediaDetail.presentation.components.StatsTab
import com.luum.michi.app.mediaDetail.presentation.components.ThreadsTab
import com.luum.michi.app.mediaDetail.presentation.model.MediaDetail
import com.luum.michi.app.mediaDetail.presentation.state.MediaDetailStateHolder

private enum class DetailTab {
    OVERVIEW,
    CONNECTIONS,
    CHARACTERS,
    STAFF,
    REVIEWS,
    THREADS,
    FOLLOWING,
    ACTIVITIES,
    STATS,
}

@Composable
internal fun MediaDetailScreen(
    mediaId: Int,
    stateHolder: MediaDetailStateHolder,
    onRequestEdit: (Int) -> Unit,
    onOpenRelation: (Int) -> Unit,
    onOpenStudio: (Int) -> Unit = {},
    onOpenCharacter: (Int) -> Unit = {},
    onOpenStaff: (Int) -> Unit = {},
) {
    val strings = LanguageProvider.strings

    LaunchedEffect(mediaId) { stateHolder.load(mediaId) }

    val detail = stateHolder.detail
    when {
        detail != null && detail.id == mediaId -> MediaDetailContent(
            detail = detail,
            stateHolder = stateHolder,
            strings = strings,
            onEditClick = { onRequestEdit(mediaId) },
            onOpenRelation = onOpenRelation,
            onOpenStudio = onOpenStudio,
            onOpenCharacter = onOpenCharacter,
            onOpenStaff = onOpenStaff,
        )
        stateHolder.isLoading -> PlatformListLoading(label = strings.mediaDetailLoadingLabel)
        stateHolder.error != null -> PlatformListMessage(
            title = strings.mediaDetailErrorLabel,
            subtitle = stateHolder.error,
            tone = PlatformListMessageTone.Error,
        )
        else -> PlatformListLoading(label = strings.mediaDetailLoadingLabel)
    }
}

@Composable
private fun MediaDetailContent(
    detail: MediaDetail,
    stateHolder: MediaDetailStateHolder,
    strings: LanguageStrings,
    onEditClick: () -> Unit,
    onOpenRelation: (Int) -> Unit,
    onOpenStudio: (Int) -> Unit = {},
    onOpenCharacter: (Int) -> Unit = {},
    onOpenStaff: (Int) -> Unit = {},
) {
    val tabs = remember { DetailTab.entries }
    var selectedTabIndex by rememberSaveable { mutableStateOf(0) }
    val selectedTab = tabs[selectedTabIndex.coerceIn(0, tabs.lastIndex)]
    var showCoverViewer by remember { mutableStateOf(false) }
    var showBannerViewer by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        MediaDetailHeader(
            detail = detail,
            onCoverClick = { showCoverViewer = true },
            onBannerClick = { showBannerViewer = true },
        )
        MediaDetailEntryAction(detail = detail, strings = strings, onClick = onEditClick)
        MediaDetailTabBar(
            selected = selectedTab,
            onSelect = { selectedTabIndex = tabs.indexOf(it) },
            strings = strings,
        )
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                DetailTab.OVERVIEW -> OverviewTab(
                    detail = detail,
                    strings = strings,
                    onOpenRelation = onOpenRelation,
                    onOpenStudio = onOpenStudio,
                )
                DetailTab.CONNECTIONS -> ConnectionsTab(
                    stateHolder = stateHolder,
                    strings = strings,
                    onOpenRelation = onOpenRelation,
                )
                DetailTab.CHARACTERS -> CharactersTab(
                    stateHolder = stateHolder,
                    strings = strings,
                    onOpenCharacter = onOpenCharacter,
                    onOpenStaff = onOpenStaff,
                )
                DetailTab.STAFF -> StaffTab(
                    stateHolder = stateHolder,
                    strings = strings,
                    onOpenStaff = onOpenStaff,
                )
                DetailTab.REVIEWS -> ReviewsTab(
                    stateHolder = stateHolder,
                    strings = strings,
                )
                DetailTab.THREADS -> ThreadsTab(
                    stateHolder = stateHolder,
                    strings = strings,
                )
                DetailTab.FOLLOWING -> FollowingTab(
                    stateHolder = stateHolder,
                    strings = strings,
                )
                DetailTab.ACTIVITIES -> ActivitiesTab(
                    stateHolder = stateHolder,
                    strings = strings,
                )
                DetailTab.STATS -> StatsTab(
                    detail = detail,
                    strings = strings,
                )
            }
        }
    }

    if (showCoverViewer && !detail.coverUrl.isNullOrBlank()) {
        FullscreenImageOverlay(
            imageUrl = detail.coverUrl!!,
            contentDescription = detail.title,
            onDismiss = { showCoverViewer = false },
        )
    }

    if (showBannerViewer && !detail.bannerUrl.isNullOrBlank()) {
        FullscreenImageOverlay(
            imageUrl = detail.bannerUrl!!,
            contentDescription = detail.title,
            onDismiss = { showBannerViewer = false },
        )
    }
}

private fun DetailTab.label(strings: LanguageStrings): String = when (this) {
    DetailTab.OVERVIEW -> strings.mediaDetailTabOverview
    DetailTab.CONNECTIONS -> strings.mediaDetailTabConnections
    DetailTab.CHARACTERS -> strings.mediaDetailTabCharacters
    DetailTab.STAFF -> strings.mediaDetailTabStaff
    DetailTab.REVIEWS -> strings.mediaDetailTabReviews
    DetailTab.THREADS -> strings.mediaDetailTabThreads
    DetailTab.FOLLOWING -> strings.mediaDetailTabFollowing
    DetailTab.ACTIVITIES -> strings.mediaDetailTabActivities
    DetailTab.STATS -> strings.mediaDetailTabStats
}

@Composable
private fun MediaDetailTabBar(
    selected: DetailTab,
    onSelect: (DetailTab) -> Unit,
    strings: LanguageStrings,
) {
    val tabs = remember { DetailTab.entries }
    PlatformChips(
        items = tabs,
        selectedItem = selected,
        onSelect = onSelect,
        label = { tab -> tab.label(strings) },
        useSoftActiveColor = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun MediaDetailEntryAction(
    detail: MediaDetail,
    strings: LanguageStrings,
    onClick: () -> Unit,
) {
    val label = if (detail.viewerEntry != null) strings.mediaDetailEditEntryAction
    else strings.mediaDetailAddToListAction
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(label, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun FullscreenImageOverlay(
    imageUrl: String,
    contentDescription: String?,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { onDismiss() },
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { },
            )
        }
    }
}
