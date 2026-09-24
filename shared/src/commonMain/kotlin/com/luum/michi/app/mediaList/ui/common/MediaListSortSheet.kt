package com.luum.michi.app.mediaList.ui.common

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.model.UserListOrder
import com.luum.michi.app.core.model.UserListSort
import com.luum.michi.app.core.model.label
import com.luum.michi.app.ui.components.ModalSheet
import com.luum.michi.app.ui.components.OptionGroup
import com.luum.michi.app.ui.components.OptionRow
import com.luum.michi.app.ui.language.Strings

/**
 * Shared list sort sheet: all [UserListSort] criteria single-select plus
 * the [UserListOrder] direction group. Primitives in, callbacks out — the
 * screen maps holder state so this file never touches a holder. Applies
 * immediately on every tap, like the Settings pickers; in-memory only
 * (persistence is a follow-up via UpdateUser).
 */
@Composable
internal fun MediaListSortSheet(
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
        Spacer(modifier = Modifier.height(16.dp))
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
    }
}
