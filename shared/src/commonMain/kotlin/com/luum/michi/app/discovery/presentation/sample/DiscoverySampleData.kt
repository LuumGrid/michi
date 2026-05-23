package com.luum.michi.app.discovery.presentation.sample

import androidx.compose.ui.graphics.Color
import com.luum.michi.app.core.platform.components.PlatformHomeMediaItem
import com.luum.michi.app.core.platform.components.PlatformHomeReleaseItem

internal val DiscoveryReleasingTodaySample: List<PlatformHomeReleaseItem> = listOf(
    PlatformHomeReleaseItem(
        "Sousou no Frieren",
        "Ep. 8",
        "06:00",
        listOf(Color(0xFF2D6CDF), Color(0xFF6DE0CF)),
    ),
    PlatformHomeReleaseItem(
        "Solo Leveling",
        "Ep. 11",
        "10:30",
        listOf(Color(0xFF251B37), Color(0xFFE04D75)),
    ),
    PlatformHomeReleaseItem(
        "The Apothecary Diaries",
        "Ep. 19",
        "18:00",
        listOf(Color(0xFF167D7F), Color(0xFFF3C969)),
    ),
)

internal val DiscoveryTrendingAnimationSample: List<PlatformHomeMediaItem> = listOf(
    PlatformHomeMediaItem("Dandadan", "TV - 24 min", listOf(Color(0xFFFF5C8A), Color(0xFF3A1C71))),
    PlatformHomeMediaItem("Chainsaw Man", "Movie", listOf(Color(0xFFF2994A), Color(0xFF2D3436))),
    PlatformHomeMediaItem("Jujutsu Kaisen", "TV", listOf(Color(0xFF4776E6), Color(0xFF8E54E9))),
    PlatformHomeMediaItem("Kaiju No. 8", "TV", listOf(Color(0xFF11998E), Color(0xFF38EF7D))),
)

internal val DiscoveryTrendingReadingSample: List<PlatformHomeMediaItem> = listOf(
    PlatformHomeMediaItem("Sakamoto Days", "Manga", listOf(Color(0xFF232526), Color(0xFFFFD166))),
    PlatformHomeMediaItem("Omniscient Reader", "Manhwa", listOf(Color(0xFF2B5876), Color(0xFF4E4376))),
    PlatformHomeMediaItem("Dandadan", "Manga", listOf(Color(0xFFFF512F), Color(0xFFDD2476))),
    PlatformHomeMediaItem("Blue Box", "Manga", listOf(Color(0xFF56CCF2), Color(0xFF2F80ED))),
)
