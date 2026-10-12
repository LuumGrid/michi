package com.luum.michi.app.mediaDetail.ui.media.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetail
import com.luum.michi.app.mediaDetail.domain.media.model.label
import com.luum.michi.app.mediaDetail.ui.components.MediaDetailSectionHeader
import com.luum.michi.app.ui.components.GlassShape

/**
 * List-status distribution as labeled bars, plus a total row. Replaces the
 * old raw status bars: each row reads `status label · count` with a bar
 * scaled to the largest bucket.
 */
@Composable
internal fun MediaDetailSummaryStats(
    detail: MediaDetail,
    strings: LanguageStrings,
    isManga: Boolean,
) {
    val rows = detail.statusDistribution
    if (rows.isEmpty()) return
    val total = rows.sumOf { it.amount }
    val max = rows.maxOfOrNull { it.amount }?.coerceAtLeast(1) ?: 1
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        MediaDetailSectionHeader(strings.mediaDetailSummaryStatsTitle)
        Spacer(modifier = Modifier.height(8.dp))
        rows.forEach { bucket ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = bucket.status.label(strings, isManga),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(88.dp),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(bucket.amount.toFloat() / max)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = compactCount(bucket.amount),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.width(48.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = strings.mediaDetailTotalLabel,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = compactCount(total),
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

/**
 * Vertical score-distribution chart: one column per score bucket scaled to
 * the tallest, score labels underneath, and a peak badge (`score · amount`).
 */
@Composable
internal fun MediaDetailScoreDistribution(detail: MediaDetail, strings: LanguageStrings) {
    val buckets = detail.scoreDistribution
    if (buckets.isEmpty()) return
    val max = buckets.maxOfOrNull { it.amount }?.coerceAtLeast(1) ?: 1
    val peak = buckets.maxByOrNull { it.amount }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        MediaDetailSectionHeader(strings.mediaDetailScoreDistributionTitle)
        if (peak != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = GlassShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Text(
                    text = "${peak.score} · ${compactCount(peak.amount)}",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            buckets.forEach { bucket ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight((bucket.amount.toFloat() / max).coerceIn(0f, 1f))
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            buckets.forEach { bucket ->
                Text(
                    text = bucket.score.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private fun compactCount(value: Int): String = when {
    value >= 1_000_000 -> "${value / 1_000_000}M"
    value >= 1_000 -> "${value / 1_000}K"
    else -> value.toString()
}
