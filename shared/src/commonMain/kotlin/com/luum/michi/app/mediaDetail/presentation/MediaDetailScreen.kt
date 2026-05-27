package com.luum.michi.app.mediaDetail.presentation

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
) {
    var selectedTab by remember(detail.id) { mutableStateOf(DetailTab.OVERVIEW) }

    Column(modifier = Modifier.fillMaxSize()) {
        MediaDetailHeader(detail = detail)
        MediaDetailEntryAction(detail = detail, strings = strings, onClick = onEditClick)
        MediaDetailTabBar(
            selected = selectedTab,
            onSelect = { selectedTab = it },
            strings = strings,
        )
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                DetailTab.OVERVIEW -> OverviewTab(
                    detail = detail,
                    strings = strings,
                    onOpenRelation = onOpenRelation,
                )
                DetailTab.CONNECTIONS -> ConnectionsTab(
                    stateHolder = stateHolder,
                    strings = strings,
                    onOpenRelation = onOpenRelation,
                )
                DetailTab.CHARACTERS -> CharactersTab(
                    stateHolder = stateHolder,
                    strings = strings,
                )
                DetailTab.STAFF -> StaffTab(
                    stateHolder = stateHolder,
                    strings = strings,
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
