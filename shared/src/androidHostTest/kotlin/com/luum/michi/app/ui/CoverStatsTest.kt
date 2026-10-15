package com.luum.michi.app.ui

import com.luum.michi.app.ui.components.favouritesBadge
import com.luum.michi.app.ui.components.popularityBadge
import com.luum.michi.app.ui.components.scoreBadge
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
}
