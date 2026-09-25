package com.luum.michi.app.core.storage.domain

/**
 * Store keys shared across the composition root. `App` (theme + language) and `Root`
 * (default discover tab) read these at startup, while [com.luum.michi.app.settings.ui.state.SettingsState]
 * writes them — so the strings must stay in one place.
 */
internal object SettingsStoreKeys {
    const val ThemeMode = "theme_mode"
    const val ThemePalette = "theme_palette"
    const val Font = "app_font"
    const val Language = "app_language"
    const val DefaultDiscoverTab = "default_home_tab"
    /** Sign-in modal auto-opened once (first anonymous launch, never again). */
    const val SeenSignIn = "seen_sign_in"
}
