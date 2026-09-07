package com.luum.michi.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.luum.michi.app.ui.language.LanguageProvider
import com.luum.michi.app.core.model.UserListSort
import com.luum.michi.app.core.model.UserListOrder

@Composable
internal fun SortSheet(
    currentSort: UserListSort,
    currentOrder: UserListOrder,
    persist: Boolean,
    isManga: Boolean,
    onDismiss: () -> Unit,
    onApply: (
        UserListSort,
        UserListOrder,
        Boolean
    ) -> Unit,
) {
    val strings = LanguageProvider.strings

    var selectedSort by remember(currentSort) { mutableStateOf(currentSort) }
    var selectedOrder by remember(currentOrder) { mutableStateOf(currentOrder) }
    var persistFilter by remember(persist) { mutableStateOf(persist) }

    ModalSheet(
        onDismiss = onDismiss,
        maxHeightFraction = 0.65f,
    ) { modifier ->
        Column(modifier = modifier) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 14.dp, start = 8.dp, end = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = strings.orderByLabel,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }

            // Main Content
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                // Persist on Restart Toggle: Styled exactly like edit entry's toggles!
                item {
                    SortGroupSection(title = strings.filterPersistLabel) {
                        BooleanRow(
                            label = strings.filterPersistLabel,
                            checked = persistFilter,
                            onCheckedChange = { persistFilter = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                        )
                    }
                }

                // Order Direction Selector
                item {
                    SortGroupSection(title = strings.filterOrderDirectionTitle) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                        ) {
                        UserListOrder.entries.forEach { order ->
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
                }

                // Sorting Criteria Selector (Grouped inside a Column to avoid the LazyColumn verticalArrangement spacedBy padding)
                item {
                    SortGroupSection(title = strings.filterSortCriterionTitle) {
                        val sortOptions = remember(isManga) {
                            UserListSort.entries.map { sort ->
                                val label = if (sort == UserListSort.PROGRESS && isManga) {
                                    strings.sortProgressManga
                                } else {
                                    sort.label(strings)
                                }
                                FilterOption(id = sort.name, label = label)
                            }
                        }

                            ChipRow(
                                options = sortOptions,
                                selectedIds = setOf(selectedSort.name),
                                onToggle = { id -> UserListSort.entries.firstOrNull { it.name == id }?.let { selectedSort = it } },
                                modifier = Modifier.fillMaxWidth(),
                            )
                    }
                }
            }

            // Bottom Buttons Bar: acción compartida de dos botones.
            SheetActionBar(
                leadingLabel = strings.filterResetAction,
                trailingLabel = strings.filterSaveAction,
                onLeadingClick = {
                    onApply(UserListSort.FOLLOW_LIST, UserListOrder.DESCENDING, false)
                },
                onTrailingClick = {
                    onApply(selectedSort, selectedOrder, persistFilter)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SortGroupSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 20.dp),
            )
        Spacer(modifier = Modifier.height(6.dp))
        content()
    }
}
