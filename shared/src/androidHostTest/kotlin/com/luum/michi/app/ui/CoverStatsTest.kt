package com.luum.michi.app.ui

import com.luum.michi.app.core.media.domain.MediaGridItem
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
 * card rails.
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
    }

    @Test
    fun aMissingValuePaintsNothing() {
        assertNull(scoreBadge(null))
        assertNull(popularityBadge(null))
    }

    @Test
    fun toPosterItemKeepsTwoGenresAndPlaceholder() {
        val item = MediaGridItem(
            mediaId = 7,
            title = "Work",
            cover = null,
            paletteHex = "#123456",
            year = 2020,
            genres = listOf("Action", "Comedy", "Drama"),
        )

        val poster = item.toPosterItem()

        assertEquals(7, poster.id)
        assertEquals("Work", poster.title)
        assertEquals("Action · Comedy", poster.subtitle, "only the first two genres")
        assertEquals("#123456", poster.placeholderHex)
    }

    @Test
    fun toPosterItemWithoutGenresPaintsNoSubtitle() {
        val item = MediaGridItem(
            mediaId = 8,
            title = "Bare",
            cover = null,
            paletteHex = null,
            year = null,
        )

        val poster = item.toPosterItem()

        assertNull(poster.subtitle)
        assertNull(poster.placeholderHex)
    }
}
