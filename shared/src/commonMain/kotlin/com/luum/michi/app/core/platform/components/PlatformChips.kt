package com.luum.michi.app.core.platform.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val ActiveChipColor = Color(0xFFFAF7EF)
private val ActiveChipLabelColor = Color(0xFF1F1B16)

@Composable
fun <T> PlatformChips(
    items: List<T>,
    selectedItem: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    contentColor: Color = Color.Unspecified,
    useSoftActiveColor: Boolean = false,
) {
    val listState = rememberLazyListState()
    val selectedIndex = items.indexOf(selectedItem).coerceAtLeast(0)
    val color = if (contentColor == Color.Unspecified) {
        MaterialTheme.colorScheme.onSurface
    } else {
        contentColor
    }

    LaunchedEffect(selectedIndex) {
        listState.animateScrollToItem(selectedIndex)
    }

    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        itemsIndexed(items) { _, item ->
            val selected = selectedItem == item
            FilterChip(
                selected = selected,
                onClick = { onSelect(item) },
                label = {
                    Text(
                        text = label(item),
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = if (useSoftActiveColor) {
                        ActiveChipColor
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    selectedLabelColor = if (useSoftActiveColor) {
                        ActiveChipLabelColor
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    },
                    containerColor = Color.Transparent,
                    labelColor = color.copy(alpha = 0.7f),
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = if (useSoftActiveColor) {
                        ActiveChipColor.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                ),
            )
        }
    }
}
