package com.luum.michi.app.mediaDetail.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.ui.components.glassBorder
import com.luum.michi.app.ui.components.solidBorder
import com.luum.michi.app.ui.theme.SurfaceStyle
import com.luum.michi.app.ui.theme.SurfaceStyleProvider
import com.luum.michi.app.core.model.CoverSizes
import com.luum.michi.app.ui.rememberCoverUrl

/**
 * Shared person hero (character/staff): centered portrait + name + native
 * name + favourites. Neutral on purpose so both scopes reuse the same shape
 * without importing each other.
 */
@Composable
internal fun MediaDetailPersonHero(
    name: String,
    nativeName: String?,
    image: CoverSizes?,
    favourites: Int?,
    strings: LanguageStrings,
    modifier: Modifier = Modifier,
) {
    val glass = SurfaceStyleProvider.current == SurfaceStyle.GLASS
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = if (glass) glassBorder() else solidBorder(),
            modifier = Modifier
                .width(140.dp)
                .aspectRatio(3f / 4f),
        ) {
            val url = rememberCoverUrl(image, 140.dp)
            if (!url.isNullOrBlank()) {
                SubcomposeAsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    loading = {},
                    error = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        nativeName?.takeIf { it.isNotBlank() }?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        favourites?.let {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$it ${strings.favoritesLabel}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
