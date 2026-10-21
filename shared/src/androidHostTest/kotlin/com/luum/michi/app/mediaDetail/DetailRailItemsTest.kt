package com.luum.michi.app.mediaDetail

import com.luum.michi.app.core.language.domain.EnglishStrings
import com.luum.michi.app.core.language.domain.SpanishStrings
import com.luum.michi.app.core.media.domain.MediaSeason
import com.luum.michi.app.core.media.domain.MediaWorkStatus
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetailRelation
import com.luum.michi.app.mediaDetail.domain.media.model.MediaRecommendationEntry
import com.luum.michi.app.mediaDetail.domain.media.model.MediaRelationKind
import com.luum.michi.app.mediaDetail.ui.components.characterRoleLabel
import com.luum.michi.app.mediaDetail.ui.components.toRailItem
import com.luum.michi.app.mediaDetail.ui.components.voiceLanguageLabel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Pure mapping of detail entries to rail/list cards: the shared role label and
 * the `format · season year · status` catalog meta (whose status wording must
 * follow the parent's anime/manga type, like the lists).
 */
class DetailRailItemsTest {

    @Test
    fun characterRoleLabelMapsKnownRolesAndDropsUnknown() {
        assertEquals("Main", characterRoleLabel("MAIN", EnglishStrings))
        assertEquals("Supporting", characterRoleLabel("SUPPORTING", EnglishStrings))
        assertEquals("Background", characterRoleLabel("BACKGROUND", EnglishStrings))
        assertNull(characterRoleLabel(null, EnglishStrings))
        assertNull(characterRoleLabel("OTHER", EnglishStrings))
    }

    @Test
    fun relatedRailItemCarriesKindAndCatalogMeta() {
        val item = relation().toRailItem(EnglishStrings, isManga = false)

        assertEquals(70, item.id)
        assertEquals("Sequel", item.subtitle)
        assertEquals("TV · Summer 2026 · Airing", item.meta)
    }

    @Test
    fun relatedMetaUsesTheMangaStatusWording() {
        val item = relation().toRailItem(EnglishStrings, isManga = true)

        assertEquals("TV · Summer 2026 · Publishing", item.meta)
    }

    @Test
    fun recommendationRailItemHasNoSubtitleAndCarriesCatalogMeta() {
        val item = recommendation().toRailItem(EnglishStrings, isManga = true)

        assertNull(item.subtitle)
        assertEquals("MANGA · Fall 2025 · Finished", item.meta)
    }

    @Test
    fun voiceLanguageLabelLocalizesTheAniListLanguage() {
        assertEquals("Japonés", voiceLanguageLabel("Japanese", SpanishStrings))
        assertEquals("Japanese", voiceLanguageLabel("JAPANESE", EnglishStrings))
        assertEquals("Klingon", voiceLanguageLabel("Klingon", SpanishStrings))
        assertNull(voiceLanguageLabel(null, SpanishStrings))
        assertNull(voiceLanguageLabel("  ", SpanishStrings))
    }

    private fun relation() = MediaDetailRelation(
        mediaId = 70,
        title = "Related",
        cover = null,
        kind = MediaRelationKind.SEQUEL,
        format = "TV",
        season = MediaSeason.SUMMER,
        year = 2026,
        workStatus = MediaWorkStatus.RELEASING,
    )

    private fun recommendation() = MediaRecommendationEntry(
        id = 9,
        title = "Recommended",
        cover = null,
        format = "MANGA",
        season = MediaSeason.FALL,
        year = 2025,
        workStatus = MediaWorkStatus.FINISHED,
        episodesCount = null,
        chaptersCount = 40,
        volumesCount = 5,
        averageScore = null,
        favouritesCount = null,
    )
}
