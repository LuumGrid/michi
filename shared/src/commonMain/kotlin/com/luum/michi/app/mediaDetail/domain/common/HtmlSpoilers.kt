package com.luum.michi.app.mediaDetail.domain.common

/**
 * Clean text plus the ranges AniList wrapped in `markdown_spoiler`. Ranges are
 * inclusive and point into [text] after every tag has been stripped.
 */
internal data class SpoilerText(
    val text: String,
    val spoilers: List<IntRange>,
)

/** One piece of the rendered text: plain prose or a whole spoiler paragraph. */
internal sealed interface SpoilerSegment {
    data class Prose(
        val text: String,
        val inlineSpoilers: List<InlineSpoiler>,
    ) : SpoilerSegment

    data class Block(
        val text: String,
        val spoilerIndex: Int,
    ) : SpoilerSegment
}

/**
 * An inline spoiler inside a [SpoilerSegment.Prose]: [range] is rebased to the
 * prose text, while [spoilerIndex] still points at the original `spoilers`
 * entry so the positional reveal state stays valid.
 */
internal data class InlineSpoiler(
    val spoilerIndex: Int,
    val range: IntRange,
)

/**
 * Private-use markers: they survive tag stripping/entity decoding untouched, so
 * the spoiler boundaries can be reconstructed once the text is clean.
 */
internal const val SpoilerStartMarker = '\uE000'
internal const val SpoilerEndMarker = '\uE001'

// AniList's spoiler wrapper is an outer `markdown_spoiler` span plus an inner
// span, so its close is a `</span></span>` pair. A pair like that also appears
// in ordinary nested spans (tooltips), which is why [markSpoilers] only treats
// one as a spoiler close while a spoiler is actually open.
private val SpoilerTokenRegex = Regex(
    """<span\b[^>]*markdown_spoiler[^>]*>|</span>\s*</span>""",
    RegexOption.IGNORE_CASE,
)
private val BlankLinesRegex = Regex("\n{3,}")

/** Replaces each spoiler wrapper with a marker before any cleaning runs. */
internal fun String.markSpoilers(): String {
    if (!contains("markdown_spoiler", ignoreCase = true)) return this
    var depth = 0
    return SpoilerTokenRegex.replace(this) { match ->
        if (match.value.startsWith("</", ignoreCase = true)) {
            if (depth > 0) {
                depth--
                SpoilerEndMarker.toString()
            } else {
                match.value
            }
        } else {
            depth++
            SpoilerStartMarker.toString()
        }
    }
}

/** Markers this line opens minus the ones it closes (used while splitting lines). */
internal fun String.spoilerMarkerBalance(): Int =
    count { it == SpoilerStartMarker } - count { it == SpoilerEndMarker }

/**
 * Synthetic text from HTML that only needs spoilers: no link/stat extraction.
 * Shared by the media synopsis and by the final step of [toHtmlContent], so both
 * apply the exact same marker/range rule.
 */
internal fun String.toSpoilerText(): SpoilerText =
    markSpoilers().stripHtml().foldMarkerOnlyLines().finalizeSpoilers()

/**
 * Wrappers often sit on their own line. Keeping their marker-only line would
 * leave a phantom blank line after the markers are removed, so each one is
 * folded into the adjacent content line: an opening joins the next line and a
 * closing joins the previous one.
 */
internal fun String.foldMarkerOnlyLines(): String {
    if (none { it.isSpoilerMarker() }) return this
    val kept = mutableListOf<String>()
    val pendingStarts = StringBuilder()
    for (line in split('\n')) {
        val content = line.filterNot { it.isSpoilerMarker() }
        if (content.isNotBlank()) {
            kept += pendingStarts.toString() + line
            pendingStarts.clear()
            continue
        }
        val starts = line.filter { it == SpoilerStartMarker }
        val ends = line.filter { it == SpoilerEndMarker }
        when {
            starts.isNotEmpty() && ends.isEmpty() -> pendingStarts.append(starts)
            ends.isNotEmpty() && starts.isEmpty() ->
                if (kept.isNotEmpty()) kept[kept.lastIndex] += ends
            // Both on a marker-only line: an empty spoiler, nothing to reveal.
        }
    }
    return kept.joinToString("\n")
}

