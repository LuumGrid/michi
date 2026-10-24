package com.luum.michi.app.core.util

private val HtmlTagRegex = Regex("<[^>]+>")
private val HtmlBreakRegex = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)
private val HtmlBlankLinesRegex = Regex("\n{3,}")
// `&amp;` is decoded LAST: doing it first would turn `&amp;lt;` into `<`
// (a double decode of what the author wrote literally).
internal val HtmlEntities = mapOf(
    "&lt;" to "<",
    "&gt;" to ">",
    "&quot;" to "\"",
    "&#39;" to "'",
    "&apos;" to "'",
    "&nbsp;" to " ",
    "&mdash;" to "—",
    "&ndash;" to "–",
    "&hellip;" to "…",
    "&amp;" to "&",
)
private val NumericEntityRegex = Regex("&#(\\d+);")
private const val MaxUnicodeCodePoint = 0x10FFFF

internal fun String.stripHtml(): String {
    val withBreaks = replace(HtmlBreakRegex, "\n")
    var result = HtmlTagRegex.replace(withBreaks, "")
    // Numeric entities first: `&amp;#60;` was written as a literal "&#60;", so
    // decoding `&amp;` first would turn it into "<" (a double decode).
    result = NumericEntityRegex.replace(result) { match ->
        val codePoint = match.groupValues[1].toIntOrNull()
            ?.takeIf { it in 0..MaxUnicodeCodePoint }
        when {
            codePoint == null -> match.value
            codePoint <= 0xFFFF -> codePoint.toChar().toString()
            // Astral planes: manual surrogate pair. Pure Int/Char arithmetic on
            // purpose — no stdlib API whose common availability we'd have to trust.
            else -> {
                val v = codePoint - 0x10000
                charArrayOf(
                    ((v ushr 10) + 0xD800).toChar(),
                    ((v and 0x3FF) + 0xDC00).toChar(),
                ).concatToString()
            }
        }
    }
    HtmlEntities.forEach { (entity, char) -> result = result.replace(entity, char) }
    return result
        .replace(HtmlBlankLinesRegex, "\n\n")
        .trim()
}
