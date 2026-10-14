package com.luum.michi.app.core.util

import com.luum.michi.app.core.model.PersonLink

/** Cleaned biography text plus the external links the source HTML carried. */
internal data class HtmlContent(
    val text: String,
    val links: List<PersonLink>,
)

private val AnchorRegex = Regex(
    """<a\b[^>]*href\s*=\s*["']([^"']+)["'][^>]*>(.*?)</a>""",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
)
private val BreakRegex = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)
private val LeadingLabelRegex = Regex("^\\s*[^:\\s]+:\\s*")
private val BareUrlRegex = Regex("^https?://\\S+$", RegexOption.IGNORE_CASE)
private val BlankLinesRegex = Regex("\n{3,}")

/**
 * Splits HTML into clean text + external links (before stripping tags, so the
 * `href` survives). Lines that are only link metadata ("Twitter:", "Website:
 * https://…", a lone anchor) are dropped; anchors inside prose keep their text.
 * Non-http(s) and internal anilist.co hrefs are ignored; links are deduped by
 * URL.
 */
internal fun String.toHtmlContent(): HtmlContent {
    val links = mutableListOf<PersonLink>()
    val seen = mutableSetOf<String>()
    val kept = replace(BreakRegex, "\n").split('\n').mapNotNull { line ->
        val anchors = AnchorRegex.findAll(line).toList()
        anchors.forEach { match ->
            val url = match.groupValues[1].trim()
            if (isExternalHttpUrl(url) && seen.add(url)) {
                links += PersonLink(label = linkLabel(match.groupValues[2], url), url = url)
            }
        }
        if (anchors.isEmpty()) {
            return@mapNotNull if (BareUrlRegex.matches(line.stripHtml().trim())) null else line
        }
        val withText = AnchorRegex.replace(line) { match ->
            if (isExternalHttpUrl(match.groupValues[1].trim())) match.groupValues[2].stripHtml() else match.value
        }
        // Remove the anchor text (and an optional leading "Label:") — if nothing
        // is left, the line was link metadata and is dropped.
        val remainder = anchors
            .fold(withText) { acc, match -> acc.replaceFirst(match.groupValues[2].stripHtml(), "") }
            .stripHtml()
            .replace(LeadingLabelRegex, "")
            .trim()
        if (remainder.isEmpty()) null else withText
    }
    val text = kept.joinToString("\n").stripHtml().replace(BlankLinesRegex, "\n\n").trim()
    return HtmlContent(text = text, links = links)
}

private fun isExternalHttpUrl(url: String): Boolean {
    val scheme = url.substringBefore("://", missingDelimiterValue = "")
    if (!scheme.equals("http", ignoreCase = true) && !scheme.equals("https", ignoreCase = true)) return false
    val host = url.substringAfter("://", "").substringBefore('/').lowercase()
    return host.isNotEmpty() && !host.endsWith("anilist.co")
}

/** Anchor text when it reads as a label; otherwise a label derived from the host. */
private fun linkLabel(anchorHtml: String, url: String): String {
    val text = anchorHtml.stripHtml().trim()
    if (text.isNotEmpty() && !text.startsWith("http", ignoreCase = true)) return text
    val host = url.substringAfter("://", "").substringBefore('/').removePrefix("www.").lowercase()
    return when {
        host == "twitter.com" || host == "x.com" || host.endsWith(".twitter.com") -> "Twitter"
        host.endsWith("instagram.com") -> "Instagram"
        host.endsWith("youtube.com") || host == "youtu.be" -> "YouTube"
        host.endsWith("facebook.com") -> "Facebook"
        host.endsWith("pixiv.net") -> "Pixiv"
        host.endsWith("tiktok.com") -> "TikTok"
        host.endsWith("tumblr.com") -> "Tumblr"
        host.endsWith("linkedin.com") -> "LinkedIn"
        else -> host
    }
}
