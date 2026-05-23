package com.luum.michi.app.account.presentation.util

import com.luum.michi.app.account.presentation.model.AccountProfileLink

internal fun isSafeProfileUrl(value: String): Boolean {
    if (value.isBlank() || value.length > 2_048) return false
    val lower = value.lowercase()
    if (!lower.startsWith("https://")) return false
    if (lower.any(Char::isWhitespace)) return false
    val hostStart = "https://".length
    val hostEnd = lower.indexOf('/', startIndex = hostStart).let { if (it == -1) lower.length else it }
    val host = lower.substring(hostStart, hostEnd)
    return "." in host && host.none { it == '/' || it == '\\' || it == '@' }
}

internal fun AccountProfileLink.sanitized(): AccountProfileLink? {
    val cleanTitle = title.trim()
    val cleanUrl = url.trim()
    if (cleanTitle.isBlank() || !isSafeProfileUrl(cleanUrl)) return null
    return AccountProfileLink(title = cleanTitle, url = cleanUrl)
}
