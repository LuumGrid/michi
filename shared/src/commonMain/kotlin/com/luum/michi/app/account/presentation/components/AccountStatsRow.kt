package com.luum.michi.app.account.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luum.michi.app.account.presentation.model.AccountStats
import com.luum.michi.app.account.presentation.model.toCompactCountLabel
import com.luum.michi.app.core.language.LanguageProvider

@Composable
internal fun AccountStatsRow(
    stats: AccountStats,
    onAnimeClick: () -> Unit = {},
    onMangaClick: () -> Unit = {},
) {
    val strings = LanguageProvider.strings

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AccountStatCell(
            value = stats.animeCount.toCompactCountLabel(),
            label = strings.accountAnimeLabel,
            onClick = onAnimeClick,
        )
        AccountStatCell(
            value = stats.mangaCount.toCompactCountLabel(),
            label = strings.accountMangaLabel,
            onClick = onMangaClick,
        )
        AccountStatCell(
            value = stats.followingCount.toCompactCountLabel(),
            label = strings.accountFollowingLabel,
        )
        AccountStatCell(
            value = stats.followersCount.toCompactCountLabel(),
            label = strings.accountFollowersLabel,
        )
    }
}

@Composable
private fun AccountStatCell(
    value: String,
    label: String,
    onClick: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val modifier = if (onClick != null) {
        Modifier.clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    } else {
        Modifier
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (onClick != null) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
