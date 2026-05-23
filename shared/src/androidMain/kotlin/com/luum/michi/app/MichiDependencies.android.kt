package com.luum.michi.app

import android.content.Context
import com.luum.michi.app.core.auth.ChromeCustomTabsOAuthLauncher
import com.luum.michi.app.core.auth.SharedPreferencesAniListTokenStorage

/**
 * Constructs the app's shared dependencies for Android. Called from
 * [MainActivity]. Encapsulates the platform-specific wiring so the
 * `androidApp` module never sees the platform impl types directly.
 */
fun createMichiDependencies(context: Context): MichiDependencies = MichiDependencies(
    tokenStorage = SharedPreferencesAniListTokenStorage(context.applicationContext),
    oAuthLauncher = ChromeCustomTabsOAuthLauncher(context.applicationContext),
)
