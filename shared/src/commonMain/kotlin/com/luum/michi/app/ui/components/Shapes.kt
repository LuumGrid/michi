package com.luum.michi.app.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Radius tokens, named by form (not by role): a session of the surface sprint
 * (5.4) settled on three radii. Use the token, never a raw
 * `RoundedCornerShape(n.dp)`.
 */
/** Covers (posters, portraits, banners' centered cover). */
internal val CoverShape = RoundedCornerShape(12.dp)

/** Panels, cards, bands, settings groups and sheet fields. */
internal val PanelShape = RoundedCornerShape(16.dp)

/** Pills: floating tools (buttons, toolbar capsules) and flat chips alike. */
internal val PillShape = RoundedCornerShape(24.dp)
