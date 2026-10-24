package com.luum.michi.app.mediaDetail.ui.media.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.ui.components.GlassCircleButton
import com.luum.michi.app.ui.components.SheetTextField
import com.luum.michi.app.ui.icons.AppIcons
import com.luum.michi.app.ui.language.Strings

/**
 * Label + editable value with suffix and round −/+ steppers (progress,
 * score, repeat, priority): the AL-chan editor row. Every keystroke commits the
 * draft to the editor (callers ignore unparsable input and clamp), so the
 * editor state always holds the last valid value — pressing Save without Done
 * never saves a stale number. Losing focus drops the draft so the field shows
 * the real clamped value; Done does the same and closes the keyboard.
 */
@Composable
internal fun EditorCounterField(
    label: String,
    displayValue: String,
    suffix: String?,
    onCommit: (String) -> Unit,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    strings: LanguageStrings = Strings.current,
    modifier: Modifier = Modifier,
    decrementEnabled: Boolean = true,
    incrementEnabled: Boolean = true,
) {
    var draft by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    fun commitDraft() {
        draft = null
        focusManager.clearFocus()
    }
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SheetTextField(
                value = draft ?: displayValue,
                onValueChange = { value ->
                    draft = value
                    onCommit(value)
                },
                suffix = suffix,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { commitDraft() },
                ),
                modifier = Modifier.weight(1f),
                // On blur, drop the draft so the field shows the clamped truth.
                textFieldModifier = Modifier.onFocusChanged { focusState ->
                    if (!focusState.isFocused) draft = null
                },
            )
            Spacer(modifier = Modifier.width(8.dp))
            GlassCircleButton(
                icon = AppIcons.ExpandMore,
                contentDescription = strings.editorDecrementAction,
                onClick = {
                    draft = null
                    onDecrement()
                },
                enabled = decrementEnabled,
            )
            Spacer(modifier = Modifier.width(8.dp))
            GlassCircleButton(
                icon = AppIcons.ExpandLess,
                contentDescription = strings.editorIncrementAction,
                onClick = {
                    draft = null
                    onIncrement()
                },
                enabled = incrementEnabled,
            )
        }
    }
}
