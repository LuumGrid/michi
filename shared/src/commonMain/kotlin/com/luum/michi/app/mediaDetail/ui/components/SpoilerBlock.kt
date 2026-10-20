package com.luum.michi.app.mediaDetail.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.ui.components.PanelShape
import com.luum.michi.app.ui.components.flatBorder
import com.luum.michi.app.ui.icons.AppIcons

/**
 * A spoiler paragraph: a flat content panel (never glass) whose header toggles
 * the text. Closed, the spoiler text is not composed at all — not even
 * transparent. The header carries a Button role and the action label for
 * accessibility; the icon is decorative.
 */
@Composable
internal fun SpoilerBlock(
    text: String,
    isRevealed: Boolean,
    onToggle: () -> Unit,
    strings: LanguageStrings,
    modifier: Modifier = Modifier,
) {
    val action = if (isRevealed) {
        strings.mediaDetailSpoilerHideAction
    } else {
        strings.mediaDetailSpoilerShowAction
    }
    Surface(
        shape = PanelShape,
        color = MaterialTheme.colorScheme.surface,
        border = flatBorder(),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = action,
                        onClick = onToggle,
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (isRevealed) AppIcons.VisibilityOff else AppIcons.Visibility,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = action,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isRevealed) {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}
