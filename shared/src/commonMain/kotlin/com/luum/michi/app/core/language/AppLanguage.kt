package com.luum.michi.app.core.language

data class AppLanguage(val code: String, val displayName: String) {
    companion object {
        val available: List<AppLanguage> = listOf(
            AppLanguage(code = "es", displayName = "Espanol"),
            AppLanguage(code = "en", displayName = "English"),
            AppLanguage(code = "pt", displayName = "Portugues"),
            AppLanguage(code = "fr", displayName = "Frances"),
        )
        val default: AppLanguage = available.first()
    }
}
