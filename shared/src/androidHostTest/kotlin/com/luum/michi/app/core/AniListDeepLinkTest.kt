package com.luum.michi.app.core

import com.luum.michi.app.core.navigation.domain.DetailDestination
import com.luum.michi.app.core.navigation.domain.parseAniListDestination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AniListDeepLinkTest {

    @Test
    fun animeLink() {
        assertEquals(DetailDestination.Media(123), parseAniListDestination("https://anilist.co/anime/123"))
    }

    @Test
    fun mangaLinkWithSlugAndTrailingSlash() {
        assertEquals(
            DetailDestination.Media(456),
            parseAniListDestination("https://anilist.co/manga/456/Some-Title/"),
        )
    }

    @Test
    fun characterLinkWithSlugAndQuery() {
        assertEquals(
            DetailDestination.Character(321),
            parseAniListDestination("https://anilist.co/character/321/Some-Name?foo=bar#x"),
        )
    }

    @Test
    fun staffLink() {
        assertEquals(DetailDestination.Staff(12), parseAniListDestination("https://anilist.co/staff/12"))
    }

    @Test
    fun studioLink() {
        assertEquals(DetailDestination.Studio(34), parseAniListDestination("https://anilist.co/studio/34"))
    }

    @Test
    fun wwwHostAndQuery() {
        assertEquals(
            DetailDestination.Media(7),
            parseAniListDestination("https://www.anilist.co/anime/7?foo=bar#x"),
        )
        assertEquals(
            DetailDestination.Character(8),
            parseAniListDestination("www.anilist.co/character/8/Name"),
        )
    }

    @Test
    fun userAndProfileLinksAreIgnored() {
        assertNull(parseAniListDestination("https://anilist.co/user/someone"))
        assertNull(parseAniListDestination("https://anilist.co/profile/someone"))
    }

    @Test
    fun activityAndForumLinksAreIgnored() {
        assertNull(parseAniListDestination("https://anilist.co/activity/123456"))
        assertNull(parseAniListDestination("https://anilist.co/forum/thread/1"))
    }

    @Test
    fun foreignHostIsIgnored() {
        assertNull(parseAniListDestination("https://example.com/anime/1"))
        assertNull(parseAniListDestination("https://notanilist.co/anime/1"))
        assertNull(parseAniListDestination("https://evil.anilist.co/anime/1"))
    }

    @Test
    fun oauthSchemeIsIgnored() {
        assertNull(parseAniListDestination("michi://oauth/callback?code=x"))
    }

    @Test
    fun extractsTheFirstValidLinkFromSharedText() {
        assertEquals(
            DetailDestination.Media(123),
            parseAniListDestination("Mira esto https://anilist.co/anime/123 está bueno"),
        )
        assertEquals(
            DetailDestination.Media(9),
            parseAniListDestination("anilist.co/manga/9"),
        )
        assertEquals(
            DetailDestination.Studio(3),
            parseAniListDestination("anilist.co/studio/3 y luego anilist.co/anime/9"),
        )
    }

    @Test
    fun plainTextWithoutLinkIsIgnored() {
        assertNull(parseAniListDestination("hello world"))
        assertNull(parseAniListDestination("https://example.com/anime/1"))
    }

    @Test
    fun malformedIsIgnored() {
        assertNull(parseAniListDestination(null))
        assertNull(parseAniListDestination(""))
        assertNull(parseAniListDestination("not a url"))
        assertNull(parseAniListDestination("https://anilist.co/anime/abc"))
        assertNull(parseAniListDestination("https://anilist.co/anime/123abc"))
        assertNull(parseAniListDestination("https://anilist.co/anime/"))
        assertNull(parseAniListDestination("https://anilist.co/anime/0"))
    }
}
