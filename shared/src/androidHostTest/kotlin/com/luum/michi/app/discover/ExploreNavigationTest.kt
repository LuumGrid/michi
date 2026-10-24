package com.luum.michi.app.discover

import com.luum.michi.app.discover.domain.model.ExploreCategory
import com.luum.michi.app.discover.domain.model.ExploreResultTarget
import com.luum.michi.app.discover.domain.model.resultTarget
import kotlin.test.Test
import kotlin.test.assertEquals

/** The pure category → detail mapping ExploreScreen dispatches through. */
class ExploreNavigationTest {

    @Test
    fun everyCategoryMapsToItsDetail() {
        assertEquals(ExploreResultTarget.MEDIA, ExploreCategory.ANIME.resultTarget())
        assertEquals(ExploreResultTarget.MEDIA, ExploreCategory.MANGA.resultTarget())
        assertEquals(ExploreResultTarget.CHARACTER, ExploreCategory.CHARACTERS.resultTarget())
        assertEquals(ExploreResultTarget.STAFF, ExploreCategory.STAFF.resultTarget())
        assertEquals(ExploreResultTarget.STUDIO, ExploreCategory.STUDIOS.resultTarget())
    }
}
