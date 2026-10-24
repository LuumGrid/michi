package com.luum.michi.app.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Radius tokens, named by form (not by role). Use the token, never a raw
 * `RoundedCornerShape(n.dp)`:
 * - [CoverShape] 12 — covers (posters, portraits, the media hero cover).
 * - [PanelShape] 16 — panels, cards, bands, settings groups, sheet fields.
 * - [PillShape] 24 — pills: floating tools (buttons, toolbar capsules) and flat
 *   chips alike.
 * - [PillEndShape] — the trailing half of a fused pill (a counter + its action):
 *   rounded on the end, square on the start so it butts against its other half.
 */
internal val CoverShape = RoundedCornerShape(12.dp)

internal val PanelShape = RoundedCornerShape(16.dp)

internal val PillShape = RoundedCornerShape(24.dp)

internal val PillEndShape = RoundedCornerShape(
    topStart = 0.dp,
    topEnd = 24.dp,
    bottomEnd = 24.dp,
    bottomStart = 0.dp,
)
