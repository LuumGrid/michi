package com.luum.michi.app.mediaDetail

import com.luum.michi.app.mediaDetail.domain.character.model.CharacterDetail
import com.luum.michi.app.mediaDetail.domain.character.model.CharacterMediaItem
import com.luum.michi.app.mediaDetail.domain.character.model.CharacterMediaPage
import com.luum.michi.app.mediaDetail.domain.character.model.CharacterMediaSort
import com.luum.michi.app.mediaDetail.domain.character.model.aniListUrl as characterAniListUrl
import com.luum.michi.app.mediaDetail.domain.character.model.shareText as characterShareText
import com.luum.michi.app.mediaDetail.domain.character.model.sortedFor as characterSortedFor
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffDetail
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffCharacterPage
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffMediaItem
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffMediaPage
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffMediaSort
import com.luum.michi.app.mediaDetail.domain.staff.model.aniListUrl as staffAniListUrl
import com.luum.michi.app.mediaDetail.domain.staff.model.shareText as staffShareText
import com.luum.michi.app.mediaDetail.domain.staff.model.sortedFor as staffSortedFor
import com.luum.michi.app.mediaDetail.domain.studio.model.StudioDetail
import com.luum.michi.app.mediaDetail.domain.studio.model.StudioMediaItem
import com.luum.michi.app.mediaDetail.domain.studio.model.StudioMediaPage
import com.luum.michi.app.mediaDetail.domain.studio.model.StudioMediaSort
import com.luum.michi.app.mediaDetail.domain.studio.model.aniListUrl as studioAniListUrl
import com.luum.michi.app.mediaDetail.domain.studio.model.shareText as studioShareText
import com.luum.michi.app.mediaDetail.domain.studio.model.sortedFor as studioSortedFor
import kotlin.test.Test
import kotlin.test.assertEquals

// ────────────── fixtures ──────────────

private fun characterMedia(id: Int, score: Int? = null, year: Int? = null) = CharacterMediaItem(
    mediaId = id,
    title = "Media $id",
    coverUrl = null,
    paletteHex = null,
    role = null,
    format = null,
    year = year,
    averageScore = score,
    viewerStatus = null,
    voiceActorName = null,
    voiceActorImageUrl = null,
    voiceActorLanguage = null,
    voiceActorId = null,
)

private fun staffMedia(id: Int, score: Int? = null, year: Int? = null) = StaffMediaItem(
    mediaId = id,
    title = "Media $id",
    coverUrl = null,
    paletteHex = null,
    staffRole = null,
    format = null,
    year = year,
    averageScore = score,
)

private fun studioMedia(id: Int, score: Int? = null, year: Int? = null) = StudioMediaItem(
    mediaId = id,
    title = "Media $id",
    coverUrl = null,
    paletteHex = null,
    format = null,
    year = year,
    averageScore = score,
    viewerStatus = null,
)

private fun characterDetail(id: Int, name: String) = CharacterDetail(
    id = id,
    name = name,
    nativeName = null,
    alternativeNames = emptyList(),
    alternativeSpoilerNames = emptyList(),
    imageUrl = null,
    descriptionPlain = "",
    gender = null,
    age = null,
    bloodType = null,
    birthday = null,
    favourites = null,
    isFavourite = false,
    media = CharacterMediaPage(emptyList(), hasNextPage = false, currentPage = 1),
)

private fun staffDetail(id: Int, name: String) = StaffDetail(
    id = id,
    name = name,
    nativeName = null,
    alternativeNames = emptyList(),
    imageUrl = null,
    descriptionPlain = "",
    gender = null,
    age = null,
    birthday = null,
    death = null,
    yearsActiveStart = null,
    yearsActiveEnd = null,
    homeTown = null,
    occupations = null,
    bloodType = null,
    favourites = null,
    isFavourite = false,
    media = StaffMediaPage(emptyList(), hasNextPage = false, currentPage = 1),
    characters = StaffCharacterPage(emptyList(), hasNextPage = false, currentPage = 1),
)