/**
 * Removes the markers and turns them into ranges over the cleaned text. Nested
 * spoilers collapse to their outer range; an unclosed opening runs to the end;
 * a stray closing marker is ignored; whitespace-only ranges are dropped.
 */
internal fun String.finalizeSpoilers(): SpoilerText {
    val marked = replace(BlankLinesRegex, "\n\n").trim()
    val text = StringBuilder()
    val rawRanges = mutableListOf<IntRange>()
    var depth = 0
    var start = 0
    marked.forEach { char ->
        when (char) {
            SpoilerStartMarker -> {
                if (depth == 0) start = text.length
                depth++
            }
            SpoilerEndMarker -> {
                if (depth > 0) {
                    depth--
                    if (depth == 0) rawRanges += start until text.length
                }
            }
            else -> text.append(char)
        }
    }
    if (depth > 0) rawRanges += start until text.length

    val unmarked = text.toString()
    val firstContent = unmarked.indexOfFirst { !it.isWhitespace() }
    if (firstContent == -1) return SpoilerText(text = "", spoilers = emptyList())
    val lastContent = unmarked.indexOfLast { !it.isWhitespace() }
    val clean = unmarked.substring(firstContent, lastContent + 1)
    val spoilers = rawRanges.mapNotNull { range ->
        var from = (range.first - firstContent).coerceAtLeast(0)
        var to = (range.last + 1 - firstContent).coerceAtMost(clean.length)
        while (from < to && clean[from].isWhitespace()) from++
        while (to > from && clean[to - 1].isWhitespace()) to--
        if (to > from) from until to else null
    }
    return SpoilerText(text = clean, spoilers = spoilers)
}

private fun Char.isSpoilerMarker(): Boolean =
    this == SpoilerStartMarker || this == SpoilerEndMarker

/**
 * Splits cleaned text into prose and spoiler-paragraph blocks. A range is a
 * block when it occupies whole lines (starts at the text start or after a
 * newline, and ends at the text end or before a newline); anything else stays
 * inline inside its prose. Original range indices are preserved in both shapes.
 */
internal fun SpoilerText.toSegments(): List<SpoilerSegment> {
    val blocks = mutableListOf<Pair<Int, IntRange>>()
    val inline = mutableListOf<Pair<Int, IntRange>>()
    spoilers.forEachIndexed { index, range ->
        if (range.isWholeLineBlock(text)) blocks += index to range else inline += index to range
    }

    val segments = mutableListOf<SpoilerSegment>()
    var cursor = 0

    fun addProse(rawStart: Int, rawEnd: Int) {
        var start = rawStart.coerceIn(0, text.length)
        var end = rawEnd.coerceIn(start, text.length)
        while (start < end && text[start] == '\n') start++
        while (end > start && text[end - 1] == '\n') end--
        if (end <= start) return
        val inlineSpoilers = inline.mapNotNull { (index, range) ->
            if (range.first >= start && range.last < end) {
                InlineSpoiler(
                    spoilerIndex = index,
                    range = (range.first - start) until (range.last + 1 - start),
                )
            } else {
                null
            }
        }
        segments += SpoilerSegment.Prose(text = text.substring(start, end), inlineSpoilers = inlineSpoilers)
    }

    blocks.forEach { (index, range) ->
        addProse(cursor, range.first)
        val blockEnd = (range.last + 1).coerceAtMost(text.length)
        if (range.first < blockEnd) {
            segments += SpoilerSegment.Block(
                text = text.substring(range.first, blockEnd),
                spoilerIndex = index,
            )
        }
        cursor = blockEnd
    }
    addProse(cursor, text.length)
    return segments
}

private fun IntRange.isWholeLineBlock(text: String): Boolean {
    if (first !in text.indices || last !in text.indices) return false
    val startsAtLine = first == 0 || text[first - 1] == '\n'
    val endsAtLine = last == text.lastIndex || text[last + 1] == '\n'
    return startsAtLine && endsAtLine
}
