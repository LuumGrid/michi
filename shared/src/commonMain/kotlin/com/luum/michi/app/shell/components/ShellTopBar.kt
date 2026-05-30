package com.luum.michi.app.shell.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.PlatformIcons
import com.luum.michi.app.core.platform.components.PlatformTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShellTopBar(
    selectedTab: ShellBottomTab,
    isAccountDetail: Boolean,
    isDetailOpen: Boolean,
    isExploreOpen: Boolean,
    isCalendarOpen: Boolean,
    isSeasonalOpen: Boolean,
    isSearchActive: Boolean,
    isSearchTab: Boolean,
    searchQuery: String,
    titleText: String,
    scrollBehavior: TopAppBarScrollBehavior,
    onOpenSearch: () -> Unit,
    onOpenExplore: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onAccountBack: () -> Unit,
    onMediaBack: () -> Unit,
    onExploreBack: () -> Unit,
    onCalendarBack: () -> Unit,
    onSeasonalBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onNotificationsClick: () -> Unit,
    onFilterClick: () -> Unit,
    onForumClick: () -> Unit,
    onOpenDiscoverSort: () -> Unit = {},
    chips: @Composable () -> Unit = {},
    exploreQuery: String = "",
    onExploreQueryChange: (String) -> Unit = {},
) {
    val strings = LanguageProvider.strings
 
    Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
        PlatformTopBar(
            title = {
                if (isExploreOpen) {
                    ShellSearchField(
                        query = exploreQuery,
                        onQueryChange = onExploreQueryChange,
                        placeholder = strings.exploreSearchPlaceholder,
                        autoFocus = false,
                    )
                } else if (!isDetailOpen && isSearchTab && isSearchActive) {
                    ShellSearchField(
                        query = searchQuery,
                        onQueryChange = onSearchQueryChange,
                        placeholder = strings.homeSearchPlaceholder,
                    )
                } else {
                    Text(
                        text = titleText,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            navigationIcon = {
                if (isDetailOpen) {
                    IconButton(onClick = onMediaBack) {
                        Icon(
                            painter = PlatformIcons.ArrowBack,
                            contentDescription = strings.backButton,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                } else if (isExploreOpen) {
                    IconButton(onClick = onExploreBack) {
                        Icon(
                            painter = PlatformIcons.ArrowBack,
                            contentDescription = strings.backButton,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                } else if (isCalendarOpen) {
                    IconButton(onClick = onCalendarBack) {
                        Icon(
                            painter = PlatformIcons.ArrowBack,
                            contentDescription = strings.backButton,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                } else if (isSeasonalOpen) {
                    IconButton(onClick = onSeasonalBack) {
                        Icon(
                            painter = PlatformIcons.ArrowBack,
                            contentDescription = strings.backButton,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                } else {
                    ShellTopBarNavigationIcon(
                        selectedTab = selectedTab,
                        isSearchActive = isSearchActive,
                        isAccountDetail = isAccountDetail,
                        onCloseSearch = onCloseSearch,
                        onAccountBack = onAccountBack,
                        onNotificationsClick = onNotificationsClick,
                        onFilterClick = onFilterClick,
                    )
                }
            },
            actions = {
                if (isExploreOpen || isSeasonalOpen) {
                    IconButton(onClick = onOpenDiscoverSort) {
                        Icon(
                            painter = PlatformIcons.FilterList,
                            contentDescription = strings.filterAction,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                } else if (!isDetailOpen && !isCalendarOpen) {
                    ShellTopBarActions(
                        selectedTab = selectedTab,
                        isSearchActive = isSearchActive,
                        isAccountDetail = isAccountDetail,
                        onOpenSearch = onOpenSearch,
                        onOpenExplore = onOpenExplore,
                        onOpenSettings = onOpenSettings,
                        onForumClick = onForumClick,
                    )
                }
            },
            scrollBehavior = scrollBehavior,
            windowInsets = WindowInsets.statusBars,
        )

        if (!isDetailOpen && !isExploreOpen && !isCalendarOpen && !isSeasonalOpen) {
            chips()
        }
    }
}

@Composable
private fun ShellTopBarNavigationIcon(
    selectedTab: ShellBottomTab,
    isSearchActive: Boolean,
    isAccountDetail: Boolean,
    onCloseSearch: () -> Unit,
    onAccountBack: () -> Unit,
    onNotificationsClick: () -> Unit,
    onFilterClick: () -> Unit,
) {
    val strings = LanguageProvider.strings

    when (selectedTab) {
        ShellBottomTab.HOME,
        ShellBottomTab.ANIMATION,
        ShellBottomTab.READING -> {
            if (isSearchActive) {
                IconButton(onClick = onCloseSearch) {
                    Icon(
                        painter = PlatformIcons.ChevronLeft,
                        contentDescription = strings.backButton,
                        modifier = Modifier.size(28.dp),
                    )
                }
            } else {
                Row {
                    IconButton(onClick = onNotificationsClick) {
                        Icon(
                            painter = PlatformIcons.Notifications,
                            contentDescription = strings.notificationsAction,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    if (selectedTab == ShellBottomTab.ANIMATION || selectedTab == ShellBottomTab.READING) {
                        IconButton(onClick = onFilterClick) {
                            Icon(
                                painter = PlatformIcons.FilterList,
                                contentDescription = strings.filterAction,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                }
            }
        }

        ShellBottomTab.FEED -> {
            Row {
                IconButton(onClick = onNotificationsClick) {
                    Icon(
                        painter = PlatformIcons.Notifications,
                        contentDescription = strings.notificationsAction,
                        modifier = Modifier.size(28.dp),
                    )
                }
                IconButton(onClick = onFilterClick) {
                    Icon(
                        painter = PlatformIcons.FilterList,
                        contentDescription = strings.filterAction,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }

        ShellBottomTab.ACCOUNT -> {
            if (isAccountDetail) {
                IconButton(onClick = onAccountBack) {
                    Icon(
                        painter = PlatformIcons.ArrowBack,
                        contentDescription = strings.tabAccount,
                        modifier = Modifier.size(28.dp),
                    )
                }
            } else {
                IconButton(onClick = onNotificationsClick) {
                    Icon(
                        painter = PlatformIcons.Notifications,
                        contentDescription = strings.notificationsAction,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ShellTopBarActions(
    selectedTab: ShellBottomTab,
    isSearchActive: Boolean,
    isAccountDetail: Boolean,
    onOpenSearch: () -> Unit,
    onOpenExplore: () -> Unit,
    onOpenSettings: () -> Unit,
    onForumClick: () -> Unit,
) {
    val strings = LanguageProvider.strings

    when (selectedTab) {
        ShellBottomTab.HOME -> {
            IconButton(onClick = onOpenExplore) {
                Icon(
                    painter = PlatformIcons.Search,
                    contentDescription = strings.searchTitle,
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        ShellBottomTab.ANIMATION,
        ShellBottomTab.READING -> {
            if (!isSearchActive) {
                IconButton(onClick = onOpenSearch) {
                    Icon(
                        painter = PlatformIcons.Search,
                        contentDescription = strings.searchTitle,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }

        ShellBottomTab.FEED -> {
            IconButton(onClick = onForumClick) {
                Icon(
                    painter = PlatformIcons.Forum,
                    contentDescription = strings.forumAction,
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        ShellBottomTab.ACCOUNT -> {
            if (!isAccountDetail) {
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        painter = PlatformIcons.Settings,
                        contentDescription = strings.settingsAction,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
    }
}
