package com.luum.michi.app.core

import com.luum.michi.app.core.util.stripHtml
import kotlin.test.Test
import kotlin.test.assertEquals

class HtmlTextTest {

    @Test
    fun astralNumericEntityDecodesToSurrogatePair() {
        assertEquals("\uD83D\uDE00", "&#128512;".stripHtml())
    }

    @Test
    fun bmpNumericEntityDecodes() {
        assertEquals("é", "&#233;".stripHtml())
    }

    @Test
    fun outOfRangeEntityIsPreservedLiterally() {
        assertEquals("&#1114112;", "&#1114112;".stripHtml())
    }

    @Test
    fun mixedContentStripsTagsAndDecodesEntities() {
        assertEquals("A \uD83D\uDE00 & B", "<b>A</b> &#128512; &amp; B".stripHtml())
    }
}
