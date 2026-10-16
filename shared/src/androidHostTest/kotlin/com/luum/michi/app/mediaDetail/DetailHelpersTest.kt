package com.luum.michi.app.mediaDetail

import com.luum.michi.app.mediaDetail.domain.character.model.CharacterDetail
import com.luum.michi.app.mediaDetail.domain.character.model.CharacterMediaPage
import com.luum.michi.app.mediaDetail.domain.character.model.aniListUrl as characterAniListUrl
import com.luum.michi.app.mediaDetail.domain.character.model.shareText as characterShareText
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffDetail
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffCharacterPage
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffMediaPage
import com.luum.michi.app.mediaDetail.domain.staff.model.aniListUrl as staffAniListUrl
import com.luum.michi.app.mediaDetail.domain.staff.model.shareText as staffShareText
import com.luum.michi.app.core.model.MediaGridPage
import com.luum.michi.app.mediaDetail.domain.studio.model.StudioDetail
import com.luum.michi.app.mediaDetail.domain.studio.model.aniListUrl as studioAniListUrl
import com.luum.michi.app.mediaDetail.domain.studio.model.shareText as studioShareText
import kotlin.test.Test
import kotlin.test.assertEquals

// ────────────── fixtures ──────────────

private fun characterDetail(id: Int, name: String) = CharacterDetail(
    id = id,
    name = name,
    nativeName = null,
    alternativeNames = emptyList(),
    image = null,
    descriptionPlain = "",
    stats = emptyList(),
    links = emptyList(),
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
    image = null,
    descriptionPlain = "",
    stats = emptyList(),
    links = emptyList(),
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
    media = MediaGridPage(emptyList(), hasNextPage = false, currentPage = 1),
)

class DetailHelpersTest {

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
