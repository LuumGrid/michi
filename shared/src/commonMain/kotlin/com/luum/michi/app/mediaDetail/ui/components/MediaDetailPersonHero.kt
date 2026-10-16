package com.luum.michi.app.mediaDetail.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.model.CoverSizes
import com.luum.michi.app.core.model.compactCount
import com.luum.michi.app.ui.components.CoverSize
import com.luum.michi.app.ui.components.FlatChip
import com.luum.michi.app.ui.components.coverSize
import com.luum.michi.app.ui.icons.AppIcons
import com.luum.michi.app.ui.rememberCoverUrl

/**
 * Shared person hero (character/staff/studio). With a portrait it is a row —
 * the cover (the [CoverSize.Featured] token) on the left, the identity column
 * (name, native name, favourites, chips) filling the rest — so the page stops
 * wasting the side air a centered portrait left. A studio has no portrait: it
 * passes [showPortrait] false and the identity goes full width. Neutral on
 * purpose, so every scope reuses the same shape without importing each other.
 *
 * [chips] are the scope's own labels (character roles, staff occupations): they
 * wrap in a [FlowRow] of flat chips and grow the hero instead of being clipped.
 */
@Composable
internal fun MediaDetailPersonHero(
    name: String,
    nativeName: String?,
    image: CoverSizes?,
    favourites: Int?,
    strings: LanguageStrings,
    modifier: Modifier = Modifier,
    showPortrait: Boolean = true,
    chips: List<String> = emptyList(),
) {
    if (showPortrait) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            PersonPortrait(image)
            Spacer(modifier = Modifier.width(16.dp))
            HeroIdentity(
                name = name,
                nativeName = nativeName,
                favourites = favourites,
                strings = strings,
                chips = chips,
                // The identity is at least as tall as the cover; a long chip
                // list grows the hero instead of clipping the chips.
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = CoverSize.Featured.height),
            )
        }
    } else {
        HeroIdentity(
            name = name,
            nativeName = nativeName,
            favourites = favourites,
            strings = strings,
            chips = chips,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun PersonPortrait(image: CoverSizes?) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.coverSize(CoverSize.Featured),
    ) {
        val url = rememberCoverUrl(image, CoverSize.Featured)
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroIdentity(
    name: String,
    nativeName: String?,
    favourites: Int?,
    strings: LanguageStrings,
    chips: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Text(
            text = name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        nativeName?.takeIf { it.isNotBlank() }?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        favourites?.let {
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = AppIcons.Favorite,
                    contentDescription = strings.favoritesLabel,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = it.compactCount(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (chips.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                chips.forEach { chip -> FlatChip(label = chip) }
            }
        }
    }
}
