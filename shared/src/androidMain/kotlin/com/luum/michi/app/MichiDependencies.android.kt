package com.luum.michi.app

import android.content.Context
import android.content.pm.ApplicationInfo
import com.luum.michi.app.core.auth.repository.ChromeCustomTabsOAuthLauncher
import com.luum.michi.app.core.auth.repository.SharedPreferencesAniListTokenStorage
import com.luum.michi.app.core.platform.repository.AndroidUrlOpener
import com.luum.michi.app.core.platform.repository.AndroidShareLauncher
import com.luum.michi.app.core.storage.repository.SharedPreferencesSettingsStore
import com.luum.michi.app.core.storage.repository.AndroidFileStore
import com.luum.michi.app.core.platform.repository.AndroidConnectivity

/**
 * Constructs the app's shared dependencies for Android. Called from
 * [MainActivity]. Encapsulates the platform-specific wiring so the
 * `androidApp` module never sees the platform impl types directly.
 */
fun createMichiDependencies(context: Context): MichiDependencies = MichiDependencies(
    tokenStorage = SharedPreferencesAniListTokenStorage(context.applicationContext),
    oAuthLauncher = ChromeCustomTabsOAuthLauncher(context.applicationContext),
    urlOpener = AndroidUrlOpener(context.applicationContext),
    shareLauncher = AndroidShareLauncher(context.applicationContext),
    settingsStore = SharedPreferencesSettingsStore(context.applicationContext),
    fileStore = AndroidFileStore(context.applicationContext),
    connectivity = AndroidConnectivity(context.applicationContext),
    isDebug = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0,
)
