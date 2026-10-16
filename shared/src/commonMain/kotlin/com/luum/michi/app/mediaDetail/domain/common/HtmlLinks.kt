package com.luum.michi.app.mediaDetail.domain.common

import com.luum.michi.app.core.util.canonicalHost
import com.luum.michi.app.core.util.isAniListHost
import com.luum.michi.app.core.util.urlHost

/** One `Label: value` line AniList marked in bold inside a biography. */
internal data class BiographyStat(val label: String, val value: String)

/** Cleaned biography text plus the external links and bold stats the HTML carried. */
internal data class HtmlContent(
    val text: String,
    val links: List<ExternalLink>,
    val stats: List<BiographyStat>,
)

private val AnchorRegex = Regex(
    """<a\b[^>]*href\s*=\s*["']([^"']+)["'][^>]*>(.*?)</a>""",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
)
private val BreakRegex = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)
private val BareUrlRegex = Regex("https?://[^\\s<>\"']+", RegexOption.IGNORE_CASE)
// AniList's spoiler wrapper: `<span class='markdown_spoiler'><span>…</span></span>`
// (inline, or opened on one line and closed on a later one).
private val SpoilerOpenRegex = Regex("""<span\b[^>]*markdown_spoiler[^>]*>""", RegexOption.IGNORE_CASE)
private val SpoilerCloseRegex = Regex("""</span>\s*</span>""", RegexOption.IGNORE_CASE)
// A bold stat label: `<strong>Height:</strong>`, `<strong>Height</strong>:` or `<b>…</b>`.
private val BoldLabelRegex = Regex(
    """<(?:b|strong)\b[^>]*>(.*?)</(?:b|strong)>\s*(:?)\s*""",
    setOf(RegexOption.IGNORE_CASE),
)
// Everything before the first bold label must be tags/whitespace, so a bold word
// inside a sentence is not mistaken for a stat.
private val TagsOnlyRegex = Regex("""^\s*(?:<[^>]+>\s*)*$""")
// A rótulo ("Twitter:", "Website:") at line start or right after a separator.
private val LabelTokenRegex = Regex("(^|[|·•/,–-])\\s*[A-Za-z][A-Za-z0-9_.]{0,20}:\\s*")
private val PipeSeparatorRegex = Regex("\\s*[|·•]\\s*")
private val SpacedSeparatorRegex = Regex("\\s+[/,–]\\s+")
private val SpacesRegex = Regex("[ \\t]+")
private val BlankLinesRegex = Regex("\n{3,}")

/**
 * Splits HTML into clean text + external links + bold stats (before stripping
 * tags, so the `href` and the bold survive). Anchors and bare URLs (with or
 * without a "Label:") are collected; the link text is removed from the body
 * unless it is real prose (e.g. "worked at <a>Studio Ghibli</a>"), which is kept
 * as plain text. A line that only repeats the text of the links that were pulled
 * out (e.g. "Profile (Arts Vision) Wiki (jp) Wiki (en)", "Website") is dropped
 * too, so a biography does not end with a row of bare link labels. Leftover
 * rótulos and separators are cleaned. Non-http(s) and internal anilist.co hrefs
 * are ignored; links are deduped by a normalized URL (scheme/www/ trailing slash).
 *
 * A `Label: value` line whose label AniList marked in bold becomes a
 * [BiographyStat] and leaves the text (AniList mixes them anywhere, not only at
 * the top, and one line can carry several); its value is cleaned like any other
 * text and any external link it carried still lands in [HtmlContent.links]. A
 * line inside a `markdown_spoiler` is left as prose — promoting it would put a
 * spoiler at the top of the biography.
 */
