package com.luum.michi.app.profile.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.language.domain.networkErrorMessage
import com.luum.michi.app.core.media.domain.MediaType
import com.luum.michi.app.core.model.compactCount
import com.luum.michi.app.core.session.domain.Viewer
import com.luum.michi.app.profile.domain.model.ProfileFavorite
import com.luum.michi.app.profile.domain.model.ProfileFavoritesCategory
import com.luum.michi.app.profile.domain.model.ProfileStats
import com.luum.michi.app.profile.domain.model.itemsFor
import com.luum.michi.app.profile.domain.model.nonEmptyCategories
import com.luum.michi.app.profile.ui.model.title
import com.luum.michi.app.profile.ui.model.toRailItem
import com.luum.michi.app.profile.ui.state.ProfileStateHolder
import com.luum.michi.app.ui.components.Avatar
import com.luum.michi.app.ui.components.AvatarSize
import com.luum.michi.app.ui.components.LoadStateContainer
import com.luum.michi.app.ui.components.MediaRail
import com.luum.michi.app.ui.components.OptionGroup
import com.luum.michi.app.ui.components.PanelShape
import com.luum.michi.app.ui.components.RailHeader
import com.luum.michi.app.ui.components.RailHorizontalPadding
import com.luum.michi.app.ui.components.SettingsRow
import com.luum.michi.app.ui.components.flatBorder
import com.luum.michi.app.ui.components.solidContainerColor
import com.luum.michi.app.ui.language.Strings

/**
 * PROFILE tab: the viewer's header (avatar + name + counters) over their
 * favourites, one rail per category with a "View all" page. Only the viewer's
 * own profile is loaded ([ProfileStateHolder.load] with the viewer id); the
 * name/avatar come from the already-loaded [Viewer] — no extra request.
 * Read-only: favourites are removed from each category's "View all" page.
 */
@Composable
internal fun ProfileScreen(
    holder: ProfileStateHolder,
    viewer: Viewer,
    strings: LanguageStrings = Strings.current,
    bottomPadding: Dp = 0.dp,
    onOpenFavorite: (ProfileFavoritesCategory, Int) -> Unit,
    onViewAll: (ProfileFavoritesCategory) -> Unit,
    onOpenStatistics: (MediaType) -> Unit,
    onOpenFollowing: () -> Unit,
    onOpenFollowers: () -> Unit,
    onOpenReviews: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Explicit load on entry: the TTL dedups re-entries (0 requests while fresh).
    LaunchedEffect(holder, viewer.id) {
        holder.load(viewer.id)
    }
    ProfileContent(
        holder = holder,
        viewer = viewer,
        strings = strings,
        bottomPadding = bottomPadding,
        onOpenFavorite = onOpenFavorite,
        onViewAll = onViewAll,
        onOpenStatistics = onOpenStatistics,
        onOpenFollowing = onOpenFollowing,
        onOpenFollowers = onOpenFollowers,
        onOpenReviews = onOpenReviews,
        modifier = modifier,
    )
}

