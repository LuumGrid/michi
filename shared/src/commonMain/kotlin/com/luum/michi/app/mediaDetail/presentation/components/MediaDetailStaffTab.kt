package com.luum.michi.app.mediaDetail.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.LanguageStrings
import com.luum.michi.app.core.platform.components.PlatformListMessage
import com.luum.michi.app.core.platform.components.PlatformListMessageTone
import com.luum.michi.app.mediaDetail.presentation.model.MediaStaffEntry
import com.luum.michi.app.mediaDetail.presentation.state.MediaDetailStateHolder

@Composable
internal fun StaffTab(
    stateHolder: MediaDetailStateHolder,
    strings: LanguageStrings,
) {
    val staff = stateHolder.staff
    if (staff.isEmpty() && !stateHolder.isLoadingStaff) {
        PlatformListMessage(
            title = strings.mediaDetailNoStaffLabel,
            tone = PlatformListMessageTone.Neutral,
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
            StaffCard(entry = entry)
        }
        if (stateHolder.isLoadingStaff) {
            item { LoadingTile() }
            item { LoadingTile() }
        }
    }
}

@Composable
internal fun StaffCard(entry: MediaStaffEntry) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        PersonHalf(
            imageUrl = entry.imageUrl,
            name = entry.name,
            subtitle = entry.role.orEmpty(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
