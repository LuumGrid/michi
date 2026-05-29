package com.luum.michi.app.core.util

private val HtmlTagRegex = Regex("<[^>]+>")
internal val HtmlEntities = mapOf(
    "&amp;" to "&",
    "&lt;" to "<",
    "&gt;" to ">",
    "&quot;" to "\"",
    "&#39;" to "'",
    "&apos;" to "'",
    "&nbsp;" to " ",
    "&mdash;" to "—",
    "&ndash;" to "–",
    "&hellip;" to "…",
)
private val NumericEntityRegex = Regex("&#(\\d+);")

internal fun String.stripHtml(): String {
    val withBreaks = replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
    var result = HtmlTagRegex.replace(withBreaks, "")
    HtmlEntities.forEach { (entity, char) -> result = result.replace(entity, char) }
    result = NumericEntityRegex.replace(result) { match ->
        match.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: match.value
    }
    return result
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
}
