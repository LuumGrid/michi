package com.luum.michi.app.core

import com.luum.michi.app.core.medialist.domain.MediaListStatus
import com.luum.michi.app.core.medialist.domain.parseMediaListStatus
import com.luum.michi.app.core.medialist.domain.toApiValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MedialistTest {

    @Test
    fun statusParsesKnownValues() {
        assertEquals(MediaListStatus.CURRENT, parseMediaListStatus("CURRENT"))
        assertEquals(MediaListStatus.PLANNING, parseMediaListStatus("PLANNING"))
        assertEquals(MediaListStatus.COMPLETED, parseMediaListStatus("COMPLETED"))
        assertEquals(MediaListStatus.DROPPED, parseMediaListStatus("DROPPED"))
        assertEquals(MediaListStatus.PAUSED, parseMediaListStatus("PAUSED"))
        assertEquals(MediaListStatus.REPEATING, parseMediaListStatus("REPEATING"))
    }

    @Test
    fun statusGarbageYieldsNull() {
        assertNull(parseMediaListStatus("BOGUS"))
        assertNull(parseMediaListStatus(null))
    }

    @Test
    fun statusRoundtripsThroughApiValue() {
        MediaListStatus.entries.forEach { status ->
            assertEquals(status, parseMediaListStatus(status.toApiValue()))
        }
    }
}
