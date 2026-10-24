package com.luum.michi.app.core.language.domain

data class AppLanguage(val code: String, val displayName: String) {
    companion object {
        val available: List<AppLanguage> = listOf(
            AppLanguage(code = "es", displayName = "Español"),
            AppLanguage(code = "en", displayName = "English"),
        )
        // Explicit by code (not position): default app language is English.
        val default: AppLanguage = available.first { it.code == "en" }

        fun fromCode(code: String?): AppLanguage {
            return available.firstOrNull { language ->
                language.code == code?.substringBefore("-")?.substringBefore("_")
            } ?: default
        }
    }
}
