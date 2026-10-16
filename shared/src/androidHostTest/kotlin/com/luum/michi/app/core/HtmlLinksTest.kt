package com.luum.michi.app.core

import com.luum.michi.app.core.model.ExternalLink
import com.luum.michi.app.core.util.BiographyStat
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

    @Test
    fun boldLabelWithTheColonInsideBecomesAStat() {
        val content = "<p><strong>Height:</strong> 190 cm (6'3&quot;)</p>".toHtmlContent()

        assertEquals(listOf(BiographyStat("Height", "190 cm (6'3\")")), content.stats)
        assertEquals("", content.text)
    }

    @Test
    fun boldLabelWithTheColonOutsideBecomesAStat() {
        val content = "<strong>Height</strong>: 145-180 cm".toHtmlContent()

        assertEquals(listOf(BiographyStat("Height", "145-180 cm")), content.stats)
        assertEquals("", content.text)
    }

    @Test
    fun bTagWorksLikeStrong() {
        val content = "<b>Grade:</b> Special Grade Sorcerer".toHtmlContent()

        assertEquals(listOf(BiographyStat("Grade", "Special Grade Sorcerer")), content.stats)
    }

    @Test
    fun statsSurviveBlankLinesAndNestedSpansAndKeepTheirOrder() {
        val content = ("<p><strong>Height:</strong> 190 cm<br />\n"
            + "\n"
            + "<span class='tooltip'><span><strong>True Devil Fruit:</strong> Nika</span></span>").toHtmlContent()

        assertEquals(
            listOf("Height" to "190 cm", "True Devil Fruit" to "Nika"),
            content.stats.map { it.label to it.value },
        )
    }

    @Test
    fun aStatAfterAParagraphIsStillReadAndTheProseStays() {
        val content = "<p>Born in Konoha.</p>\n<p><strong>Height:</strong> 170 cm</p>".toHtmlContent()

        assertEquals("Born in Konoha.", content.text)
        assertEquals(listOf(BiographyStat("Height", "170 cm")), content.stats)
    }

    @Test
    fun aColonInPlainProseIsNotAStat() {
        val content = "He said: hello and then left.".toHtmlContent()

        assertTrue(content.stats.isEmpty())
        assertEquals("He said: hello and then left.", content.text)
    }

    @Test
    fun aBoldWordWithoutAColonIsNotAStat() {
        val content = "<strong>Important</strong> note about the character.".toHtmlContent()

        assertTrue(content.stats.isEmpty())
        assertEquals("Important note about the character.", content.text)
    }

    @Test
    fun aBoldWordInsideProseIsNotAStat() {
        val content = "He is <strong>very</strong> strong.".toHtmlContent()

        assertTrue(content.stats.isEmpty())
        assertEquals("He is very strong.", content.text)
    }

    @Test
    fun aBiographyWithoutStatsKeepsItsText() {
        val content = "Voice actor and singer.".toHtmlContent()

        assertTrue(content.stats.isEmpty())
        assertEquals("Voice actor and singer.", content.text)
    }

    @Test
    fun aLinkInsideAStatValueStillGoesToLinks() {
        val content = "<strong>Website:</strong> <a href=\"https://example.com\">example.com</a>".toHtmlContent()

        assertEquals("https://example.com", content.links.single().url)
        assertTrue(content.stats.isEmpty(), "a value that is only a link leaves nothing to show")
    }

    @Test
    fun aStatInsideASpoilerStaysInTheText() {
        val content = "<span class='markdown_spoiler'><span><strong>Status:</strong> Deceased</span></span>".toHtmlContent()

        assertTrue(content.stats.isEmpty(), "a spoiler stat must not be promoted")
        // The line stays as prose; the existing cleaning drops the bare "Status:" rótulo.
        assertEquals("Deceased", content.text)
    }

    @Test
    fun aStatWrappedByASpoilerAcrossLinesStaysInTheText() {
        val content = ("<span class='markdown_spoiler'><span><strong>True Devil Fruit:</strong> Nika<br />\n"
            + "<strong>Blood type:</strong> AB</span></span>").toHtmlContent()

        assertTrue(content.stats.isEmpty())
        assertEquals("True Devil Fruit: Nika\nBlood type: AB", content.text)
    }

    @Test
    fun severalStatsOnOneLineBecomeSeveralEntries() {
        val content = "<b>Height:</b> 170 cm <b>Weight:</b> 60 kg".toHtmlContent()

        assertEquals(
            listOf(BiographyStat("Height", "170 cm"), BiographyStat("Weight", "60 kg")),
            content.stats,
        )
        assertEquals("", content.text)
    }
}
