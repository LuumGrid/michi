package com.luum.michi.app.discovery.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.language.LanguageStrings
import com.luum.michi.app.core.platform.PlatformIcons
import com.luum.michi.app.core.platform.components.PlatformHomeCommunityCard
import com.luum.michi.app.core.platform.components.PlatformHomeHeader
import com.luum.michi.app.core.platform.components.PlatformHomeMediaItem
import com.luum.michi.app.core.platform.components.PlatformHomeMediaRail
import com.luum.michi.app.core.platform.components.PlatformHomeReleaseItem
import com.luum.michi.app.core.platform.components.PlatformHomeReleaseRail
import com.luum.michi.app.core.platform.components.PlatformHomeShortcut
import com.luum.michi.app.core.platform.components.PlatformHomeShortcutRow

@Composable
fun DiscoveryScreen() {
    val strings = LanguageProvider.strings
    val shortcutItems = homeShortcutItems(strings)
    val releasingToday = releasingTodayItems()
    val trendingAnimation = trendingAnimationItems()
    val trendingReading = trendingReadingItems()

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
        item {
            PlatformHomeShortcutRow(items = shortcutItems)
        }
        item {
            PlatformHomeReleaseRail(
                title = strings.homeReleasingTodayTitle,
                items = releasingToday,
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
                items = trendingAnimation,
            )
        }
        item {
            PlatformHomeMediaRail(
                title = strings.homeTrendingReadingTitle,
                items = trendingReading,
            )
        }
    }
}

@Composable
private fun homeShortcutItems(strings: LanguageStrings): List<PlatformHomeShortcut> {
    return listOf(
        PlatformHomeShortcut(strings.homeSeasonalAction, PlatformIcons.Season, MaterialTheme.colorScheme.primary),
        PlatformHomeShortcut(strings.homeExploreAction, PlatformIcons.Explore, MaterialTheme.colorScheme.tertiary),
        PlatformHomeShortcut(strings.homeReviewsAction, PlatformIcons.Comments, MaterialTheme.colorScheme.secondary),
        PlatformHomeShortcut(strings.homeCalendarAction, PlatformIcons.Calendar, MaterialTheme.colorScheme.error),
    )
}

private fun releasingTodayItems(): List<PlatformHomeReleaseItem> {
    return listOf(
        PlatformHomeReleaseItem("Sousou no Frieren", "Ep. 8", "06:00", listOf(Color(0xFF2D6CDF), Color(0xFF6DE0CF))),
        PlatformHomeReleaseItem("Solo Leveling", "Ep. 11", "10:30", listOf(Color(0xFF251B37), Color(0xFFE04D75))),
        PlatformHomeReleaseItem("The Apothecary Diaries", "Ep. 19", "18:00", listOf(Color(0xFF167D7F), Color(0xFFF3C969))),
    )
}

private fun trendingAnimationItems(): List<PlatformHomeMediaItem> {
    return listOf(
        PlatformHomeMediaItem("Dandadan", "TV - 24 min", listOf(Color(0xFFFF5C8A), Color(0xFF3A1C71))),
        PlatformHomeMediaItem("Chainsaw Man", "Movie", listOf(Color(0xFFF2994A), Color(0xFF2D3436))),
        PlatformHomeMediaItem("Jujutsu Kaisen", "TV", listOf(Color(0xFF4776E6), Color(0xFF8E54E9))),
        PlatformHomeMediaItem("Kaiju No. 8", "TV", listOf(Color(0xFF11998E), Color(0xFF38EF7D))),
    )
}

private fun trendingReadingItems(): List<PlatformHomeMediaItem> {
    return listOf(
        PlatformHomeMediaItem("Sakamoto Days", "Manga", listOf(Color(0xFF232526), Color(0xFFFFD166))),
        PlatformHomeMediaItem("Omniscient Reader", "Manhwa", listOf(Color(0xFF2B5876), Color(0xFF4E4376))),
        PlatformHomeMediaItem("Dandadan", "Manga", listOf(Color(0xFFFF512F), Color(0xFFDD2476))),
        PlatformHomeMediaItem("Blue Box", "Manga", listOf(Color(0xFF56CCF2), Color(0xFF2F80ED))),
    )
}
