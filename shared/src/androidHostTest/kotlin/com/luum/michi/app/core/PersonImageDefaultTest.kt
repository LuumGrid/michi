package com.luum.michi.app.core

import com.luum.michi.app.core.model.CoverSizes
import com.luum.michi.app.core.person.repository.dto.PersonImageDto
import com.luum.michi.app.core.person.repository.personCoverSizes
import com.luum.michi.app.core.person.repository.toCoverSizes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// AniList's shared "no image" art, taken from a live payload (character 926 and
// a staff page). Both variants end in /default.jpg.
private const val DefaultCharacterLarge = "https://s4.anilist.co/file/anilistcdn/character/large/default.jpg"
private const val DefaultCharacterMedium = "https://s4.anilist.co/file/anilistcdn/character/medium/default.jpg"
private const val DefaultStaffLarge = "https://s4.anilist.co/file/anilistcdn/staff/large/default.jpg"
private const val RealLarge = "https://s4.anilist.co/file/anilistcdn/character/large/b127691-9zqh1xpIubn7.png"
private const val RealMedium = "https://s4.anilist.co/file/anilistcdn/character/medium/b127691-9zqh1xpIubn7.png"

/**
 * A person without an image comes back as AniList's shared placeholder; mapping
 * it to null lets the UI paint its own grey placeholder instead of the blue
 * "NO IMAGE" art.
 */
class PersonImageDefaultTest {

    @Test
    fun theSharedPlaceholderMapsToNull() {
        assertNull(personCoverSizes(DefaultCharacterLarge, DefaultCharacterMedium))
        assertNull(personCoverSizes(DefaultStaffLarge, DefaultStaffLarge))
        assertNull(personCoverSizes(null, null))
    }

    @Test
    fun realImagesAreKept() {
        assertEquals(CoverSizes(large = RealLarge, medium = RealMedium), personCoverSizes(RealLarge, RealMedium))
    }

    @Test
    fun aRealVariantSurvivesADefaultTwin() {
        assertEquals(CoverSizes(large = RealLarge), personCoverSizes(RealLarge, DefaultCharacterMedium))
        assertEquals(CoverSizes(medium = RealMedium), personCoverSizes(DefaultCharacterLarge, RealMedium))
    }

    @Test
    fun theDtoMapperUsesTheSameRule() {
        assertNull(PersonImageDto(large = DefaultCharacterLarge, medium = DefaultCharacterMedium).toCoverSizes())
        assertEquals(
            CoverSizes(large = RealLarge, medium = RealMedium),
            PersonImageDto(large = RealLarge, medium = RealMedium).toCoverSizes(),
        )
    }
}
