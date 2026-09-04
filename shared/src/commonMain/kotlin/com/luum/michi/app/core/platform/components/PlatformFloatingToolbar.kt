package com.luum.michi.app.core.platform.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val PlatformFloatingPillHeight: Dp = 52.dp
private val PlatformFloatingGroupCorner = RoundedCornerShape(28.dp)

/**
 * Back aislado en círculo (grupo propio, como pide Apple HIG).
 * Color M3 Expressive opaco: secondaryContainer. Sin glass/blur.
 */
@Composable
internal fun PlatformFloatingBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(PlatformFloatingPillHeight),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        tonalElevation = 3.dp,
        shadowElevation = 3.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}

/**
 * Grupo conectado de acciones en una sola píldora (share + "..." de la referencia).
 * Contenedor M3 Expressive opaco: surfaceContainerHigh.
 */
@Composable
internal fun PlatformFloatingActionGroup(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        modifier = modifier.height(PlatformFloatingPillHeight),
        shape = PlatformFloatingGroupCorner,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 3.dp,
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/**
 * Contenedor de la search pill y de la title pill: mismo alto que los grupos
 * para la alineación global de la toolbar flotante.
 */
@Composable
internal fun PlatformFloatingSearchContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .height(PlatformFloatingPillHeight)
            .widthIn(min = 0.dp),
        shape = PlatformFloatingGroupCorner,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 3.dp,
        shadowElevation = 3.dp,
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}
