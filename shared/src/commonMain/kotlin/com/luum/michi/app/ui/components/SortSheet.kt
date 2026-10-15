package com.luum.michi.app.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.model.UserListOrder
import com.luum.michi.app.core.model.UserListSort
import com.luum.michi.app.core.model.label
import com.luum.michi.app.ui.language.Strings

/**
 * Shared sort sheet: all [UserListSort] criteria single-select plus the
 * [UserListOrder] direction group. Primitives in, callbacks out — the screen
 * maps holder state so this file never touches a holder. Applies immediately on
 * every tap, like the Settings pickers. Used by the anime/manga lists and by the
 * studio media grid (which re-queries on a change instead of sorting locally).
 */
@Composable
internal fun SortSheet(
    options: List<UserListSort>,
    selected: UserListSort,
    onSelect: (UserListSort) -> Unit,
    order: UserListOrder,
    onSelectOrder: (UserListOrder) -> Unit,
    onDismiss: () -> Unit,
    strings: LanguageStrings = Strings.current,
    modifier: Modifier = Modifier,
) {
    ModalSheet(
        title = strings.sortAction,
        dismissLabel = strings.dismissAction,
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        // Direction first: it applies to whichever criterion is picked
        // below, so it reads as the primary switch.
        OptionGroup(title = strings.filterOrderDirectionTitle) {
            UserListOrder.entries.forEachIndexed { index, entry ->
                OptionRow(
                    label = entry.label(strings),
                    selected = entry == order,
                    onClick = { onSelectOrder(entry) },
                    divider = index != 0,
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        OptionGroup(title = strings.filterSortCriterionTitle) {
            options.forEachIndexed { index, option ->
                OptionRow(
                    label = option.label(strings),
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    divider = index != 0,
                )
            }
        }
    }
}
