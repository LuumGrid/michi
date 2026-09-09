package com.luum.michi.app.core

import com.luum.michi.app.core.medialist.domain.MediaListStatus
import com.luum.michi.app.core.medialist.domain.parseMediaListStatus
import com.luum.michi.app.core.model.MediaSeason
import com.luum.michi.app.core.model.parseMediaSeason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CoreParsingTest {

    @Test
    fun listStatusParsesKnownValues() {
        assertEquals(MediaListStatus.CURRENT, parseMediaListStatus("CURRENT"))
        assertEquals(MediaListStatus.PLANNING, parseMediaListStatus("PLANNING"))
        assertEquals(MediaListStatus.COMPLETED, parseMediaListStatus("COMPLETED"))
    }

    @Test
    fun listStatusGarbageParsesToNull() {
        // Unknown wire values degrade to null (treated as not-in-list downstream),
        // never to a wrong bucket.
        assertNull(parseMediaListStatus("BOGUS"))
        assertNull(parseMediaListStatus(null))
    }

    @Test
    fun seasonParsesKnownValues() {
        assertEquals(MediaSeason.WINTER, parseMediaSeason("WINTER"))
        assertEquals(MediaSeason.SPRING, parseMediaSeason("spring"))
    }

    @Test
    fun seasonGarbageParsesToNull() {
        assertNull(parseMediaSeason("BOGUS"))
        assertNull(parseMediaSeason(null))
    }
}