internal fun String.toHtmlContent(): HtmlContent {
    val links = mutableListOf<ExternalLink>()
    val seen = mutableSetOf<String>()
    // Texts of the anchors we are about to extract: a line made only of them is
    // the link list left behind, not prose.
    val linkTexts = AnchorRegex.findAll(this)
        .filter { isExternalHttpUrl(it.groupValues[1].trim()) }
        .map { cleanLine(it.groupValues[2], stripLabels = true).lowercase() }
        .filter { it.isNotEmpty() }
        .toSet()
    fun extractText(raw: String): String {
        // 1) Anchors first: collect them and substitute their text (keep prose,
        //    drop link-y text). Their `href` must not be seen as a bare URL.
        var removedLink = false
        var out = AnchorRegex.replace(raw) { match ->
            val url = match.groupValues[1].trim().trimUrlTrailingPunctuation()
            if (!isExternalHttpUrl(url)) {
                match.value
            } else {
                removedLink = true
                val text = match.groupValues[2].stripHtml().trim()
                if (seen.add(normalizedUrl(url))) {
                    links += ExternalLink(
                        site = hostLabel(url),
                        url = url,
                        handle = text.takeIf { it.startsWith("@") && it.length > 1 },
                    )
                }
                if (isLinkyText(text, url)) "" else text
            }
        }
        // 2) Bare URLs (no anchor) are collected and removed. The punctuation
        //    trimmed off the URL (a sentence period, an unmatched bracket) stays
        //    in the prose.
        out = BareUrlRegex.replace(out) { match ->
            val url = match.value.trimUrlTrailingPunctuation()
            if (!isExternalHttpUrl(url)) {
                match.value
            } else {
                removedLink = true
                if (seen.add(normalizedUrl(url))) {
                    links += ExternalLink(site = hostLabel(url), url = url)
                }
                match.value.substring(url.length)
            }
        }
        // Rótulos ("Twitter:", "Website:") are only stripped from lines that
        // actually carried links; doing it on every line mangles prose ("Re:Zero").
        return cleanLine(out, stripLabels = removedLink)
    }
    val stats = mutableListOf<BiographyStat>()
    // A stat hidden inside a `markdown_spoiler` stays as prose: promoting it
    // would put a spoiler at the top of the biography.
    var spoilerDepth = 0
    val kept = replace(BreakRegex, "\n").split('\n').mapNotNull { line ->
        val inSpoiler = spoilerDepth > 0 || SpoilerOpenRegex.containsMatchIn(line)
        spoilerDepth = (spoilerDepth + line.spoilerBalance()).coerceAtLeast(0)
        if (!inSpoiler) {
            // A bold stat line (or several on one line): keep the label/value
            // pairs and drop the line from the prose. A bold line without a
            // usable value falls through as normal text.
            val rawStats = line.toRawStats()
            if (rawStats.isNotEmpty()) {
                val values = rawStats.map { extractText(it.valueHtml) }
                if (values.all { it.isNotBlank() }) {
                    rawStats.forEachIndexed { index, raw ->
                        stats += BiographyStat(label = raw.label, value = values[index])
                    }
                    return@mapNotNull null
                }
            }
        }
        val cleaned = extractText(line)
        if (cleaned.isBlank() || cleaned.isOnlyLinkText(linkTexts)) null else cleaned
    }
    val text = kept.joinToString("\n").replace(BlankLinesRegex, "\n\n").trim()
    return HtmlContent(text = text, links = links, stats = stats)
}

private data class RawStat(val label: String, val valueHtml: String)

/** `markdown_spoiler` wrappers the line opens minus the ones it closes. */
private fun String.spoilerBalance(): Int =
    SpoilerOpenRegex.findAll(this).count() - SpoilerCloseRegex.findAll(this).count()

/** The bold label of a match, or null when it is not a `Label:` shape. */
private fun MatchResult.statLabel(): String? {
    val raw = groupValues[1].trim()
    // The colon is inside the bold (`Height:`) or right after it (`Height`).
    if (!raw.endsWith(":") && groupValues[2] != ":") return null
    return raw.removeSuffix(":").stripHtml().trim().takeIf { it.isNotEmpty() }
}

/**
 * The `Label: value` pairs a line carries — a line can hold several bold stats —
 * or an empty list when the line is not a stats line. The line must start with
 * the bold label (only tags/whitespace before it); each value runs to the next
 * bold label that has the shape, so a bold word inside a value never splits it.
 */
