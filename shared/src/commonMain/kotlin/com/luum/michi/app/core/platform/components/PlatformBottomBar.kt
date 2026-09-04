package com.luum.michi.app.core.platform.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Estilo compartido de la barra inferior (tab bar + search): misma píldora negra
 * sólida en ambos temas, sin estar unificados en un solo contenedor.
 */
internal object PlatformBottomBarDefaults {
    val ContainerColor: Color = Color(0xFF141414)
    val ContentColor: Color = Color.White
    val HintColor: Color = Color(0xFF9E9E9E)
    val Shape: Shape = CircleShape
    val PillHeight: Dp = 64.dp
}

/**
 * Círculo aislado inferior con el estilo de la tab bar (search). Misma altura que
 * la tab bar; se pega a ella sin separación para que la tab bar respire.
 */
@Composable
internal fun PlatformBottomBarCircle(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(PlatformBottomBarDefaults.PillHeight),
        shape = CircleShape,
        color = PlatformBottomBarDefaults.ContainerColor,
        contentColor = PlatformBottomBarDefaults.ContentColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}
