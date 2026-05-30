package com.luum.michi.app.core.platform.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Bottom padding for scrollable content on tabs that show the floating bottom nav bar,
 * so the last item clears the bar *and* the system bottom inset (iOS Home Indicator /
 * Android gesture or nav bar). [base] is the visual gap used on Android with no inset.
 */
@Composable
fun bottomNavBarClearance(base: Dp = 96.dp): Dp =
    base + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
