package com.luum.michi.app.core.platform.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.LanguageProvider

/** Una opción seleccionable dentro de un grupo del filter sheet. */
internal data class PlatformFilterOption(
    val id: String,
    val label: String,
    /** Conteo opcional mostrado a la derecha (usado por Anime/Manga, no por Discover). */
    val count: Int? = null,
)

/**
 * Un grupo de opciones dentro del filter sheet.
 * - Si [title] es null, el grupo se muestra sin encabezado (caso Anime/Manga: un solo grupo plano).
 * - Si hay varios grupos con [title], se muestran apilados con su encabezado (caso Discover: Tipo/Género/Formato/Año).
 */
internal data class PlatformFilterGroup(
    val id: String,
    val title: String? = null,
    val options: List<PlatformFilterOption>,
    val selectedId: String,
)

/**
 * Bottom sheet de filtro genérico de selección única: reemplaza lo que antes eran
 * ShellSectionFilterSheet (Anime/Manga) y DiscoverFiltersSheet (Discover),
 * que eran casi el mismo composable duplicado dos veces.
 */
@Composable
internal fun PlatformFilterSheet(
    groups: List<PlatformFilterGroup>,
    onSelect: (groupId: String, optionId: String) -> Unit,
    onDismiss: () -> Unit,
    maxHeightFraction: Float = 0.86f,
) {
    val strings = LanguageProvider.strings

    PlatformModalSheet(
        onDismiss = onDismiss,
        maxHeightFraction = maxHeightFraction,
    ) { modifier ->
        Column(modifier = modifier) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 14.dp, start = 8.dp, end = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = strings.filterByLabel,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                groups.forEach { group ->
                    if (group.title != null) {
                        item(key = "header_${group.id}") {
                            Text(
                                text = group.title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    item(key = "options_${group.id}") {
                        PlatformChipRow(
                            options = group.options,
                            selectedIds = setOf(group.selectedId),
                            onToggle = { id -> onSelect(group.id, id) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}