package com.luum.michi.app.mediaDetail.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.LanguageStrings
import com.luum.michi.app.mediaDetail.presentation.model.MediaDetail
import com.luum.michi.app.mediaDetail.presentation.model.MediaScoreBucket
import com.luum.michi.app.mediaDetail.presentation.model.MediaStatusBucket
import com.luum.michi.app.mediaDetail.presentation.model.MediaStatsStatus

@Composable
internal fun StatsTab(detail: MediaDetail, strings: LanguageStrings) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (detail.statusDistribution.isNotEmpty()) {
            item {
                MediaDetailSection(title = strings.mediaDetailStatusDistributionTitle) {
                    StatusDistributionList(buckets = detail.statusDistribution, strings = strings)
                }
            }
        }
        if (detail.scoreDistribution.isNotEmpty()) {
            item {
                MediaDetailSection(title = strings.mediaDetailScoreDistributionTitle) {
                    ScoreDistributionList(buckets = detail.scoreDistribution)
                }
            }
        }
    }
}

@Composable
internal fun StatusDistributionList(
    buckets: List<MediaStatusBucket>,
    strings: LanguageStrings,
) {
    val total = buckets.sumOf { it.amount }.coerceAtLeast(1)
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        buckets.forEach { bucket ->
            val pct = bucket.amount.toFloat() / total
            DistributionBar(
                label = statusLabel(bucket.status, strings),
                value = bucket.amount.toString(),
                fraction = pct,
                color = statusColor(bucket.status),
            )
        }
    }
}

@Composable
internal fun ScoreDistributionList(buckets: List<MediaScoreBucket>) {
    val max = buckets.maxOfOrNull { it.amount }?.coerceAtLeast(1) ?: 1
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        buckets.forEach { bucket ->
            val pct = bucket.amount.toFloat() / max
            DistributionBar(
                label = "${bucket.score}",
                value = bucket.amount.toString(),
                fraction = pct,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
internal fun DistributionBar(
    label: String,
    value: String,
    fraction: Float,
    color: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color),
            )
        }
    }
}

internal fun statusLabel(status: MediaStatsStatus, strings: LanguageStrings): String = when (status) {
    MediaStatsStatus.CURRENT -> strings.mediaStatsStatusCurrent
    MediaStatsStatus.PLANNING -> strings.mediaStatsStatusPlanning
    MediaStatsStatus.COMPLETED -> strings.mediaStatsStatusCompleted
    MediaStatsStatus.DROPPED -> strings.mediaStatsStatusDropped
    MediaStatsStatus.PAUSED -> strings.mediaStatsStatusPaused
    MediaStatsStatus.REPEATING -> strings.mediaStatsStatusRepeating
    MediaStatsStatus.OTHER -> ""
}

@Composable
internal fun statusColor(status: MediaStatsStatus): Color = when (status) {
    MediaStatsStatus.CURRENT -> MaterialTheme.colorScheme.primary
    MediaStatsStatus.PLANNING -> MaterialTheme.colorScheme.tertiary
    MediaStatsStatus.COMPLETED -> MaterialTheme.colorScheme.secondary
    MediaStatsStatus.DROPPED -> MaterialTheme.colorScheme.error
    MediaStatsStatus.PAUSED -> MaterialTheme.colorScheme.outline
    MediaStatsStatus.REPEATING -> MaterialTheme.colorScheme.primary
    MediaStatsStatus.OTHER -> MaterialTheme.colorScheme.outlineVariant
}
