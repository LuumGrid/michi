package com.luum.michi.app.ui

import androidx.compose.ui.Modifier
import com.luum.michi.app.core.navigation.domain.TabSection

/**
 * Stable UiAutomator handles for the Baseline Profile generator and the
 * startup benchmark. They are semantics tags only: no visual change.
 *
 * `testTagsAsResourceIds` maps them to `viewIdResourceName` on Android, so
 * UiAutomator can match them with `viewIdResourceName == TestTags.X`.
 *
 * Keep the literals in sync with
 * `baselineprofile/src/main/kotlin/com/luum/michi/baselineprofile/UiAutomation.kt`.
 */
internal object TestTags {
    const val AuthGuest = "michi.auth.guest"
    const val Dashboard = "michi.discover.list"
    const val Rail = "michi.rail"
    const val Poster = "michi.poster"
    const val Detail = "michi.detail.list"

    fun tab(section: TabSection): String = "michi.tab.${section.name}"
}

/**
 * Android-only Compose flag: expose `testTag`s as accessibility resource IDs.
 * iOS has no UiAutomator, so it is a no-op there.
 */
internal expect fun Modifier.testTagsAsResourceIds(): Modifier
