package com.luum.michi.app.core

import com.luum.michi.app.core.model.compactCount
import kotlin.test.Test
import kotlin.test.assertEquals

class CompactCountTest {

    @Test
    fun truncatesAndDropsAZeroDecimal() {
        assertEquals("950", 950.compactCount())
        assertEquals("1K", 1_000.compactCount())
        assertEquals("1.8K", 1_800.compactCount())
        assertEquals("12K", 12_345.compactCount())
        assertEquals("1.3M", 1_300_000.compactCount())
    }
}
