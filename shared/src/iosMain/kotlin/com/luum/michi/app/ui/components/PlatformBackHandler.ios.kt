package com.luum.michi.app.ui.components

import androidx.compose.runtime.Composable

/**
 * iOS has no system back affordance to intercept: the app's own back button
 * (toolbar chevron) is the only back path, so this stays a no-op.
 */
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // No-op: no platform back gesture is wired on iOS today.
}
