package com.luum.michi.app.mediaDetail

import com.luum.michi.app.core.domain.language.EnglishStrings
import com.luum.michi.app.core.domain.language.SpanishStrings
import com.luum.michi.app.core.repository.anilist.dto.MediaDetailDto
import com.luum.michi.app.mediaDetail.repository.media.toDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MediaDetailSeasonTest {

    @Test
    fun seasonLocalizes() {
        val dto = MediaDetailDto(id = 1, season = "WINTER", seasonYear = 2024)
        assertEquals("Winter 2024", dto.toDomain(EnglishStrings).season)
        assertEquals("Invierno 2024", dto.toDomain(SpanishStrings).season)
    }

    @Test
    fun seasonFallsBackToYearOnly() {
        val dto = MediaDetailDto(id = 1, seasonYear = 2024)
        assertEquals("2024", dto.toDomain(EnglishStrings).season)
    }

    @Test
    fun seasonNullWhenNoData() {
        assertNull(MediaDetailDto(id = 1).toDomain(EnglishStrings).season)
    }
}
