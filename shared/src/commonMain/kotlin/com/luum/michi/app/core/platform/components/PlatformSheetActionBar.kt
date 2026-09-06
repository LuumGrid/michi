package com.luum.michi.app.core.platform.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Qué lado lleva el énfasis primario (filled). El otro lado es outlined. */
internal enum class PlatformActionButtonEmphasis { FILLED, OUTLINED }

/**
 * Única action-bar de dos botones de todo el proyecto (sort, filtros, editor, share).
 * Cada lado puede ser filled u outlined vía [leadingEmphasis]/[trailingEmphasis]
 * (defaults: izquierda outlined, derecha filled) y llevar un ícono opcional.
 * La variante destructiva en rojo solo aplica al leading outlined.
 */
@Composable
internal fun PlatformSheetActionBar(
    leadingLabel: String,
    trailingLabel: String,
    onLeadingClick: () -> Unit,
    onTrailingClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: Painter? = null,
    trailingIcon: Painter? = null,
    leadingEmphasis: PlatformActionButtonEmphasis = PlatformActionButtonEmphasis.OUTLINED,
    trailingEmphasis: PlatformActionButtonEmphasis = PlatformActionButtonEmphasis.FILLED,
    leadingDestructive: Boolean = false,
    leadingEnabled: Boolean = true,
    trailingEnabled: Boolean = true,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            ActionBarButton(
                label = leadingLabel,
                icon = leadingIcon,
                onClick = onLeadingClick,
                enabled = leadingEnabled,
                emphasis = leadingEmphasis,
                destructive = leadingDestructive,
                labelWeight = FontWeight.SemiBold,
            )
            ActionBarButton(
                label = trailingLabel,
                icon = trailingIcon,
                onClick = onTrailingClick,
                enabled = trailingEnabled,
                emphasis = trailingEmphasis,
                destructive = false,
                labelWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun RowScope.ActionBarButton(
    label: String,
    icon: Painter?,
    onClick: () -> Unit,
    enabled: Boolean,
    emphasis: PlatformActionButtonEmphasis,
    destructive: Boolean,
    labelWeight: FontWeight,
) {
    val content: @Composable RowScope.() -> Unit = {
        if (icon != null) {
            Icon(
                painter = icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (destructive) FontWeight.Normal else labelWeight,
            maxLines = 1,
        )
    }
    val buttonModifier = Modifier
        .weight(1f)
        .height(48.dp)
    if (emphasis == PlatformActionButtonEmphasis.FILLED) {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(24.dp),
            modifier = buttonModifier,
            content = content,
        )
    } else if (destructive) {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = buttonModifier,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
            content = content,
        )
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = buttonModifier,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            content = content,
        )
    }
}
