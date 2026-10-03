package com.luum.michi.app.core

import com.luum.michi.app.core.navigation.domain.parseAniListMediaId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AniListDeepLinkTest {

    @Test
    fun animeLink() {
        assertEquals(123, parseAniListMediaId("https://anilist.co/anime/123"))
    }

    @Test
    fun mangaLinkWithSlugAndTrailingSlash() {
        assertEquals(456, parseAniListMediaId("https://anilist.co/manga/456/Some-Title/"))
    }

    @Test
    fun wwwHostAndQuery() {
        assertEquals(7, parseAniListMediaId("https://www.anilist.co/anime/7?foo=bar#x"))
    }

    @Test
    fun userLinksAreIgnored() {
        assertNull(parseAniListMediaId("https://anilist.co/user/someone"))
        assertNull(parseAniListMediaId("https://anilist.co/profile/someone"))
    }

    @Test
    fun foreignHostIsIgnored() {
        assertNull(parseAniListMediaId("https://example.com/anime/1"))
    }

    @Test
    fun oauthSchemeIsIgnored() {
        assertNull(parseAniListMediaId("michi://oauth/callback?code=x"))
    }

    @Test
    fun extractsLinkFromSharedText() {
        assertEquals(123, parseAniListMediaId("Mira esto https://anilist.co/anime/123 está bueno"))
        assertEquals(9, parseAniListMediaId("anilist.co/manga/9"))
    }

    @Test
    fun plainTextWithoutLinkIsIgnored() {
        assertNull(parseAniListMediaId("hello world"))
        assertNull(parseAniListMediaId("https://example.com/anime/1"))
    }

    @Test
    fun malformedIsIgnored() {
        assertNull(parseAniListMediaId(null))
        assertNull(parseAniListMediaId(""))
        assertNull(parseAniListMediaId("not a url"))
        assertNull(parseAniListMediaId("https://anilist.co/anime/abc"))
        assertNull(parseAniListMediaId("https://anilist.co/anime/"))
    }
}
