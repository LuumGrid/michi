package com.luum.michi.app.core.platform.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Un criterio de orden de AniList (campo base + etiqueta localizada). */
data class DiscoverSortField(val field: String, val label: String)

/** Parte el enum de AniList (p.ej. "POPULARITY_DESC" / "POPULARITY") en campo + descendente. */
fun parseDiscoverSort(sort: String): Pair<String, Boolean> {
    val descending = sort.endsWith("_DESC")
    val field = if (descending) sort.removeSuffix("_DESC") else sort
    return field to descending
}

/** Combina campo + dirección en el enum de AniList. */
fun combineDiscoverSort(field: String, descending: Boolean): String =
    if (descending) "${field}_DESC" else field

/**
 * Sheet de Sort + Order para las superficies de Discover (Explore, Seasonal), análogo al
 * [PlatformListFilterSheet] de las listas. Las etiquetas llegan ya localizadas desde el caller.
 */
@Composable
fun DiscoverSortSheet(
    title: String,
    sortTitle: String,
    orderTitle: String,
    fields: List<DiscoverSortField>,
    currentSort: String,
    ascendingLabel: String,
    descendingLabel: String,
    applyLabel: String,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit,
) {
    val (initialField, initialDescending) = parseDiscoverSort(currentSort)
    var selectedField by remember(currentSort) {
        mutableStateOf(fields.firstOrNull { it.field == initialField }?.field ?: fields.first().field)
    }
    var descending by remember(currentSort) { mutableStateOf(initialDescending) }

    PlatformModalSheet(
        onDismiss = onDismiss,
        maxHeightFraction = 0.86f,
    ) { modifier ->
        Column(modifier = modifier) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 14.dp, start = 8.dp, end = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    // Order direction selector
                    item {
                        Text(
                            text = orderTitle,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            listOf(true to descendingLabel, false to ascendingLabel).forEach { (desc, label) ->
                                val isSelected = descending == desc
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { descending = desc },
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(vertical = 10.dp),
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Sort criterion selector
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = sortTitle,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
                            )
                            fields.forEach { sortField ->
                                val isSelected = selectedField == sortField.field
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedField = sortField.field }
                                        .padding(vertical = 2.dp, horizontal = 6.dp),
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedField = sortField.field },
                                        modifier = Modifier.size(36.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = sortField.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 0.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                ) {
                    Button(
                        onClick = { onApply(combineDiscoverSort(selectedField, descending)) },
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    ) {
                        Text(
                            text = applyLabel,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}
