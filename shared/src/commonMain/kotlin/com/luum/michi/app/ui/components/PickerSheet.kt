package com.luum.michi.app.ui.components

import androidx.compose.runtime.Composable

/**
 * A modal sheet holding a single [OptionList]: the one-list picker (language,
 * font, theme style, and so on). Every option tap runs [onSelect] exactly as the
 * caller wants (it does not close the sheet on its own).
 */
@Composable
internal fun <T> PickerSheet(
    title: String,
    dismissLabel: String,
    onDismiss: () -> Unit,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    leading: (@Composable (T) -> Unit)? = null,
    isSelected: (T) -> Boolean = { it == selected },
) {
    ModalSheet(
        title = title,
        dismissLabel = dismissLabel,
        onDismiss = onDismiss,
    ) {
        OptionList(
            options = options,
            selected = selected,
            label = label,
            onSelect = onSelect,
            leading = leading,
            isSelected = isSelected,
        )
    }
}
