package com.luum.michi.app.core

import com.luum.michi.app.core.model.ExternalLink
import com.luum.michi.app.core.util.toHtmlContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HtmlLinksTest {

    @Test
    fun anchorOnlyLineBecomesALinkAndLeavesTheText() {
        val content = "Voice actor.<br><a href=\"https://twitter.com/x\">Twitter</a>".toHtmlContent()

        assertEquals("Voice actor.", content.text)
        assertEquals(listOf(ExternalLink(site = "Twitter", url = "https://twitter.com/x")), content.links)
    }

    @Test
    fun anchorInsideProseKeepsItsText() {
        val content = "Known for <a href=\"https://example.com/n\">Naruto</a>.".toHtmlContent()

        assertEquals("Known for Naruto.", content.text)
        assertEquals("example.com", content.links.single().site)
    }

    @Test
    fun proseWithAnInlineAnchorStaysReadable() {
        val content =
            "trabajó en <a href=\"https://example.com/ghibli\">Studio Ghibli</a> desde 1985".toHtmlContent()

        assertEquals("trabajó en Studio Ghibli desde 1985", content.text)
    }

    @Test
    fun labelPlusLinkLineIsDropped() {
        val content = "Bio line.<br>Website: <a href=\"https://example.com\">example.com</a>".toHtmlContent()

        assertEquals("Bio line.", content.text)
        assertEquals("https://example.com", content.links.single().url)
    }

    @Test
    fun severalLinksAndSeparatorsDisappear() {
        val content = ("Twitter: <a href=\"https://twitter.com/x\">@x</a> | "
            + "Instagram: <a href=\"https://instagram.com/y\">@y</a>").toHtmlContent()

        assertEquals("", content.text)
        assertEquals(listOf("Twitter", "Instagram"), content.links.map { it.site })
        assertEquals(listOf("@x", "@y"), content.links.map { it.handle })
    }

    @Test
    fun bareUrlIsCollectedWithAndWithoutALabel() {
        assertEquals("https://example.com", "Website: https://example.com".toHtmlContent().links.single().url)
        assertEquals("", "Website: https://example.com".toHtmlContent().text)
        assertEquals("https://example.com", "https://example.com".toHtmlContent().links.single().url)
    }

    @Test
    fun handleIsKeptOutOfTheLabel() {
        val link = "<a href=\"https://twitter.com/x\">@x</a>".toHtmlContent().links.single()

        assertEquals("Twitter", link.site)
        assertEquals("@x", link.handle)
    }

    @Test
    fun duplicateUrlsAreDedupedAcrossSchemeAndWww() {
        val content = ("<a href=\"http://www.twitter.com/x\">a</a><br>"
            + "<a href=\"https://twitter.com/x/\">b</a>").toHtmlContent()

        assertEquals(1, content.links.size)
    }

    @Test
    fun nonHttpAndAniListLinksAreIgnored() {
        val content = ("<a href=\"mailto:x@y.com\">Email</a><br>"
            + "<a href=\"https://anilist.co/user/x\">AniList</a>").toHtmlContent()

        assertTrue(content.links.isEmpty())
    }

    @Test
    fun anchorLabelsThatAreNotHostNamesAreDroppedFromTheBody() {
        val content = ("Bio.<br><a href=\"https://example.com/profile\">Profile (Arts Vision)</a> "
            + "<a href=\"https://ja.example.org/wiki\">Wiki (jp)</a> "
            + "<a href=\"https://en.example.org/wiki\">Wiki (en)</a>").toHtmlContent()

        assertEquals("Bio.", content.text)
        assertEquals(3, content.links.size)
    }

    @Test
    fun aLineThatIsOnlyALinkLabelIsDropped() {
        val content = "<a href=\"https://example.com\">Website</a>".toHtmlContent()

        assertEquals("", content.text)
        assertEquals("https://example.com", content.links.single().url)
    }

    @Test
    fun aLabelInsideProseKeepsTheLine() {
        val content = "Visit the <a href=\"https://example.com\">Website</a> for more.".toHtmlContent()

        assertEquals("Visit the Website for more.", content.text)
    }

    @Test
    fun labelIsDerivedFromTheHostWhenTheAnchorTextIsTheUrl() {
        val content = "<a href=\"https://www.instagram.com/x/\">https://www.instagram.com/x/</a>".toHtmlContent()

        assertEquals("Instagram", content.links.single().site)
        assertEquals("", content.text)
    }
}
