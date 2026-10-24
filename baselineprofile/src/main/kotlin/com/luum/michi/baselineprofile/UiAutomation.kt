package com.luum.michi.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope

internal const val TARGET_PACKAGE = "com.luum.michi.app"

/**
 * Resource IDs exposed by the app through `testTagsAsResourceId`.
 *
 * The test module runs in its own APK and cannot import the app's internal
 * constants, so keep these literals in sync with
 * `shared/src/commonMain/kotlin/com/luum/michi/app/ui/TestTags.kt`.
 */
internal object Tags {
    const val AuthGuest = "michi.auth.guest"
    const val Dashboard = "michi.discover.list"
    const val Rail = "michi.rail"
    const val Poster = "michi.poster"
    const val Detail = "michi.detail.list"
    const val DiscoverTab = "michi.tab.DISCOVER"
}

/**
 * The sign-in sheet auto-opens once on the first anonymous launch. It is a
 * separate window, so it must be dismissed before interacting with the shell.
 */
internal fun MacrobenchmarkScope.dismissAuthIfShown() {
    onElementOrNull(timeoutMs = 3_000) { viewIdResourceName == Tags.AuthGuest }?.click()
}

/** Re-selects DISCOVER as a safety net (a guest already starts there). */
internal fun MacrobenchmarkScope.ensureDiscoverTab() {
    onElement(timeoutMs = 10_000) { viewIdResourceName == Tags.DiscoverTab }.click()
}

/**
 * The dashboard's LazyColumn only exists once its rails have content, so this
 * waits for actual content rather than for the first frame.
 */
internal fun MacrobenchmarkScope.awaitDiscoverContent(timeoutMs: Long = 60_000) {
    onElement(timeoutMs = timeoutMs) { viewIdResourceName == Tags.Dashboard }
}
