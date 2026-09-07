package com.luum.michi.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip as MaterialFilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Una opción seleccionable dentro de una fila de chips. */
internal data class FilterOption(
    val id: String,
    val label: String,
    /** Conteo opcional mostrado entre paréntesis (usado por Anime/Manga). */
    val count: Int? = null,
)

/**
 * Fila horizontal de altura fija con [FilterChip] de Material3 y sombra de
 * overflow en ambos bordes (replica `ShadowedOverflowList` + `ChipSelector` /
 * `ChipMultiSelector` de Otraku). Con [multiSelect] en true cada tap alterna
 * esa opción sin afectar las demás; en false el tap reemplaza la selección.
 */@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChipRow(
    options: List<FilterOption>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
    multiSelect: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp),
) {
    Box(modifier = modifier.height(40.dp)) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = contentPadding,
            modifier = Modifier.height(40.dp),
        ) {
            items(options, key = { it.id }) { option ->
                val selected = option.id in selectedIds
                MaterialFilterChip(
                    selected = selected,
                    onClick = {
                        if (multiSelect || !selected) onToggle(option.id)
                    },
                    label = {
                        Text(if (option.count != null) "${option.label} (${option.count})" else option.label)
                    },
                )
            }
        }
        val surface = MaterialTheme.colorScheme.surface
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(24.dp)
                .fillMaxHeight()
                .background(Brush.horizontalGradient(listOf(surface, Color.Transparent))),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(24.dp)
                .fillMaxHeight()
                .background(Brush.horizontalGradient(listOf(Color.Transparent, surface))),
        )
    }
}
