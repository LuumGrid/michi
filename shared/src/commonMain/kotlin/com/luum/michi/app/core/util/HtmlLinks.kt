package com.luum.michi.app.core.util

import com.luum.michi.app.core.model.ExternalLink

/** Cleaned biography text plus the external links the source HTML carried. */
internal data class HtmlContent(
    val text: String,
    val links: List<ExternalLink>,
)

private val AnchorRegex = Regex(
    """<a\b[^>]*href\s*=\s*["']([^"']+)["'][^>]*>(.*?)</a>""",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
)
private val BreakRegex = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)
private val BareUrlRegex = Regex("https?://\\S+", RegexOption.IGNORE_CASE)
// A rótulo ("Twitter:", "Website:") at line start or right after a separator.
private val LabelTokenRegex = Regex("(^|[|·•/,–-])\\s*[A-Za-z][A-Za-z0-9_.]{0,20}:\\s*")
private val PipeSeparatorRegex = Regex("\\s*[|·•]\\s*")
private val SpacedSeparatorRegex = Regex("\\s+[/,–]\\s+")
private val SpacesRegex = Regex("[ \\t]+")
private val BlankLinesRegex = Regex("\n{3,}")

/**
 * Splits HTML into clean text + external links (before stripping tags, so the
 * `href` survives). Anchors and bare URLs (with or without a "Label:") are
 * collected; the link text is removed from the body unless it is real prose
 * (e.g. "worked at <a>Studio Ghibli</a>"), which is kept as plain text. Leftover
 * rótulos and separators are cleaned. Non-http(s) and internal anilist.co hrefs
 * are ignored; links are deduped by a normalized URL (scheme/www/ trailing slash).
 */
internal fun String.toHtmlContent(): HtmlContent {
    val links = mutableListOf<ExternalLink>()
    val seen = mutableSetOf<String>()
    val kept = replace(BreakRegex, "\n").split('\n').mapNotNull { line ->
        // 1) Anchors first: collect them and substitute their text (keep prose,
        //    drop link-y text). Their `href` must not be seen as a bare URL.
        var out = AnchorRegex.replace(line) { match ->
            val url = match.groupValues[1].trim()
            if (!isExternalHttpUrl(url)) {
                match.value
            } else {
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
        // 2) Bare URLs (no anchor) are collected and removed.
        out = BareUrlRegex.replace(out) { match ->
            val url = match.value
            if (!isExternalHttpUrl(url)) {
                match.value
            } else {
                if (seen.add(normalizedUrl(url))) {
                    links += ExternalLink(site = hostLabel(url), url = url)
                }
                ""
            }
        }
        val cleaned = cleanLine(out)
        if (cleaned.isBlank()) null else cleaned
    }
    val text = kept.joinToString("\n").stripHtml().replace(BlankLinesRegex, "\n\n").trim()
    return HtmlContent(text = text, links = links)
}

/** Text that only repeats the link (URL, `@handle`, or the site name itself). */
private fun isLinkyText(text: String, url: String): Boolean =
    text.isEmpty() ||
        text.startsWith("http", ignoreCase = true) ||
        text.startsWith("@") ||
        text.equals(hostLabel(url), ignoreCase = true)

private fun cleanLine(line: String): String {
    var s = line.stripHtml()
    s = LabelTokenRegex.replace(s) { " " }
    s = PipeSeparatorRegex.replace(s, " ")
    s = SpacedSeparatorRegex.replace(s, " ")
    return s.replace(SpacesRegex, " ").trim()
}

private fun isExternalHttpUrl(url: String): Boolean {
    val scheme = url.substringBefore("://", missingDelimiterValue = "")
    if (!scheme.equals("http", ignoreCase = true) && !scheme.equals("https", ignoreCase = true)) return false
    val host = url.substringAfter("://", "").substringBefore('/').lowercase()
    return host.isNotEmpty() && !host.endsWith("anilist.co")
}

/** Scheme-agnostic key so http/https and www/non-www duplicates collapse. */
private fun normalizedUrl(url: String): String {
    val rest = url.substringAfter("://", "")
    val host = rest.substringBefore('/').lowercase().removePrefix("www.")
    val path = rest.substringAfter('/', "").trimEnd('/')
    return "$host/$path"
}

/** A label derived from the host (the catalog may override it with the site name). */
private fun hostLabel(url: String): String {
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
