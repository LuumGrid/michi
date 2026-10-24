package com.luum.michi.app.mediaDetail

import com.luum.michi.app.mediaDetail.domain.common.toHtmlContent
import com.luum.michi.app.mediaDetail.domain.common.toSpoilerText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HtmlSpoilersTest {

    private fun spoiler(text: String): String =
        "<span class='markdown_spoiler'><span>$text</span></span>"

    @Test
    fun spoilerOnOneLineProducesARangeOverTheExactText() {
        val content = "Before ${spoiler("secret")} after".toHtmlContent()

        assertEquals("Before secret after", content.text)
        assertEquals("secret", content.text.substring(content.spoilers.single()))
    }

    @Test
    fun spoilerAcrossLinesProducesOneRange() {
        val content = ("Intro<br>${spoiler("secret one<br>secret two")}<br>Outro").toHtmlContent()

        assertEquals("Intro\nsecret one\nsecret two\nOutro", content.text)
        assertEquals("secret one\nsecret two", content.text.substring(content.spoilers.single()))
    }

    @Test
    fun wrapperOnItsOwnLineDoesNotLeaveABlankLine() {
        val content = ("Intro<br><span class='markdown_spoiler'><span><br>secret<br>"
            + "</span></span><br>Outro").toHtmlContent()

        assertEquals("Intro\nsecret\nOutro", content.text)
        assertEquals("secret", content.text.substring(content.spoilers.single()))
    }

    @Test
    fun twoSpoilersOnOneLineStaySeparate() {
        val content = "${spoiler("one")} and ${spoiler("two")}".toHtmlContent()

        assertEquals("one and two", content.text)
        assertEquals(listOf("one", "two"), content.spoilers.map { content.text.substring(it) })
    }

    @Test
    fun nestedSpoilersCollapseToTheOuterRange() {
        val content = ("<span class='markdown_spoiler'><span>outer "
            + "<span class='markdown_spoiler'><span>inner</span></span>"
            + " tail</span></span>").toHtmlContent()

        assertEquals(1, content.spoilers.size)
        assertEquals("outer inner tail", content.text.substring(content.spoilers.single()))
    }

    @Test
    fun unclosedSpoilerRunsToTheEnd() {
        val content = "Before <span class='markdown_spoiler'><span>secret<br>more".toHtmlContent()

        assertEquals("Before secret\nmore", content.text)
        assertEquals("secret\nmore", content.text.substring(content.spoilers.single()))
    }

    @Test
    fun aStatInsideASpoilerStaysProseAndKeepsItsRange() {
        val content = spoiler("<strong>Status:</strong> Deceased").toHtmlContent()

        assertTrue(content.stats.isEmpty(), "a spoiler stat must not be promoted")
        assertEquals("Status: Deceased", content.text)
        assertEquals("Status: Deceased", content.text.substring(content.spoilers.single()))
    }

    @Test
    fun anExternalLinkInsideASpoilerIsStillExtracted() {
        val content = ("Bio<span class='markdown_spoiler'><span> "
            + "<a href=\"https://twitter.com/x\">@x</a>secret</span></span>").toHtmlContent()

        assertEquals("Bio secret", content.text)
        assertEquals("secret", content.text.substring(content.spoilers.single()))
        assertEquals("https://twitter.com/x", content.links.single().url)
    }

    @Test
    fun synopsisWithASpoilerUsesTheSameRule() {
        val content = "A ${spoiler("twist")} ends.".toSpoilerText()

        assertEquals("A twist ends.", content.text)
        assertEquals("twist", content.text.substring(content.spoilers.single()))
    }
}
