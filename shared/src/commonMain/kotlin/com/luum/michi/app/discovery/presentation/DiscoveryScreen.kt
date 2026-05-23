package com.luum.michi.app.discovery.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.language.LanguageStrings
import com.luum.michi.app.core.platform.PlatformIcons
import com.luum.michi.app.core.platform.components.PlatformHomeCommunityCard
import com.luum.michi.app.core.platform.components.PlatformHomeHeader
import com.luum.michi.app.core.platform.components.PlatformHomeMediaRail
import com.luum.michi.app.core.platform.components.PlatformHomeReleaseRail
import com.luum.michi.app.core.platform.components.PlatformHomeShortcut
import com.luum.michi.app.core.platform.components.PlatformHomeShortcutRow
import com.luum.michi.app.discovery.presentation.sample.DiscoveryReleasingTodaySample
import com.luum.michi.app.discovery.presentation.sample.DiscoveryTrendingAnimationSample
import com.luum.michi.app.discovery.presentation.sample.DiscoveryTrendingReadingSample

@Composable
fun DiscoveryScreen() {
    val strings = LanguageProvider.strings
    val shortcuts = discoveryShortcuts(strings)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item {
            PlatformHomeHeader(
                title = strings.homeGreetingTitle,
                subtitle = strings.homeGreetingSubtitle,
            )
        }
        item { PlatformHomeShortcutRow(items = shortcuts) }
        item {
            PlatformHomeReleaseRail(
                title = strings.homeReleasingTodayTitle,
                items = DiscoveryReleasingTodaySample,
            )
        }
        item {
            PlatformHomeCommunityCard(
                title = strings.homeCommunityTitle,
                subtitle = strings.homeCommunitySubtitle,
            )
        }
        item {
            PlatformHomeMediaRail(
                title = strings.homeTrendingAnimationTitle,
                items = DiscoveryTrendingAnimationSample,
            )
        }
        item {
            PlatformHomeMediaRail(
                title = strings.homeTrendingReadingTitle,
                items = DiscoveryTrendingReadingSample,
            )
        }
    }
}

@Composable
private fun discoveryShortcuts(strings: LanguageStrings): List<PlatformHomeShortcut> = listOf(
    PlatformHomeShortcut(strings.homeSeasonalAction, PlatformIcons.Season, MaterialTheme.colorScheme.primary),
    PlatformHomeShortcut(strings.homeExploreAction, PlatformIcons.Explore, MaterialTheme.colorScheme.tertiary),
    PlatformHomeShortcut(strings.homeReviewsAction, PlatformIcons.Comments, MaterialTheme.colorScheme.secondary),
    PlatformHomeShortcut(strings.homeCalendarAction, PlatformIcons.Calendar, MaterialTheme.colorScheme.error),
)
