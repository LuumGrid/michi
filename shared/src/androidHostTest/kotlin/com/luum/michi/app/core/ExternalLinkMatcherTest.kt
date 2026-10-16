package com.luum.michi.app.core

import com.luum.michi.app.core.model.ExternalLink
import com.luum.michi.app.core.model.matchCatalogSite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExternalLinkMatcherTest {

    private fun site(site: String, url: String) = ExternalLink(site = site, url = url)

    @Test
    fun exactHostMatches() {
        val catalog = listOf(site("Twitter", "https://twitter.com"))

        assertEquals("Twitter", matchCatalogSite(catalog, "https://twitter.com/user")?.site)
    }

    @Test
    fun subdomainMatchesOnADotBoundary() {
        val catalog = listOf(
            site("Fanbox", "https://fanbox.cc"),
            site("Bandcamp", "https://bandcamp.com"),
            site("Tumblr", "https://tumblr.com"),
            site("Wikipedia", "https://wikipedia.org"),
        )

        assertEquals("Fanbox", matchCatalogSite(catalog, "https://usuario.fanbox.cc")?.site)
        assertEquals("Bandcamp", matchCatalogSite(catalog, "https://user.bandcamp.com/album/x")?.site)
        assertEquals("Tumblr", matchCatalogSite(catalog, "https://user.tumblr.com")?.site)
        assertEquals("Wikipedia", matchCatalogSite(catalog, "https://en.wikipedia.org/wiki/X")?.site)
    }

    @Test
    fun partialLabelDoesNotMatch() {
        val catalog = listOf(site("Fanbox", "https://fanbox.cc"))

        assertNull(matchCatalogSite(catalog, "https://notfanbox.cc"))
    }

    @Test
    fun aliasesStillMatch() {
        assertEquals(
            "Twitter",
            matchCatalogSite(listOf(site("Twitter", "https://twitter.com")), "https://x.com/user")?.site,
        )
        assertEquals(
            "Twitter",
            matchCatalogSite(listOf(site("Twitter", "https://x.com")), "https://twitter.com/user")?.site,
        )
        assertEquals(
            "YouTube",
            matchCatalogSite(listOf(site("YouTube", "https://youtube.com")), "https://youtu.be/abc")?.site,
        )
    }

    @Test
    fun schemeLessCatalogUrlsStillMatch() {
        // AniList's catalog mixes full URLs with bare hosts ("instagram.com"),
        // which used to be dropped because they carry no scheme.
        val catalog = listOf(site("Instagram", "instagram.com"))

        assertEquals("Instagram", matchCatalogSite(catalog, "https://instagram.com/user")?.site)
        assertEquals("Instagram", matchCatalogSite(catalog, "https://www.instagram.com/user")?.site)
    }

    @Test
    fun mostSpecificHostWins() {
        val catalog = listOf(
            site("Example", "https://example.com"),
            site("Sub", "https://sub.example.com"),
        )

        assertEquals("Sub", matchCatalogSite(catalog, "https://a.sub.example.com")?.site)
    }

    @Test
    fun nonHttpAndUnknownHostsDoNotMatch() {
        val catalog = listOf(site("Fanbox", "https://fanbox.cc"))

        assertNull(matchCatalogSite(catalog, "mailto:x@fanbox.cc"))
        assertNull(matchCatalogSite(catalog, "https://unknown.tld/user"))
    }
}