private fun String.toRawStats(): List<RawStat> {
    val first = BoldLabelRegex.find(this) ?: return emptyList()
    if (!TagsOnlyRegex.matches(substring(0, first.range.first))) return emptyList()
    val stats = mutableListOf<RawStat>()
    var index = first.range.first
    while (index < length) {
        val match = BoldLabelRegex.find(this, index)?.takeIf { it.range.first == index } ?: return emptyList()
        val label = match.statLabel() ?: return emptyList()
        val valueStart = match.range.last + 1
        val next = BoldLabelRegex.findAll(this, valueStart).firstOrNull { it.statLabel() != null }
        val valueEnd = next?.range?.first ?: length
        stats += RawStat(label = label, valueHtml = substring(valueStart, valueEnd).trim())
        index = valueEnd
    }
    return stats
}

/**
 * True when, after removing every extracted link text, nothing readable is left:
 * the line only repeated the labels of links that were already pulled out.
 */
private fun String.isOnlyLinkText(linkTexts: Set<String>): Boolean {
    if (linkTexts.isEmpty()) return false
    var rest = lowercase()
    linkTexts.sortedByDescending { it.length }.forEach { rest = rest.replace(it, " ") }
    return rest.none { it.isLetterOrDigit() }
}

/** Text that only repeats the link (URL, `@handle`, or the site name itself). */
private fun isLinkyText(text: String, url: String): Boolean =
    text.isEmpty() ||
        text.startsWith("http", ignoreCase = true) ||
        text.startsWith("@") ||
        text.equals(hostLabel(url), ignoreCase = true)

private fun cleanLine(line: String, stripLabels: Boolean): String {
    var s = line.stripHtml()
    if (stripLabels) s = LabelTokenRegex.replace(s) { " " }
    s = PipeSeparatorRegex.replace(s, " ")
    s = SpacedSeparatorRegex.replace(s, " ")
    return s.replace(SpacesRegex, " ").trim()
}

/**
 * Drops the punctuation a bare URL tends to pick up from surrounding prose
 * (`https://x.com).` / `https://x.com,`). A closing bracket is kept when it is
 * balanced (`.../Foo_(bar)`), so real URLs survive.
 */
private fun String.trimUrlTrailingPunctuation(): String {
    var end = length
    while (end > 0) {
        when (this[end - 1]) {
            '.', ',', ';', ':', '!', '?', '"', '\'' -> end--
            ')' -> if (count { it == '(' } < count { it == ')' }) end-- else break
            ']' -> if (count { it == '[' } < count { it == ']' }) end-- else break
            '}' -> if (count { it == '{' } < count { it == '}' }) end-- else break
            else -> break
        }
    }
    return substring(0, end)
}

private fun isExternalHttpUrl(url: String): Boolean {
    val host = urlHost(url) ?: return false
    return !isAniListHost(host)
}

/** Scheme-agnostic key so http/https and www/non-www duplicates collapse. */
private fun normalizedUrl(url: String): String {
    val rest = url.substringAfter("://", "")
    val host = urlHost(url).orEmpty()
    val path = rest.substringAfter('/', "").trimEnd('/')
    return "$host/$path"
}

/** A label derived from the host (the catalog may override it with the site name). */
private fun hostLabel(url: String): String {
    val host = urlHost(url).orEmpty()
    return when {
        canonicalHost(url) == "twitter.com" || host.endsWith(".twitter.com") -> "Twitter"
        canonicalHost(url) == "youtube.com" || host.endsWith(".youtube.com") -> "YouTube"
        host.endsWith("instagram.com") -> "Instagram"
        host.endsWith("facebook.com") -> "Facebook"
        host.endsWith("pixiv.net") -> "Pixiv"
        host.endsWith("tiktok.com") -> "TikTok"
        host.endsWith("tumblr.com") -> "Tumblr"
        host.endsWith("linkedin.com") -> "LinkedIn"
        else -> host
    }
}
