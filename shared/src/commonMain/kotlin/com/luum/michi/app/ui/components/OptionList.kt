package com.luum.michi.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A framed [OptionGroup] of single-select [OptionRow]s: the block every picker
 * sheet repeats. [isSelected] defaults to equality, so a caller whose options
 * have no `equals` (e.g. the theme palette classes) can pass its own predicate.
 * [leading] paints a per-option glyph (a palette dot, a font sample).
 */
@Composable
internal fun <T> OptionList(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    title: String? = null,
    leading: (@Composable (T) -> Unit)? = null,
    modifier: Modifier = Modifier,
    isSelected: (T) -> Boolean = { it == selected },
) {
    OptionGroup(title = title, modifier = modifier) {
        options.forEachIndexed { index, option ->
            OptionRow(
                label = label(option),
                selected = isSelected(option),
                onClick = { onSelect(option) },
                divider = index > 0,
                leading = leading?.let { render -> { render(option) } },
            )
        }
    }
}
