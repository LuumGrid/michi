package com.luum.michi.app.core.platform.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Bottom padding for scrollable content on tabs that show the floating bottom tab bar,
 * so the last item clears the bar *and* the system bottom inset (iOS Discover Indicator /
 * Android gesture or nav bar). [base] is the visual gap used on Android with no inset.
 * [extraBottom] covers transient overlays above the tab bar (e.g. the bottom search pill).
 */
@Composable
fun tabBarClearance(base: Dp = 80.dp, extraBottom: Dp = 0.dp): Dp =
    base + extraBottom + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

/**
 * Top clearance matching the floating global toolbar (status bars + 52dp pill + 16dp
 * vertical padding), so scrollable content starts below the pills while detail banners
 * intentionally bleed underneath them.
 */
@Composable
fun floatingToolbarClearance(): Dp =
    WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 68.dp
