package com.luum.michi.app.account.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import coil3.compose.AsyncImage
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.MichiBrand
import com.luum.michi.app.core.platform.PlatformIcons
import kotlinx.coroutines.launch

private data class AccountPostPreview(
    val id: Int,
    val colors: List<Color>,
)

private val AccountPostPreviews = listOf(
    AccountPostPreview(1, listOf(Color(0xFF1E293B), Color(0xFF64748B))),
    AccountPostPreview(2, listOf(Color(0xFF7C2D12), Color(0xFFF97316))),
    AccountPostPreview(3, listOf(Color(0xFF164E63), Color(0xFF22D3EE))),
    AccountPostPreview(4, listOf(Color(0xFF3F3F46), Color(0xFFA1A1AA))),
    AccountPostPreview(5, listOf(Color(0xFF581C87), Color(0xFFD8B4FE))),
    AccountPostPreview(6, listOf(Color(0xFF14532D), Color(0xFF86EFAC))),
    AccountPostPreview(7, listOf(Color(0xFF831843), Color(0xFFF9A8D4))),
    AccountPostPreview(8, listOf(Color(0xFF312E81), Color(0xFFA5B4FC))),
    AccountPostPreview(9, listOf(Color(0xFF713F12), Color(0xFFFDE68A))),
    AccountPostPreview(10, listOf(Color(0xFF0F172A), Color(0xFF38BDF8))),
    AccountPostPreview(11, listOf(Color(0xFF365314), Color(0xFFA3E635))),
    AccountPostPreview(12, listOf(Color(0xFF701A75), Color(0xFFF0ABFC))),
    AccountPostPreview(13, listOf(Color(0xFF1E293B), Color(0xFF64748B))),
    AccountPostPreview(14, listOf(Color(0xFF7C2D12), Color(0xFFF97316))),
    AccountPostPreview(15, listOf(Color(0xFF164E63), Color(0xFF22D3EE))),
    AccountPostPreview(16, listOf(Color(0xFF3F3F46), Color(0xFFA1A1AA))),
    AccountPostPreview(17, listOf(Color(0xFF581C87), Color(0xFFD8B4FE))),
    AccountPostPreview(18, listOf(Color(0xFF14532D), Color(0xFF86EFAC))),
    AccountPostPreview(19, listOf(Color(0xFF831843), Color(0xFFF9A8D4))),
    AccountPostPreview(20, listOf(Color(0xFF312E81), Color(0xFFA5B4FC))),
    AccountPostPreview(21, listOf(Color(0xFF713F12), Color(0xFFFDE68A))),
    AccountPostPreview(22, listOf(Color(0xFF0F172A), Color(0xFF38BDF8))),
    AccountPostPreview(23, listOf(Color(0xFF365314), Color(0xFFA3E635))),
    AccountPostPreview(24, listOf(Color(0xFF701A75), Color(0xFFF0ABFC))),
)

private enum class AccountTab {
    POSTS,
    ANIMATION,
    READING,
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AccountScreen(
    brand: MichiBrand,
    username: String = "psyxho_skull",
    displayName: String = brand.appName,
    userAvatarUrl: String? = null,
    userBio: String? = null,
    onEditProfileClick: () -> Unit = {},
    onShareProfileClick: () -> Unit = {},
) {
    val posts = remember { AccountPostPreviews }
    val tabs = remember { AccountTab.entries }
    val pagerState = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(top = 96.dp, bottom = 16.dp),
    ) {
        item {
            AccountHeader(
                username = username,
                displayName = displayName,
                userAvatarUrl = userAvatarUrl,
                userBio = userBio,
                onEditProfileClick = onEditProfileClick,
                onShareProfileClick = onShareProfileClick,
            )
        }

        stickyHeader {
            AccountTabs(
                brand = brand,
                selectedTab = tabs[pagerState.currentPage],
                onSelect = { selected ->
                    scope.launch {
                        pagerState.animateScrollToPage(tabs.indexOf(selected))
                    }
                },
            )
        }

        item {
            AccountPostPager(
                posts = posts,
                tabs = tabs,
                pagerState = pagerState,
            )
        }
    }
}

