package com.luum.michi.app.core

import com.luum.michi.app.core.language.domain.EnglishStrings
import com.luum.michi.app.core.language.domain.SpanishStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class MonthNamesTest {

    @Test
    fun englishMonthsAreAllPresent() {
        val names = (1..12).map(EnglishStrings::monthShortName)
        assertEquals(12, names.filter { it.isNotBlank() }.size)
        assertEquals("Jan", names[0])
        assertEquals("Dec", names[11])
    }

    @Test
    fun spanishMonthsAreAllPresent() {
        val names = (1..12).map(SpanishStrings::monthShortName)
        assertEquals(12, names.filter { it.isNotBlank() }.size)
        assertEquals("ene", names[0])
    }

    @Test
    fun spanishDiffersFromEnglishProvingTheWiring() {
        assertNotEquals(
            (1..12).map(EnglishStrings::monthShortName),
            (1..12).map(SpanishStrings::monthShortName),
        )
    }
}
