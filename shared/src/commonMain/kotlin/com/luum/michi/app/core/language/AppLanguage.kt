package com.luum.michi.app.core.language

data class AppLanguage(val code: String, val displayName: String) {
    companion object {
        val available: List<AppLanguage> = listOf(
            AppLanguage(code = "es", displayName = "Español"),
            AppLanguage(code = "en", displayName = "English"),
            AppLanguage(code = "pt", displayName = "Português"),
            AppLanguage(code = "fr", displayName = "Français"),
        )
        val default: AppLanguage = available.first()
    }
}
