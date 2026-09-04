package com.luum.michi.app.discover.presentation.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.language.LanguageStrings
import com.luum.michi.app.discover.presentation.state.DiscoverSortSelection

internal fun DiscoverSortSelection.label(strings: LanguageStrings): String = when (this) {
    DiscoverSortSelection.BEST_MATCH -> strings.sortBestMatch
    DiscoverSortSelection.MOST_POPULAR -> strings.sortMostPopular
    DiscoverSortSelection.LEAST_POPULAR -> strings.sortLeastPopular
    DiscoverSortSelection.HIGHEST_SCORED -> strings.sortHighestScored
    DiscoverSortSelection.LOWEST_SCORED -> strings.sortLowestScored
}

/**
 * Sort in-app de Discover como dropdown anclado al botón: ordena lo cargado
 * por popularidad (favoritos) o puntaje, asc/desc. Best match = orden del API.
 */
@Composable
internal fun DiscoverSortDropdown(
    expanded: Boolean,
    selected: DiscoverSortSelection,
    onSelect: (DiscoverSortSelection) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LanguageProvider.strings

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 3.dp,
        tonalElevation = 3.dp,
    ) {
        DiscoverSortSelection.entries.forEach { option ->
            val isSelected = option == selected
            DropdownMenuItem(
                text = {
                    Text(
                        text = option.label(strings),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                },
                leadingIcon = {
                    RadioButton(
                        selected = isSelected,
                        onClick = null,
                        modifier = Modifier.size(32.dp),
                    )
                },
                onClick = { onSelect(option) },
            )
        }
    }
}