private fun studioDetail(id: Int, name: String) = StudioDetail(
    id = id,
    name = name,
    isAnimeStudio = true,
    favourites = null,
    isFavourite = false,
    media = StudioMediaPage(emptyList(), hasNextPage = false, currentPage = 1),
)

class DetailHelpersTest {

    // ── client-side ordering ──

    @Test
    fun characterSortedForScoreYearAndFallback() {
        val items = listOf(
            characterMedia(1, score = 70, year = 2001),
            characterMedia(2, score = 95, year = 1999),
            characterMedia(3, score = null, year = 2020),
        )

        assertEquals(listOf(2, 1, 3), items.characterSortedFor(CharacterMediaSort.SCORE).map { it.mediaId })
        assertEquals(listOf(3, 1, 2), items.characterSortedFor(CharacterMediaSort.NEWEST).map { it.mediaId })
        assertEquals(listOf(2, 1, 3), items.characterSortedFor(CharacterMediaSort.OLDEST).map { it.mediaId })
        // No client ordering: the server popularity order is preserved.
        assertEquals(listOf(1, 2, 3), items.characterSortedFor(CharacterMediaSort.POPULARITY).map { it.mediaId })
        assertEquals(listOf(1, 2, 3), items.characterSortedFor(CharacterMediaSort.FAVOURITES).map { it.mediaId })
    }

    @Test
    fun staffSortedForScoreYearAndTitle() {
        val items = listOf(
            staffMedia(1, score = 70, year = 2001).copy(title = "Beta"),
            staffMedia(2, score = 95, year = 1999).copy(title = "alpha"),
            staffMedia(3, score = null, year = 2020).copy(title = "Gamma"),
        )

        assertEquals(listOf(2, 1, 3), items.staffSortedFor(StaffMediaSort.SCORE).map { it.mediaId })
        assertEquals(listOf(3, 1, 2), items.staffSortedFor(StaffMediaSort.NEWEST).map { it.mediaId })
        assertEquals(listOf(2, 1, 3), items.staffSortedFor(StaffMediaSort.OLDEST).map { it.mediaId })
        // Title is case-insensitive.
        assertEquals(listOf(2, 1, 3), items.staffSortedFor(StaffMediaSort.TITLE).map { it.mediaId })
        assertEquals(listOf(1, 2, 3), items.staffSortedFor(StaffMediaSort.POPULARITY).map { it.mediaId })
    }

    @Test
    fun studioSortedForScoreYearAndFallback() {
        val items = listOf(
            studioMedia(1, score = 70, year = 2001),
            studioMedia(2, score = 95, year = 1999),
            studioMedia(3, score = null, year = 2020),
        )

        assertEquals(listOf(2, 1, 3), items.studioSortedFor(StudioMediaSort.SCORE).map { it.mediaId })
        assertEquals(listOf(3, 1, 2), items.studioSortedFor(StudioMediaSort.NEWEST).map { it.mediaId })
        assertEquals(listOf(2, 1, 3), items.studioSortedFor(StudioMediaSort.OLDEST).map { it.mediaId })
        assertEquals(listOf(1, 2, 3), items.studioSortedFor(StudioMediaSort.POPULARITY).map { it.mediaId })
    }

    // ── canonical URLs + share body per scope ──

    @Test
    fun characterUrlAndShareText() {
        val detail = characterDetail(id = 5, name = "Luffy")
        assertEquals("https://anilist.co/character/5", detail.characterAniListUrl())
        assertEquals("Luffy\nhttps://anilist.co/character/5", detail.characterShareText())
    }

    @Test
    fun staffUrlAndShareText() {
        val detail = staffDetail(id = 6, name = "Oda")
        assertEquals("https://anilist.co/staff/6", detail.staffAniListUrl())
        assertEquals("Oda\nhttps://anilist.co/staff/6", detail.staffShareText())
    }

    @Test
    fun studioUrlAndShareText() {
        val detail = studioDetail(id = 7, name = "Bones")
        assertEquals("https://anilist.co/studio/7", detail.studioAniListUrl())
        assertEquals("Bones\nhttps://anilist.co/studio/7", detail.studioShareText())
    }
}