@Composable
private fun AccountPostPager(
    posts: List<AccountPostPreview>,
    tabs: List<AccountTab>,
    pagerState: PagerState,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val spacing = 2.dp
        val rows = ceil(posts.size / 3f).toInt()
        val cellSize = (maxWidth - spacing * 2) / 3
        val pagerHeight = cellSize * rows + spacing * (rows - 1).coerceAtLeast(0)

        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            modifier = Modifier
                .fillMaxWidth()
                .height(pagerHeight),
        ) { page ->
            AccountPostGrid(
                posts = posts,
                tab = tabs[page],
                spacing = spacing,
            )
        }
    }
}

@Composable
private fun AccountPostGrid(
    posts: List<AccountPostPreview>,
    tab: AccountTab,
    spacing: androidx.compose.ui.unit.Dp,
) {
    Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
        posts.chunked(3).forEach { rowPosts ->
            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                rowPosts.forEach { post ->
                    AccountPostTile(
                        post = post,
                        tab = tab,
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(3 - rowPosts.size) {
                    Spacer(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountHeader(
    username: String,
    displayName: String,
    userAvatarUrl: String?,
    userBio: String?,
    onEditProfileClick: () -> Unit,
    onShareProfileClick: () -> Unit,
) {
    val strings = LanguageProvider.strings

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            AccountAvatar(
                username = username,
                userAvatarUrl = userAvatarUrl,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AccountStat(value = "12", label = strings.accountPostsLabel)
                AccountStat(value = "1.8K", label = strings.accountFollowersLabel)
                AccountStat(value = "284", label = strings.accountFollowingLabel)
            }
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = "@$username",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = displayName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (!userBio.isNullOrBlank()) {
            Text(
                text = userBio,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onEditProfileClick,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Text(strings.accountEditProfileAction, maxLines = 1)
            }

            OutlinedButton(
                onClick = onShareProfileClick,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Text(strings.accountShareProfileAction, maxLines = 1)
            }
        }
    }
}

@Composable
private fun AccountAvatar(
    username: String,
    userAvatarUrl: String?,
) {
    Box(
        modifier = Modifier
            .size(85.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFFFFC857),
                        Color(0xFFFF5C8A),
                        Color(0xFF6C63FF),
                    ),
                ),
            )
            .padding(3.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(4.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (!userAvatarUrl.isNullOrEmpty()) {
            AsyncImage(
                model = userAvatarUrl,
                contentDescription = username,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = username.take(2).uppercase(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AccountStat(value: String, label: String) {
    Column(
        modifier = Modifier.size(width = 76.dp, height = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountTabs(
    brand: MichiBrand,
    selectedTab: AccountTab,
    onSelect: (AccountTab) -> Unit,
) {
    val tabs = remember { AccountTab.entries }
    val selectedIndex = tabs.indexOf(selectedTab)

    PrimaryTabRow(
        selectedTabIndex = selectedIndex,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(20.dp),
        ),
    containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        divider = {},
        indicator = {
            TabRowDefaults.PrimaryIndicator(
                modifier = Modifier.tabIndicatorOffset(selectedIndex),
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = selectedIndex == index
            val interactionSource = remember { MutableInteractionSource() }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { onSelect(tab) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                AccountTabIcon(
                    painter = when (tab) {
                        AccountTab.POSTS -> PlatformIcons.Posts
                        AccountTab.ANIMATION -> PlatformIcons.Animation
                        AccountTab.READING -> PlatformIcons.Reading
                    },
                    contentDescription = when (tab) {
                        AccountTab.POSTS -> brand.postsLabel
                        AccountTab.ANIMATION -> brand.animationLabel
                        AccountTab.READING -> brand.readingLabel
                    },
                    selected = isSelected,
                )
            }
        }
    }
}

@Composable
private fun AccountTabIcon(
    painter: androidx.compose.ui.graphics.painter.Painter,
    contentDescription: String,
    selected: Boolean,
) {
    Icon(
        painter = painter,
        contentDescription = contentDescription,
        tint = if (selected) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier.size(22.dp),
    )
}

@Composable
private fun AccountPostTile(
    post: AccountPostPreview,
    tab: AccountTab,
    modifier: Modifier = Modifier,
) {
    val colors = remember(post, tab) {
        when (tab) {
            AccountTab.POSTS -> post.colors
            AccountTab.ANIMATION -> post.colors.reversed()
            AccountTab.READING -> listOf(post.colors.first(), Color.Black)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(Brush.linearGradient(colors)),
    )
}
