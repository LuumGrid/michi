package com.luum.michi.app.root

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luum.michi.app.ui.components.PrimaryButton
import com.luum.michi.app.ui.theme.brandColor

/**
 * One login entry per service (AniList today; MAL and others tomorrow by
 * appending to the list). Brand literal kept untranslated on purpose.
 * [brandHex] is a raw hex String converted at the UI boundary (project rule).
 */
internal data class AuthService(
    val id: String,
    val label: String,
    val onLogin: () -> Unit,
    val icon: ImageVector? = null,
    val brandHex: String? = null,
)

/**
 * Service buttons shared by every sign-in surface (landing yesterday, the
 * sign-in modal today): one branded button per entry, or the
 * not-configured note when OAuth isn't set up. Emits items for the
 * caller's Column — spacing around them belongs to the caller.
 */
@Composable
internal fun AuthServiceButtons(
    services: List<AuthService>,
    isConfigured: Boolean,
    configMissingLabel: String,
    modifier: Modifier = Modifier,
) {
    if (isConfigured) {
        services.forEach { service ->
            val brand = service.brandHex?.let { brandColor(it) }
            PrimaryButton(
                label = service.label,
                onClick = service.onLogin,
                leadingIcon = service.icon,
                containerColor = brand,
                contentColor = if (brand != null) Color.White else null,
                modifier = modifier,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    } else {
        Text(
            text = configMissingLabel,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = modifier,
        )
    }
}
