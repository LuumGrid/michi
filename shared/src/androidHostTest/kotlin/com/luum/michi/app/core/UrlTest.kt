package com.luum.michi.app.core

import com.luum.michi.app.core.util.canonicalHost
import com.luum.michi.app.core.util.isAniListHost
import com.luum.michi.app.core.util.isWebUrl
import com.luum.michi.app.core.util.urlHost
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UrlTest {

    @Test
    fun urlHostStripsEverythingButTheHost() {
        assertEquals("example.com", urlHost("https://example.com/path?a=1#frag"))
        assertEquals("example.com", urlHost("http://user:pass@example.com:8443/x"))
        assertEquals("example.com", urlHost("https://www.example.com/x"))
        assertEquals("example.com", urlHost("https://mobile.example.com"))
        assertEquals("example.com", urlHost("https://m.example.com"))
        // Any other subdomain is a real host, not a prefix to strip.
        assertEquals("usuario.fanbox.cc", urlHost("https://usuario.fanbox.cc/"))
    }

    @Test
    fun urlHostRejectsNonWebUrls() {
        assertNull(urlHost("javascript:alert(1)"))
        assertNull(urlHost("michi://oauth/callback"))
        assertNull(urlHost(null))
        assertNull(urlHost(""))
    }

    @Test
    fun isWebUrlAcceptsOnlyHttpAndHttps() {
        assertTrue(isWebUrl("https://example.com"))
        assertTrue(isWebUrl("HTTP://example.com"))
        assertFalse(isWebUrl("javascript:alert(1)"))
        assertFalse(isWebUrl("michi://oauth/callback"))
        assertFalse(isWebUrl(null))
    }

    @Test
    fun canonicalHostCollapsesAliases() {
        assertEquals("twitter.com", canonicalHost("https://x.com/user"))
        assertEquals("twitter.com", canonicalHost("https://twitter.com/user"))
        assertEquals("youtube.com", canonicalHost("https://youtu.be/abc"))
        assertEquals("youtube.com", canonicalHost("https://youtube.com/watch"))
        assertEquals("fanbox.cc", canonicalHost("https://fanbox.cc"))
        assertNull(canonicalHost("mailto:x@fanbox.cc"))
    }

    @Test
    fun aniListHostRejectsLookalikes() {
        assertTrue(isAniListHost("anilist.co"))
        assertTrue(isAniListHost("sub.anilist.co"))
        assertFalse(isAniListHost("fakeanilist.co"))
        assertFalse(isAniListHost("notanilist.co"))
    }
}
