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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.luum.michi.app.ui.components.GlassCircleButton
import com.luum.michi.app.ui.components.GlassTextField
import com.luum.michi.app.ui.icons.AppIcons

/**
 * Label + editable value with suffix and round −/+ steppers (progress,
 * score, repeat, priority): the AL-chan editor row. The draft lives here —
 * Done commits (or reverts on unparsable input) and steppers clear it, so
 * the field always returns to the truth.
 */
@Composable
internal fun EditorCounterField(
    label: String,
    displayValue: String,
    suffix: String?,
    onCommit: (String) -> Unit,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
    decrementEnabled: Boolean = true,
    incrementEnabled: Boolean = true,
) {
    var draft by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    fun commitDraft() {
        draft?.let(onCommit)
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
            GlassTextField(
                value = draft ?: displayValue,
                onValueChange = { draft = it },
                suffix = suffix,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { commitDraft() },
                ),
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            GlassCircleButton(
                icon = AppIcons.ExpandMore,
                contentDescription = null,
                onClick = {
                    draft = null
                    onDecrement()
                },
                enabled = decrementEnabled,
            )
            Spacer(modifier = Modifier.width(8.dp))
            GlassCircleButton(
                icon = AppIcons.ExpandLess,
                contentDescription = null,
                onClick = {
                    draft = null
                    onIncrement()
                },
                enabled = incrementEnabled,
            )
        }
    }
}