@Composable
private fun ProfileContent(
    holder: ProfileStateHolder,
    viewer: Viewer,
    strings: LanguageStrings,
    bottomPadding: Dp,
    onOpenFavorite: (ProfileFavoritesCategory, Int) -> Unit,
    onViewAll: (ProfileFavoritesCategory) -> Unit,
    onOpenStatistics: (MediaType) -> Unit,
    onOpenFollowing: () -> Unit,
    onOpenFollowers: () -> Unit,
    onOpenReviews: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val errorMessage = holder.error?.let { strings.networkErrorMessage(it) }
    val favorites = holder.favorites
    val categories = favorites.nonEmptyCategories()
    LoadStateContainer(
        hasContent = holder.hasContent,
        // Pull-to-refresh drives the refresh branch (keeps the profile on screen).
        isLoading = holder.isLoading || holder.isRefreshing,
        errorMessage = errorMessage,
        loadingLabel = strings.profileLoadingLabel,
        waitingLabel = strings.networkThrottleWaitingLabel,
        errorTitle = strings.profileErrorLabel,
        retryLabel = strings.retryAction,
        onRetry = { holder.refresh() },
        modifier = modifier.fillMaxSize(),
        // With the profile already on screen, a failed load is a compact line.
        refreshError = if (holder.hasContent) errorMessage else null,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp + bottomPadding),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item(key = "profile-header") {
                ProfileHeader(
                    viewer = viewer,
                    stats = holder.stats,
                    strings = strings,
                    onOpenStatistics = onOpenStatistics,
                    onOpenFollowing = onOpenFollowing,
                    onOpenFollowers = onOpenFollowers,
                )
            }
            item(key = "profile-reviews") {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    OptionGroup(title = null) {
                        SettingsRow(
                            label = strings.profileReviewsTitle,
                            value = "",
                            onClick = onOpenReviews,
                            divider = false,
                        )
                    }
                }
            }
            if (categories.isEmpty()) {
                item(key = "profile-favorites-empty") {
                    Text(
                        text = strings.profileFavoritesGridEmptyLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
            categories.forEach { category ->
                val items = favorites.itemsFor(category)
                item(key = "profile-favorites-${category.name}") {
                    if (category == ProfileFavoritesCategory.STUDIOS) {
                        ProfileStudioSection(
                            items = items,
                            title = category.title(strings),
                            viewAllLabel = strings.viewAllAction,
                            onSelect = { id -> onOpenFavorite(category, id) },
                            onViewAll = { onViewAll(category) },
                        )
                    } else {
                        MediaRail(
                            title = category.title(strings),
                            items = items.map { it.toRailItem() },
                            onSelect = { id -> onOpenFavorite(category, id) },
                            onViewAll = { onViewAll(category) },
                            viewAllLabel = strings.viewAllAction,
                        )
                    }
                }
            }
        }
    }
}

/** Flat header panel: avatar + name + the four counters (no hero, no banner).
 *  Each counter is a button that opens Statistics (anime/manga) or the
 *  following/followers list. */
@Composable
private fun ProfileHeader(
    viewer: Viewer,
    stats: ProfileStats,
    strings: LanguageStrings,
    onOpenStatistics: (MediaType) -> Unit,
    onOpenFollowing: () -> Unit,
    onOpenFollowers: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Surface(
            shape = PanelShape,
            color = solidContainerColor(),
            border = flatBorder(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(
                        imageUrl = viewer.avatarUrl,
                        initials = viewer.initials(),
                        size = AvatarSize.Header,
                    )
                    Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                        Text(
                            text = viewer.name,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    ProfileStatColumn(
                        value = stats.animeCount.compactCount(),
                        label = strings.animeLabel,
                        onClick = { onOpenStatistics(MediaType.ANIME) },
                        modifier = Modifier.weight(1f),
                    )
                    ProfileStatColumn(
                        value = stats.mangaCount.compactCount(),
                        label = strings.mangaLabel,
                        onClick = { onOpenStatistics(MediaType.MANGA) },
                        modifier = Modifier.weight(1f),
                    )
                    ProfileStatColumn(
                        value = stats.followingCount.compactCount(),
                        label = strings.profileFollowingLabel,
                        onClick = onOpenFollowing,
                        modifier = Modifier.weight(1f),
                    )
                    ProfileStatColumn(
                        value = stats.followersCount.compactCount(),
                        label = strings.profileFollowersLabel,
                        onClick = onOpenFollowers,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** One tappable counter: a 48dp-minimum touch target announced as a button. */
@Composable
private fun ProfileStatColumn(
    value: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Studio favourites: no image on AniList, so they are settings-style rows (the
 * same look as Explore's studio results) under a rail header with "View all".
 */
@Composable
private fun ProfileStudioSection(
    items: List<ProfileFavorite>,
    title: String,
    viewAllLabel: String,
    onSelect: (Int) -> Unit,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        RailHeader(
            title = title,
            viewAllLabel = viewAllLabel,
            onViewAll = onViewAll,
        )
        Column(modifier = Modifier.padding(horizontal = RailHorizontalPadding)) {
            OptionGroup(title = null) {
                items.forEachIndexed { index, studio ->
                    SettingsRow(
                        label = studio.name,
                        value = "",
                        onClick = { onSelect(studio.id) },
                        divider = index > 0,
                    )
                }
            }
        }
    }
}

/** Initials fallback for the avatar: first letters of the first two words. */
private fun Viewer.initials(): String =
    name.split(' ')
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { name.take(1).uppercase() }
