package com.luum.michi.app.ui

import com.luum.michi.app.core.media.domain.MediaGridItem
import com.luum.michi.app.ui.components.favouritesBadge
import com.luum.michi.app.ui.components.popularityBadge
import com.luum.michi.app.ui.components.scoreBadge
import com.luum.michi.app.ui.components.toPosterItem
import com.luum.michi.app.ui.icons.AppIcons
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The single badge format of the app: score as `85%`, counts with
 * `compactCount()`, and one glyph per stat. Used by the dashboard rails and the
 * studio grid.
 */
class CoverStatsTest {

    @Test
    fun scoreReadsAsAPercentage() {
        assertEquals("85%", scoreBadge(85)?.value)
        assertEquals(AppIcons.StarOutline, scoreBadge(85)?.icon)
    }

    @Test
    fun countsUseTheCompactForm() {
        assertEquals("1.8K", popularityBadge(1800)?.value)
        assertEquals(AppIcons.Groups, popularityBadge(1800)?.icon)
        assertEquals("12K", favouritesBadge(12_000)?.value)
        assertEquals(AppIcons.Favorite, favouritesBadge(12_000)?.icon)
    }

    @Test
    fun aMissingValuePaintsNothing() {
        assertNull(scoreBadge(null))
        assertNull(popularityBadge(null))
        assertNull(favouritesBadge(null))
    }

    @Test
    fun toPosterItemKeepsTwoGenresStatsAndPlaceholder() {
        val item = MediaGridItem(
            mediaId = 7,
            title = "Work",
            cover = null,
            paletteHex = "#123456",
            format = "TV",
            year = 2020,
            averageScore = 85,
            favourites = 1800,
            genres = listOf("Action", "Comedy", "Drama"),
        )

        val poster = item.toPosterItem()

        assertEquals(7, poster.id)
        assertEquals("Work", poster.title)
        assertEquals("Action · Comedy", poster.subtitle, "only the first two genres")
        assertEquals("#123456", poster.placeholderHex)
        assertEquals(listOf("85%", "1.8K"), poster.stats.map { it.value })
    }

    @Test
    fun toPosterItemWithoutGenresOrStatsPaintsNoSubtitleOrBadges() {
        val item = MediaGridItem(
            mediaId = 8,
            title = "Bare",
            cover = null,
            paletteHex = null,
            format = null,
            year = null,
            averageScore = null,
        )

        val poster = item.toPosterItem()

        assertNull(poster.subtitle)
        assertEquals(emptyList(), poster.stats)
        assertNull(poster.placeholderHex)
    }
}
