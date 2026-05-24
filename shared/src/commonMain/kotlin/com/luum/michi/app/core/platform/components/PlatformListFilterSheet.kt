package com.luum.michi.app.core.platform.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.model.UserListSort
import com.luum.michi.app.core.platform.model.UserListOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlatformListFilterSheet(
    currentSort: UserListSort,
    currentOrder: UserListOrder,
    persist: Boolean,
    isManga: Boolean,
    onDismiss: () -> Unit,
    onApply: (UserListSort, UserListOrder, Boolean) -> Unit,
) {
    val strings = LanguageProvider.strings

    var selectedSort by remember { mutableStateOf(currentSort) }
    var selectedOrder by remember { mutableStateOf(currentOrder) }
    var persistFilter by remember { mutableStateOf(persist) }

    PlatformModalSheet(
        onDismiss = onDismiss,
        maxHeightFraction = 0.86f,
    ) { modifier ->
        Column(
            modifier = modifier
        ) {
            // Header: Centered like EditorHeader
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 14.dp, start = 8.dp, end = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = strings.filterSheetTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }

            // Main Content
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    // Persist on Restart Toggle: Styled exactly like edit entry's toggles!
                    item {
                        PlatformBooleanRow(
                            label = strings.filterPersistLabel,
                            checked = persistFilter,
                            onCheckedChange = { persistFilter = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                        )
                    }

                    // Order Direction Selector
                    item {
                        Text(
                            text = strings.filterOrderDirectionTitle,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            UserListOrder.values().forEach { order ->
                                val isSelected = selectedOrder == order
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { selectedOrder = order },
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(vertical = 10.dp),
                                    ) {
                                        Text(
                                            text = order.label(strings),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Sort Criteria Selector (Grouped inside a Column to avoid the LazyColumn verticalArrangement spacedBy padding)
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = strings.filterSortCriterionTitle,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
                            )

                            UserListSort.values().forEach { sort ->
                                val rawLabel = sort.label(strings)
                                val displayLabel = if (sort == UserListSort.PROGRESS && isManga) {
                                    strings.sortProgressManga
                                } else {
                                    rawLabel
                                }

                                val isSelected = selectedSort == sort
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedSort = sort }
                                        .padding(vertical = 2.dp, horizontal = 6.dp),
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedSort = sort },
                                        modifier = Modifier.size(36.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = displayLabel,
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

            // Bottom Buttons Bar with same surface container styles as EditorActionBar
            Surface(
                modifier = Modifier.fillMaxWidth(),
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
                    OutlinedButton(
                        onClick = {
                            onApply(UserListSort.FOLLOW_LIST, UserListOrder.DESCENDING, false)
                        },
                        shape = RoundedCornerShape(24.dp), // Styled exactly like Editor cancel button!
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    ) {
                        Text(
                            text = strings.filterResetAction,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Button(
                        onClick = { onApply(selectedSort, selectedOrder, persistFilter) },
                        shape = RoundedCornerShape(24.dp), // Styled exactly like Editor save button!
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    ) {
                        Text(
                            text = strings.filterSaveAction,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}
