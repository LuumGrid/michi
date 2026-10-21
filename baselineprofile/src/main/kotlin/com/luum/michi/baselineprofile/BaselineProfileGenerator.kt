package com.luum.michi.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.Direction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Critical user journeys for the Baseline Profile: cold startup to the first
 * Discover content, then the first vertical/horizontal scrolls and one media
 * detail round trip. Everything runs as a guest (no login).
 *
 * Iterations are intentionally low: every launch hits AniList, whose public
 * limit is 30 requests per minute.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    /** Startup-only rules, used by AGP for DEX layout (startup profile). */
    @Test
    fun startup() = baselineProfileRule.collect(
        packageName = TARGET_PACKAGE,
        maxIterations = 3,
        stableIterations = 2,
        includeInStartupProfile = true,
    ) {
        startApp(TARGET_PACKAGE)
        dismissAuthIfShown()
        ensureDiscoverTab()
        awaitDiscoverContent()
    }

    /** Baseline rules for the first scrolls and one media detail round trip. */
    @Test
    fun discoverJourney() = baselineProfileRule.collect(
        packageName = TARGET_PACKAGE,
        maxIterations = 3,
        stableIterations = 2,
        includeInStartupProfile = false,
    ) {
        startApp(TARGET_PACKAGE)
        dismissAuthIfShown()
        ensureDiscoverTab()
        awaitDiscoverContent()

        // Horizontal rail, then the dashboard's vertical list.
        onElement { viewIdResourceName == Tags.Rail }.fling(Direction.LEFT)
        onElement { viewIdResourceName == Tags.Dashboard }.fling(Direction.DOWN)
        onElement { viewIdResourceName == Tags.Dashboard }.fling(Direction.UP)

        // Open the first media detail, scroll it, and come back.
        onElement { viewIdResourceName == Tags.Poster }.click()
        onElement(timeoutMs = 20_000) { viewIdResourceName == Tags.Detail }
            .fling(Direction.DOWN)
        pressBack()
    }
}
