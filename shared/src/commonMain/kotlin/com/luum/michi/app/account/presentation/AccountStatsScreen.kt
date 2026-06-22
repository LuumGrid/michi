package com.luum.michi.app.account.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luum.michi.app.account.presentation.components.AccountStatBarChart
import com.luum.michi.app.account.presentation.model.AccountMediaTypeStats
import com.luum.michi.app.account.presentation.model.AccountStats
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.components.bottomNavBarClearance
import kotlin.math.round

private enum class AccountStatsTab { ANIME, MANGA }

@Composable
internal fun AccountStatsScreen(stats: AccountStats) {
    val strings = LanguageProvider.strings
    var selectedTab by remember { mutableStateOf(AccountStatsTab.ANIME) }
    val mediaStats = if (selectedTab == AccountStatsTab.ANIME) stats.anime else stats.manga
    val isManga = selectedTab == AccountStatsTab.MANGA

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = bottomNavBarClearance(),
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AccountStatsTabButton(
                    label = strings.accountAnimeLabel,
                    isSelected = selectedTab == AccountStatsTab.ANIME,
                    onClick = { selectedTab = AccountStatsTab.ANIME },
                    modifier = Modifier.weight(1f),
                )
                AccountStatsTabButton(
                    label = strings.accountMangaLabel,
                    isSelected = selectedTab == AccountStatsTab.MANGA,
                    onClick = { selectedTab = AccountStatsTab.MANGA },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            AccountStatsOverview(stats = mediaStats, isManga = isManga)
        }

        if (mediaStats.scoreDistribution.isNotEmpty()) {
            item {
                AccountStatsSection(title = strings.accountStatsScoreDistributionTitle) {
                    AccountStatBarChart(entries = mediaStats.scoreDistribution)
                }
            }
        }

        if (mediaStats.formatDistribution.isNotEmpty()) {
            item {
                AccountStatsSection(title = strings.accountStatsFormatDistributionTitle) {
                    AccountStatBarChart(entries = mediaStats.formatDistribution)
                }
            }
        }

        if (mediaStats.statusDistribution.isNotEmpty()) {
            item {
                AccountStatsSection(title = strings.accountStatsStatusDistributionTitle) {
                    AccountStatBarChart(entries = mediaStats.statusDistribution)
                }
            }
        }

        if (mediaStats.topGenres.isNotEmpty()) {
            item {
                AccountStatsSection(title = strings.accountStatsTopGenresTitle) {
                    AccountStatBarChart(entries = mediaStats.topGenres)
                }
            }
        }

        if (mediaStats.count == 0) {
            item {
                Text(
                    text = strings.accountStatsEmptyLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AccountStatsTabButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
            contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        ),
    ) {
        Text(text = label, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AccountStatsOverview(stats: AccountMediaTypeStats, isManga: Boolean) {
    val strings = LanguageProvider.strings

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = strings.accountStatsOverviewLabel,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AccountStatsOverviewCard(
                label = if (isManga) strings.accountStatsChaptersReadLabel else strings.accountStatsEpisodesWatchedLabel,
                value = stats.episodesOrChapters.toString(),
                modifier = Modifier.weight(1f),
            )
            AccountStatsOverviewCard(
                label = if (isManga) strings.accountStatsVolumesReadLabel else strings.accountStatsDaysWatchedLabel,
                value = stats.daysOrVolumes.toOneDecimalLabel(),
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AccountStatsOverviewCard(
                label = strings.accountStatsMeanScoreLabel,
                value = stats.meanScore.toOneDecimalLabel(),
                modifier = Modifier.weight(1f),
            )
            AccountStatsOverviewCard(
                label = strings.accountStatsStandardDeviationLabel,
                value = stats.standardDeviation.toOneDecimalLabel(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AccountStatsOverviewCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 12.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AccountStatsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        content()
    }
}

private fun Double.toOneDecimalLabel(): String {
    val rounded = round(this * 10) / 10.0
    return if (rounded == rounded.toLong().toDouble()) {
        "${rounded.toLong()}.0"
    } else {
        rounded.toString()
    }
}
