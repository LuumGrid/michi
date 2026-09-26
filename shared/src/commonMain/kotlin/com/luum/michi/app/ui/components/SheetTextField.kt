package com.luum.michi.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luum.michi.app.ui.theme.SurfaceStyle
import com.luum.michi.app.ui.theme.SurfaceStyleProvider

/**
 * Sheet text input with both surface recipes inline: same metrics,
 * decoration and behavior in both branches — only the container recipe
 * varies (translucent + glass border + shadow vs opaque + outline).
 * Callers (editor counters, notes) never branch themselves.
 */
@Composable
internal fun SheetTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    hint: String? = null,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val glass = SurfaceStyleProvider.current == SurfaceStyle.GLASS
    Surface(
        shape = GlassShape,
        color = if (glass) glassContainerColor() else solidContainerColor(),
        border = if (glass) glassBorder() else solidBorder(),
        modifier = if (glass) modifier.glass(GlassShape) else modifier,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            enabled = enabled,
            singleLine = minLines == 1 && maxLines == 1,
            minLines = minLines,
            maxLines = maxLines,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Start,
            ),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (value.isEmpty() && hint != null) {
                            Text(
                                text = hint,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                        innerTextField()
                    }
                    if (suffix != null) {
                        Text(
                            text = suffix,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
        )
    }
}
