package com.luum.michi.app.core

import com.luum.michi.app.core.language.domain.EnglishStrings
import com.luum.michi.app.core.language.domain.SpanishStrings
import com.luum.michi.app.core.media.repository.dto.FuzzyDateDto
import com.luum.michi.app.core.media.repository.dto.format
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FuzzyDateFormatTest {

    @Test
    fun fullDateUsesLanguageOrder() {
        val date = FuzzyDateDto(year = 2026, month = 10, day = 5)
        assertEquals("Oct 5, 2026", date.format(EnglishStrings))
        assertEquals("5 oct 2026", date.format(SpanishStrings))
    }

    @Test
    fun partialDatesKeepTheirShape() {
        assertEquals("Oct 5", FuzzyDateDto(month = 10, day = 5).format(EnglishStrings))
        assertEquals("5 oct", FuzzyDateDto(month = 10, day = 5).format(SpanishStrings))

        assertEquals("Oct 2026", FuzzyDateDto(year = 2026, month = 10).format(EnglishStrings))
        assertEquals("oct 2026", FuzzyDateDto(year = 2026, month = 10).format(SpanishStrings))

        assertEquals("2026", FuzzyDateDto(year = 2026).format(EnglishStrings))
        assertEquals("Oct", FuzzyDateDto(month = 10).format(EnglishStrings))
    }

    @Test
    fun nullAndEmptyDatesReturnNull() {
        assertNull((null as FuzzyDateDto?).format(EnglishStrings))
        assertNull(FuzzyDateDto().format(EnglishStrings))
    }

    @Test
    fun includeYearFalseKeepsABirthdayYearLess() {
        val date = FuzzyDateDto(year = 1990, month = 10, day = 5)
        assertEquals("Oct 5", date.format(EnglishStrings, includeYear = false))
        assertEquals("5 oct", date.format(SpanishStrings, includeYear = false))
        // Year-only has nothing left to show once the year is dropped.
        assertNull(FuzzyDateDto(year = 1990).format(EnglishStrings, includeYear = false))
    }
}
