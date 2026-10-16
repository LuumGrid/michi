package com.luum.michi.app.mediaDetail

import com.luum.michi.app.mediaDetail.domain.common.stripHtml
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

    @Test
    fun ampersandIsDecodedLastSoDoubleEncodedTextSurvives() {
        // `&amp;lt;` was written as a literal "&lt;", not as "<".
        assertEquals("&lt;", "&amp;lt;".stripHtml())
    }
}
