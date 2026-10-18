package com.luum.michi.app.profile

import com.luum.michi.app.core.language.domain.EnglishStrings
import com.luum.michi.app.core.medialist.domain.ScoreFormat
import com.luum.michi.app.profile.domain.model.ProfileStatDistributionEntry
import com.luum.michi.app.profile.ui.model.formatDistributionLabel
import com.luum.michi.app.profile.ui.model.formatProfileDays
import com.luum.michi.app.profile.ui.model.formatProfileMeanScore
import com.luum.michi.app.profile.ui.model.profileScoreBars
import com.luum.michi.app.profile.ui.model.statusDistributionLabel
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pure formatting of the statistics distributions: labels and the score scale
 * per [ScoreFormat], including the 0-100 bucket grouping AniList returns.
 */
class ProfileStatsFormattingTest {

    private val strings = EnglishStrings

    @Test
    fun meanScoreConvertsFromTheApiZeroToHundredScale() {
        // AniList always reports meanScore on 0-100, whatever the format.
        assertEquals("68", formatProfileMeanScore(67.57, ScoreFormat.POINT_100))
        assertEquals("7", formatProfileMeanScore(73.37, ScoreFormat.POINT_10))
        assertEquals("7.6", formatProfileMeanScore(76.41, ScoreFormat.POINT_10_DECIMAL))
        assertEquals("3.6", formatProfileMeanScore(72.12, ScoreFormat.POINT_5_STARS))
        assertEquals("😐", formatProfileMeanScore(50.0, ScoreFormat.POINT_3_SMILEYS))
    }

    @Test
    fun daysWatchedKeepsOneDecimalIntegersPlain() {
        assertEquals("123.4", formatProfileDays(123.4))
        assertEquals("123", formatProfileDays(123.0))
    }

    @Test
    fun point100BucketsGroupByTens() {
        // AniList ships up to 20 buckets including non-multiples of 10
        // (5/15/…/95); grouping keeps at most ten bars, ascending.
        val distribution = (5..100 step 5).map { ProfileStatDistributionEntry(it.toString(), 1) }
        val bars = profileScoreBars(distribution, ScoreFormat.POINT_100)
        assertEquals(10, bars.size)
        assertEquals("1-10", bars.first().label)
        assertEquals(2, bars.first().count, "5 and 10 both fall in the first decade")
        assertEquals("91-100", bars.last().label)
        assertEquals(2, bars.last().count, "95 and 100 both fall in the last decade")
    }

    @Test
    fun decimalBucketsGroupOnTheTenScale() {
        // POINT_10_DECIMAL ships raw ×10 values (85 = 8.5); the bars relabel to
        // the 0-10 scale, grouped by tens.
        val distribution = listOf(
            ProfileStatDistributionEntry("85", 3),
            ProfileStatDistributionEntry("80", 2),
            ProfileStatDistributionEntry("5", 1),
        )
        val bars = profileScoreBars(distribution, ScoreFormat.POINT_10_DECIMAL)
        assertEquals(listOf("0-1" to 1, "7-8" to 2, "8-9" to 3), bars.map { it.label to it.count })
    }

    @Test
    fun smallScalesKeepOneBarPerValue() {
        val point10 = listOf(
            ProfileStatDistributionEntry("8", 3),
            ProfileStatDistributionEntry("4", 1),
            ProfileStatDistributionEntry("10", 2),
        )
        assertEquals(
            listOf("4", "8", "10"),
            profileScoreBars(point10, ScoreFormat.POINT_10).map { it.label },
        )
        assertEquals(
            listOf("1", "3", "5"),
            profileScoreBars(
                listOf(
                    ProfileStatDistributionEntry("5", 1),
                    ProfileStatDistributionEntry("1", 1),
                    ProfileStatDistributionEntry("3", 1),
                ),
                ScoreFormat.POINT_5_STARS,
            ).map { it.label },
        )
    }

    @Test
    fun smileysReuseTheSharedRepresentation() {
        val distribution = listOf(
            ProfileStatDistributionEntry("3", 1),
            ProfileStatDistributionEntry("1", 1),
            ProfileStatDistributionEntry("2", 1),
        )
        assertEquals(
            listOf("🙁", "😐", "🙂"),
            profileScoreBars(distribution, ScoreFormat.POINT_3_SMILEYS).map { it.label },
        )
    }

    @Test
    fun formatAndStatusLabelsUseTheSharedVocabulary() {
        assertEquals("TV", ProfileStatDistributionEntry("TV", 1).formatDistributionLabel(strings))
        assertEquals("One Shot", ProfileStatDistributionEntry("ONE_SHOT", 1).formatDistributionLabel(strings))
        assertEquals(
            strings.sectionWatching,
            ProfileStatDistributionEntry("CURRENT", 1).statusDistributionLabel(strings, isManga = false),
        )
        assertEquals(
            strings.sectionCurrent,
            ProfileStatDistributionEntry("CURRENT", 1).statusDistributionLabel(strings, isManga = true),
        )
        // An unknown raw is kept verbatim instead of dropping the bar.
        assertEquals(
            "WEIRD",
            ProfileStatDistributionEntry("WEIRD", 1).statusDistributionLabel(strings, isManga = false),
        )
    }
}
