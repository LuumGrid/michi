package com.luum.michi.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.ui.language.LanguageProvider
import com.luum.michi.app.ui.Icons

@Composable
fun DiscoverSection(
    title: String,
    onSeeAll: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val strings = LanguageProvider.strings
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (onSeeAll != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Row(
                    modifier = Modifier.clickable { onSeeAll() },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = strings.seeAllAction,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Icon(
                        painter = Icons.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        } else {
            Text(
                text = title,
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
        }
        content()
    }
}

@Composable
fun DiscoverPoster(
    paletteHex: String?,
    modifier: Modifier = Modifier,
    coverUrl: String? = null,
    contentDescription: String? = null,
    cornerRadius: Dp = 8.dp,
    fallbackIconSize: Dp = 28.dp,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    MediaCover(
        coverUrl = coverUrl,
        paletteHex = paletteHex,
        contentDescription = contentDescription,
        modifier = modifier,
        cornerRadius = cornerRadius,
        fallbackIcon = Icons.Discover,
        fallbackIconSize = fallbackIconSize,
        overlay = overlay,
    )
}
