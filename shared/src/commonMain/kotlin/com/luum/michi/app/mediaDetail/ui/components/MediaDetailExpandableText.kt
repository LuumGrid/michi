package com.luum.michi.app.mediaDetail.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.ui.components.SectionHeader

/**
 * Collapsible long-text section shared by every detail scope (media
 * synopsis, character/staff biography). Collapsed to five lines; the
 * read more/less link only appears when the collapsed text actually
 * overflows (or while expanded, so it can always be folded back).
 */
@Composable
internal fun MediaDetailExpandableText(
    title: String,
    text: String,
    strings: LanguageStrings,
    modifier: Modifier = Modifier,
    // Baked 16dp matches the full-width layout; panels (already inset) pass
    // zero to avoid double padding.
    horizontalPadding: Dp = 16.dp,
) {
    var expanded by remember(text) { mutableStateOf(false) }
    var overflows by remember(text) { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
    ) {
        SectionHeader(title)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = if (expanded) Int.MAX_VALUE else COLLAPSED_LINES,
            overflow = TextOverflow.Ellipsis,
            // Measured only while collapsed: once expanded there is no
            // overflow to report, and the link stays via [expanded].
            onTextLayout = { result ->
                if (!expanded) overflows = result.hasVisualOverflow
            },
        )
        if (expanded || overflows) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (expanded) strings.mediaDetailReadLessAction else strings.mediaDetailReadMoreAction,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp),
            )
        }
    }
}

private const val COLLAPSED_LINES = 5
