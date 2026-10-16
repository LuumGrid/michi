package com.luum.michi.app.discover

import com.luum.michi.app.discover.domain.genre.model.urlEncodeQueryValue
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The genre links carry the genre name as a query value, so a space must not
 * break the URL and a non-ASCII name must survive as UTF-8 percent-escapes.
 */
class UrlEncodingTest {

    @Test
    fun aSpaceBecomesPercent20NotPlus() {
        assertEquals("Slice%20of%20Life", "Slice of Life".urlEncodeQueryValue())
    }

    @Test
    fun anAmpersandDoesNotStartANewParameter() {
        assertEquals("Action%20%26%20Adventure", "Action & Adventure".urlEncodeQueryValue())
    }

    @Test
    fun aNonAsciiNameIsPercentEncodedAsUtf8() {
        // "Café": the é is two UTF-8 bytes (0xC3 0xA9).
        assertEquals("Caf%C3%A9", "Café".urlEncodeQueryValue())
    }
}
