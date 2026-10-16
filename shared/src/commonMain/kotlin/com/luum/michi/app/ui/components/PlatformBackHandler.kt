package com.luum.michi.app.ui.components

import androidx.compose.runtime.Composable

/**
 * Platform system-back hook used by `Root` to funnel the OS back gesture
 * through the app's progressive back step. Android routes through its back
 * dispatcher; iOS has no system back gesture wired here (its navigation is
 * gesture-driven at the framework level), so it is a no-op there.
 *
 * [enabled] false means "there is nothing for the app to consume": the platform
 * handles the gesture (leaving the app), which is exactly what
 * `State.nextBackStep() == BackStep.SystemDefault` means.
 */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
