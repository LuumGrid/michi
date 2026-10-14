package com.luum.michi.app.core

import com.luum.michi.app.core.model.PersonLink
import com.luum.michi.app.core.util.toHtmlContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HtmlLinksTest {

    @Test
    fun anchorOnlyLineBecomesALinkAndLeavesTheText() {
        val content = "Voice actor.<br><a href=\"https://twitter.com/x\">Twitter</a>".toHtmlContent()

        assertEquals("Voice actor.", content.text)
        assertEquals(listOf(PersonLink(label = "Twitter", url = "https://twitter.com/x")), content.links)
    }

    @Test
    fun anchorInsideProseKeepsItsText() {
        val content = "Known for <a href=\"https://example.com/n\">Naruto</a>.".toHtmlContent()

        assertEquals("Known for Naruto.", content.text)
        assertEquals("Naruto", content.links.single().label)
    }

    @Test
    fun labelPlusLinkLineIsDropped() {
        val content = "Bio line.<br>Website: <a href=\"https://example.com\">example.com</a>".toHtmlContent()

        assertEquals("Bio line.", content.text)
        assertEquals("https://example.com", content.links.single().url)
    }

    @Test
    fun duplicateUrlsAreDeduped() {
        val content = ("<a href=\"https://twitter.com/x\">Twitter</a><br>"
            + "<a href=\"https://twitter.com/x\">@x</a>").toHtmlContent()

        assertEquals(1, content.links.size)
    }

    @Test
    fun nonHttpAndAniListLinksAreIgnored() {
        val content = ("<a href=\"mailto:x@y.com\">Email</a><br>"
            + "<a href=\"https://anilist.co/user/x\">AniList</a>").toHtmlContent()

        assertTrue(content.links.isEmpty())
    }

    @Test
    fun labelIsDerivedFromTheHostWhenTheAnchorTextIsTheUrl() {
        val content = "<a href=\"https://www.instagram.com/x/\">https://www.instagram.com/x/</a>".toHtmlContent()

        assertEquals("Instagram", content.links.single().label)
    }
}
