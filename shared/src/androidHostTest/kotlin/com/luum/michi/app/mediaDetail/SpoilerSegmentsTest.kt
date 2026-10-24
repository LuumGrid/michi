package com.luum.michi.app.mediaDetail

import com.luum.michi.app.mediaDetail.domain.common.InlineSpoiler
import com.luum.michi.app.mediaDetail.domain.common.SpoilerSegment
import com.luum.michi.app.mediaDetail.domain.common.SpoilerText
import com.luum.michi.app.mediaDetail.domain.common.toSegments
import kotlin.test.Test
import kotlin.test.assertEquals

class SpoilerSegmentsTest {

    @Test
    fun aWholeParagraphBecomesABlock() {
        val segments = SpoilerText(
            text = "Intro\nsecret\nOutro",
            spoilers = listOf(6..11),
        ).toSegments()

        assertEquals(
            listOf(
                SpoilerSegment.Prose("Intro", emptyList()),
                SpoilerSegment.Block("secret", spoilerIndex = 0),
                SpoilerSegment.Prose("Outro", emptyList()),
            ),
            segments,
        )
    }

    @Test
    fun aSpoilerInsideASentenceStaysInline() {
        val segments = SpoilerText(
            text = "Before secret after",
            spoilers = listOf(7..12),
        ).toSegments()

        assertEquals(
            listOf(
                SpoilerSegment.Prose(
                    text = "Before secret after",
                    inlineSpoilers = listOf(InlineSpoiler(spoilerIndex = 0, range = 7..12)),
                ),
            ),
            segments,
        )
    }

    @Test
    fun aBlockAtTheStartAndAnotherAtTheEndFlankTheProse() {
        val segments = SpoilerText(
            text = "secret\nmiddle\nfinale",
            spoilers = listOf(0..5, 14..19),
        ).toSegments()

        assertEquals(
            listOf(
                SpoilerSegment.Block("secret", spoilerIndex = 0),
                SpoilerSegment.Prose("middle", emptyList()),
                SpoilerSegment.Block("finale", spoilerIndex = 1),
            ),
            segments,
        )
    }

    @Test
    fun twoConsecutiveBlocksDoNotProduceAnEmptyProse() {
        val segments = SpoilerText(
            text = "one\ntwo",
            spoilers = listOf(0..2, 4..6),
        ).toSegments()

        assertEquals(
            listOf(
                SpoilerSegment.Block("one", spoilerIndex = 0),
                SpoilerSegment.Block("two", spoilerIndex = 1),
            ),
            segments,
        )
    }

    @Test
    fun mixedInlineAndBlockKeepTheOriginalIndicesAndRebaseTheInlineRanges() {
        val segments = SpoilerText(
            text = "A one B\nblock\nC two D",
            spoilers = listOf(2..4, 8..12, 16..18),
        ).toSegments()

        assertEquals(
            listOf(
                SpoilerSegment.Prose(
                    text = "A one B",
                    inlineSpoilers = listOf(InlineSpoiler(spoilerIndex = 0, range = 2..4)),
                ),
                SpoilerSegment.Block("block", spoilerIndex = 1),
                SpoilerSegment.Prose(
                    text = "C two D",
                    inlineSpoilers = listOf(InlineSpoiler(spoilerIndex = 2, range = 2..4)),
                ),
            ),
            segments,
        )
    }

    @Test
    fun textWithoutSpoilersIsOneProse() {
        val segments = SpoilerText(text = "Plain text", spoilers = emptyList()).toSegments()

        assertEquals(listOf(SpoilerSegment.Prose("Plain text", emptyList())), segments)
    }
}
