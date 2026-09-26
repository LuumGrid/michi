package com.luum.michi.app.settings

import com.luum.michi.app.settings.domain.model.ScoreFormat
import com.luum.michi.app.settings.domain.model.formatScoreValue
import com.luum.michi.app.settings.domain.model.parseScoreValue
import com.luum.michi.app.settings.domain.model.scoreMaxFor
import com.luum.michi.app.settings.domain.model.scoreStepFor
import com.luum.michi.app.settings.domain.model.scoreSuffixFor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ScoreFormatDisplayTest {

    @Test
    fun point100IsVerbatim() {
        assertEquals(100f, scoreMaxFor(ScoreFormat.POINT_100))
        assertEquals(1f, scoreStepFor(ScoreFormat.POINT_100))
        assertEquals("85", formatScoreValue(ScoreFormat.POINT_100, 85f))
        assertEquals(85f, parseScoreValue(ScoreFormat.POINT_100, "85"))
        assertNull(parseScoreValue(ScoreFormat.POINT_100, "101"))
        assertEquals("/100", scoreSuffixFor(ScoreFormat.POINT_100))
    }

    @Test
    fun point10DecimalKeepsOneDecimal() {
        assertEquals(10f, scoreMaxFor(ScoreFormat.POINT_10_DECIMAL))
        assertEquals(0.5f, scoreStepFor(ScoreFormat.POINT_10_DECIMAL))
        assertEquals("8", formatScoreValue(ScoreFormat.POINT_10_DECIMAL, 8f))
        assertEquals("8.5", formatScoreValue(ScoreFormat.POINT_10_DECIMAL, 8.5f))
        assertEquals(8.5f, parseScoreValue(ScoreFormat.POINT_10_DECIMAL, "8.5"))
        assertNull(parseScoreValue(ScoreFormat.POINT_10_DECIMAL, "10.5"))
        assertEquals("/10", scoreSuffixFor(ScoreFormat.POINT_10_DECIMAL))
    }

    @Test
    fun point10Rounds() {
        assertEquals("8", formatScoreValue(ScoreFormat.POINT_10, 8.4f))
        assertEquals(8f, parseScoreValue(ScoreFormat.POINT_10, "8"))
        assertNull(parseScoreValue(ScoreFormat.POINT_10, "11"))
    }

    @Test
    fun point5StarsScale() {
        assertEquals(5f, scoreMaxFor(ScoreFormat.POINT_5_STARS))
        assertEquals("4", formatScoreValue(ScoreFormat.POINT_5_STARS, 4f))
        assertEquals(4f, parseScoreValue(ScoreFormat.POINT_5_STARS, "4"))
        assertNull(parseScoreValue(ScoreFormat.POINT_5_STARS, "6"))
        assertEquals("/5", scoreSuffixFor(ScoreFormat.POINT_5_STARS))
    }

    @Test
    fun point3SmileysHaveNoTypedForm() {
        assertEquals(3f, scoreMaxFor(ScoreFormat.POINT_3_SMILEYS))
        assertEquals("—", formatScoreValue(ScoreFormat.POINT_3_SMILEYS, 0f))
        assertEquals("🙁", formatScoreValue(ScoreFormat.POINT_3_SMILEYS, 1f))
        assertEquals("😐", formatScoreValue(ScoreFormat.POINT_3_SMILEYS, 2f))
        assertEquals("🙂", formatScoreValue(ScoreFormat.POINT_3_SMILEYS, 3f))
        assertNull(parseScoreValue(ScoreFormat.POINT_3_SMILEYS, "2"))
        assertNull(scoreSuffixFor(ScoreFormat.POINT_3_SMILEYS))
    }
}
