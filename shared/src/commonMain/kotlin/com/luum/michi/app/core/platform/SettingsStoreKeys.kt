package com.luum.michi.app.core.platform

/**
 * Store keys shared across the composition root. `App` (theme + language) and `ShellScreen`
 * (default home tab) read these at startup, while [com.luum.michi.app.settings.presentation.state.SettingsState]
 * writes them — so the strings must stay in one place.
 */
internal object SettingsStoreKeys {
    const val ThemeMode = "theme_mode"
    const val Language = "app_language"
    const val DefaultHomeTab = "default_home_tab"
}
