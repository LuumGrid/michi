package com.luum.michi.app.shell.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.PlatformIcons
import com.luum.michi.app.core.platform.components.PlatformFloatingActionGroup
import com.luum.michi.app.core.platform.components.PlatformFloatingBackButton
import com.luum.michi.app.core.platform.components.PlatformFloatingPillHeight
import com.luum.michi.app.core.platform.components.PlatformFloatingSearchContainer

/**
 * Toolbar global de la app, 100% flotante: se dibuja como overlay sobre el contenido
 * (ninguna pantalla reserva su espacio) y cada función es una píldora M3 Expressive
 * opaca — back aislado en círculo + grupos conectados + título en píldora.
 *
 * Sin search (vive abajo junto a la tab bar) y sin chips (absorbidos por el filter).
 * Derecha en listas: filter (secciones) + sort (orden) separados.
 * En el tab Discover, el centro y los trailing alternan con AnimatedContent:
 * Dashboard muestra título + ícono de catálogo; Explore muestra search field + sort + filtro.
 * El overlay de search es solo back, sin título ni acciones.
 */
@Composable
internal fun ShellToolBar(
    selectedTab: ShellTabSection,
    isAccountDetail: Boolean,
    isDetailOpen: Boolean,
    isExploreOpen: Boolean,
    isCalendarOpen: Boolean,
    isNotificationsOpen: Boolean,
    titleText: String,
    onAccountBack: () -> Unit,
    onMediaBack: () -> Unit,
    onExploreBack: () -> Unit,
    onCalendarBack: () -> Unit,
    onNotificationsBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onNotificationsClick: () -> Unit,
    onSortClick: () -> Unit,
    onSectionFilterClick: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenExplore: () -> Unit = {},
    exploreQuery: String = "",
    onExploreQueryChange: (String) -> Unit = {},
    exploreFocusRequested: Boolean = false,
    onExploreFocusConsumed: () -> Unit = {},
    onExploreFilterClick: () -> Unit = {},
    unreadCount: Int = 0,
    modifier: Modifier = Modifier,
) {
    val strings = LanguageProvider.strings

    // Back aislado (círculo propio). Null = sin back, se muestra grupo leading.
    // Search es overlay vacío: solo back, sin título ni acciones.
    val backAction: (() -> Unit)? = when {
        isDetailOpen -> onMediaBack
        isExploreOpen -> onExploreBack
        isCalendarOpen -> onCalendarBack
        isNotificationsOpen -> onNotificationsBack
        selectedTab == ShellTabSection.ACCOUNT && isAccountDetail -> onAccountBack
        else -> null
    }

    // Trailing con acciones solo donde hay algo que mostrar: en detail puro
    // y calendar el back queda aislado como en la referencia de Apple.
    val noOverlay = !isDetailOpen && !isCalendarOpen && !isNotificationsOpen
    val showExploreEntry = selectedTab == ShellTabSection.DISCOVER && !isExploreOpen && noOverlay
    val showExploreActions = isExploreOpen
    val showListActions = (selectedTab == ShellTabSection.ANIME ||
        selectedTab == ShellTabSection.MANGA) && noOverlay
    val showTrailingMain = (selectedTab == ShellTabSection.ACCOUNT && !isAccountDetail) &&
        !isDetailOpen && !isCalendarOpen && !isNotificationsOpen

    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(PlatformFloatingPillHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (backAction != null) {
            PlatformFloatingBackButton(onClick = backAction) {
                Icon(
                    painter = PlatformIcons.ChevronLeft,
                    contentDescription = strings.backButton,
                    modifier = Modifier.size(24.dp),
                )
            }
            // Explore no es overlay vacío: back + search field + acciones.
            if (!isExploreOpen) return@Column
        } else {
                ShellToolBarLeadingGroup(
                    selectedTab = selectedTab,
                    unreadCount = unreadCount,
                    onNotificationsClick = onNotificationsClick,
                    onOpenCalendar = onOpenCalendar,
                )
        }

        Spacer(modifier = Modifier.width(8.dp))

        PlatformFloatingSearchContainer(modifier = Modifier.weight(1f)) {
            if (isExploreOpen) {
                ShellSearchField(
                    query = exploreQuery,
                    onQueryChange = onExploreQueryChange,
                    placeholder = strings.discoverSearchPlaceholder,
                    autoFocus = false,
                    externalFocusRequest = exploreFocusRequested,
                    onFocusConsumed = onExploreFocusConsumed,
                )
            } else {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        }

        if (showExploreEntry || showExploreActions || showListActions || showTrailingMain) {
            Spacer(modifier = Modifier.width(8.dp))
            PlatformFloatingActionGroup {
                when {
                    showExploreEntry -> {
                        IconButton(onClick = onOpenExplore) {
                            Icon(
                                painter = PlatformIcons.Discover,
                                contentDescription = strings.discoverTitle,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                    showExploreActions -> {
                        IconButton(onClick = onExploreFilterClick) {
                            Icon(
                                painter = PlatformIcons.Filter,
                                contentDescription = strings.filterByLabel,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        IconButton(onClick = onSortClick) {
                            Icon(
                                painter = PlatformIcons.Sort,
                                contentDescription = strings.orderByLabel,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                    showListActions -> {
                        IconButton(onClick = onSectionFilterClick) {
                            Icon(
                                painter = PlatformIcons.Filter,
                                contentDescription = strings.filterByLabel,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        IconButton(onClick = onSortClick) {
                            Icon(
                                painter = PlatformIcons.Sort,
                                contentDescription = strings.orderByLabel,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                    else -> {
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                painter = PlatformIcons.Settings,
                                contentDescription = strings.settingsAction,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
            }
        }
        }
}
}

@Composable
private fun ShellToolBarLeadingGroup(
    selectedTab: ShellTabSection,
    unreadCount: Int,
    onNotificationsClick: () -> Unit,
    onOpenCalendar: () -> Unit,
) {
    val strings = LanguageProvider.strings

    PlatformFloatingActionGroup {
        IconButton(onClick = onNotificationsClick) {
            BadgedBox(
                badge = {
                    if (unreadCount > 0) {
                        Badge { Text(text = if (unreadCount > 99) "99+" else unreadCount.toString()) }
                    }
                },
            ) {
                Icon(
                    painter = PlatformIcons.Notifications,
                    contentDescription = strings.notificationsAction,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        // Calendar a la derecha de notifications (tabs principales).
        if (selectedTab == ShellTabSection.DISCOVER ||
            selectedTab == ShellTabSection.ANIME ||
            selectedTab == ShellTabSection.MANGA
        ) {
            IconButton(onClick = onOpenCalendar) {
                Icon(
                    painter = PlatformIcons.Calendar,
                    contentDescription = strings.calendarTitle,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}
