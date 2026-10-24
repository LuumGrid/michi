package com.luum.michi.app.core

import com.luum.michi.app.core.util.isAniListDefaultImage
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The one rule both media covers and people images use to drop AniList's art. */
class AniListDefaultImageTest {

    @Test
    fun theSharedArtEndsInDefaultJpg() {
        assertTrue(".../media/anime/cover/medium/default.jpg".isAniListDefaultImage())
        assertTrue(".../character/large/default.jpg".isAniListDefaultImage())
    }

    @Test
    fun aRealCoverIsNotTheSharedArt() {
        assertFalse(".../bx16498-73IhOXpJZiMF.jpg".isAniListDefaultImage())
    }
}
