package com.luum.michi.app.core.platform.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
 * Sheet de Order + filtros onList para las superficies de Discover (Explore, Seasonal).
 * El sort field se maneja ahora vía un PlatformFilterChip en el caller.
 * Las etiquetas llegan ya localizadas desde el caller.
 */
@Composable
fun DiscoverFilterSheet(
    title: String,
    orderTitle: String,
    ascendingLabel: String,
    descendingLabel: String,
    currentDescending: Boolean,
    showOnListFilters: Boolean,
    hideOnListLabel: String,
    onlyOnListLabel: String,
    currentOnList: Boolean?,
    applyLabel: String,
    onDismiss: () -> Unit,
    onApply: (descending: Boolean, onList: Boolean?) -> Unit,
) {
    var descending by remember(currentDescending) { mutableStateOf(currentDescending) }
    var hideOnList by remember(currentOnList) { mutableStateOf(currentOnList == false) }
    var onlyOnList by remember(currentOnList) { mutableStateOf(currentOnList == true) }

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

                    // onList checkboxes — only for ANIME/MANGA browse
                    if (showOnListFilters) {
                        item {
                            PlatformBooleanRow(
                                label = hideOnListLabel,
                                checked = hideOnList,
                                onCheckedChange = { checked ->
                                    hideOnList = checked
                                    if (checked) onlyOnList = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                            )
                        }
                        item {
                            PlatformBooleanRow(
                                label = onlyOnListLabel,
                                checked = onlyOnList,
                                onCheckedChange = { checked ->
                                    onlyOnList = checked
                                    if (checked) hideOnList = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
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
                        onClick = {
                            val onList = when {
                                onlyOnList -> true
                                hideOnList -> false
                                else -> null
                            }
                            onApply(descending, onList)
                        },
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
