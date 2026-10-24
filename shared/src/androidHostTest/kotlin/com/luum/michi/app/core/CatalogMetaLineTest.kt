package com.luum.michi.app.core

import com.luum.michi.app.core.media.domain.catalogMetaLine
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The shared catalog meta line: format · season year · status, with season and
 * year read together. Pure, so it is tested without the UI.
 */
class CatalogMetaLineTest {

    @Test
    fun everyFieldJoinsWithDots() {
        assertEquals("TV · Fall 2026 · Airing", catalogMetaLine("TV", "Fall", 2026, "Airing"))
    }

    @Test
    fun yearAloneWhenThereIsNoSeason() {
        assertEquals("Manga · 2021", catalogMetaLine("Manga", null, 2021, null))
    }

    @Test
    fun seasonAloneWhenThereIsNoYear() {
        assertEquals("Fall", catalogMetaLine(null, "Fall", null, null))
    }

    @Test
    fun nothingKnownIsAnEmptyLine() {
        assertEquals("", catalogMetaLine(null, null, null, null))
    }
}
