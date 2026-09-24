package com.luum.michi.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.luum.michi.app.ui.icons.AppIcons

/**
 * Expanded in-context search field: pure text input with an inline clear-X.
 * The back (chevron) affordance lives OUTSIDE, as a floating glass circle
 * composed by [Toolbar]. The X sits bare inside the field (no circle) and
 * only with text to clear — standard behavior that also frees 56dp.
 *
 * [BasicTextField] on purpose: M3's [TextField][androidx.compose.material3.TextField]
 * carries a 56dp minimum plus internal padding that sits the text higher
 * than the 48dp tools (and forcing 48dp clips it — it has no
 * contentPadding override). The basic field takes the exact 48dp,
 * centered, with zero clipping at any font scale.
 *
 * Takes only primitives so no feature model leaks into shared UI.
 */
@Composable
internal fun SearchField(
    query: String,
    hint: String,
    autoFocus: Boolean,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    clearDescription: String,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(autoFocus) {
        if (autoFocus) focusRequester.requestFocus()
    }
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .height(48.dp)
            .focusRequester(focusRequester),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            // innerTextField fills the width (not wrap content): edge drags
            // stay inside the text layout, so the native edge auto-scroll
            // wakes up on long queries. Zero visual change.
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    innerTextField()
                }
                if (query.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { onQueryChange("") },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = AppIcons.Clear,
                            contentDescription = clearDescription,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
    )
}
