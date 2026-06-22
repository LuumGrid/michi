package com.luum.michi.app.account.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luum.michi.app.account.presentation.model.AccountStatDistributionEntry

private val BarHeight = 10.dp
private val LabelWidth = 72.dp

/**
 * Reusable horizontal bar list for a distribution (score / format / status /
 * genre breakdowns). Each bar's width is normalized against the largest
 * [AccountStatDistributionEntry.count] in [entries] - no charting library
 * involved, just a row of fractional-width boxes.
 */
@Composable
internal fun AccountStatBarChart(
    entries: List<AccountStatDistributionEntry>,
    modifier: Modifier = Modifier,
) {
    if (entries.isEmpty()) return
    val maxCount = entries.maxOf { it.count }.coerceAtLeast(1)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        entries.forEach { entry ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(LabelWidth),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(BarHeight)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    val fraction = (entry.count.toFloat() / maxCount.toFloat()).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
                Text(
                    text = entry.count.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}
