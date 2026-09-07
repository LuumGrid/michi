package com.luum.michi.app.mediaDetail.presentation.media.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.LanguageStrings
import com.luum.michi.app.ui.components.ListMessage
import com.luum.michi.app.ui.components.ListMessageTone
import com.luum.michi.app.mediaDetail.domain.media.model.MediaStaffEntry
import com.luum.michi.app.mediaDetail.presentation.media.state.MediaDetailStateHolder

@Composable
internal fun StaffTab(
    stateHolder: MediaDetailStateHolder,
    strings: LanguageStrings,
    onOpenStaff: (Int) -> Unit = {},
) {
    val staff = stateHolder.staff
    if (staff.isEmpty() && !stateHolder.isLoadingStaff) {
        ListMessage(
            title = strings.mediaDetailNoStaffLabel,
            tone = ListMessageTone.Neutral,
        )
        return
    }
    val gridState = rememberLazyGridState()
    val nearEnd by remember {
        derivedStateOf {
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= staff.size - 4
        }
    }
    LaunchedEffect(nearEnd, stateHolder.staffHasNextPage) {
        if (nearEnd && stateHolder.staffHasNextPage && !stateHolder.isLoadingStaff) {
            stateHolder.loadMoreStaff()
        }
    }
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(1),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        gridItems(items = staff, key = { it.edgeKey }) { entry ->
            StaffCard(entry = entry, onOpenStaff = onOpenStaff)
        }
        if (stateHolder.isLoadingStaff) {
            item { LoadingTile() }
            item { LoadingTile() }
        }
    }
}

@Composable
internal fun StaffCard(
    entry: MediaStaffEntry,
    onOpenStaff: (Int) -> Unit = {},
) {
    ConnectionRowCard(
        coverUrl = entry.imageUrl,
        paletteHex = null,
        title = entry.name,
        averageScore = null,
        favourites = null,
        topLine = entry.role,
        metaParts = emptyList(),
        onClick = { onOpenStaff(entry.staffId) },
    )
}
