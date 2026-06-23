package com.luum.michi.app.notifications.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.language.networkErrorMessage
import com.luum.michi.app.core.platform.components.PlatformListLoading
import com.luum.michi.app.core.platform.components.PlatformListMessage
import com.luum.michi.app.core.platform.components.PlatformListMessageTone
import com.luum.michi.app.core.platform.components.bottomNavBarClearance
import com.luum.michi.app.notifications.presentation.components.NotificationCard
import com.luum.michi.app.notifications.presentation.model.NotificationTarget
import com.luum.michi.app.notifications.presentation.state.NotificationsStateHolder

@Composable
internal fun NotificationsScreen(
    stateHolder: NotificationsStateHolder,
    onOpen: (NotificationTarget) -> Unit,
) {
    val strings = LanguageProvider.strings
    val listState = rememberLazyListState()

    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= info.totalItemsCount - 4
        }
    }

    LaunchedEffect(nearEnd) {
        if (nearEnd) stateHolder.loadMore()
    }

    when {
        stateHolder.isLoading -> PlatformListLoading(label = strings.listsLoadingLabel)
        stateHolder.error != null -> PlatformListMessage(
            title = strings.notificationsErrorLabel,
            subtitle = strings.networkErrorMessage(stateHolder.error!!),
            tone = PlatformListMessageTone.Error,
            actionLabel = strings.mediaDetailLoadMoreAction,
            onAction = { stateHolder.load() },
        )
        stateHolder.notifications.isEmpty() -> PlatformListMessage(title = strings.notificationsEmptyLabel)
        else -> LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = bottomNavBarClearance()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(
                items = stateHolder.notifications,
                key = { it.id },
            ) { notification ->
                NotificationCard(
                    notification = notification,
                    onClick = { onOpen(notification.target) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (stateHolder.isLoadingMore) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}
