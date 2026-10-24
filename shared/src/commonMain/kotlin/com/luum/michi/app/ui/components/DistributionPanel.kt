package com.luum.michi.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** One bar of a [DistributionPanel]: its label and its raw count. */
internal data class DistributionEntry(
    val label: String,
    val count: Int,
)

/**
 * A framed distribution block: a [SectionHeader] title over a flat panel
 * (`surface` + [flatBorder], [PanelShape], 16dp padding) holding one
 * [DistributionBar] per entry, each scaled to the largest count. Content, so it
 * is always flat; the caller owns the horizontal inset.
 */
@Composable
internal fun DistributionPanel(
    title: String,
    entries: List<DistributionEntry>,
    modifier: Modifier = Modifier,
) {
    if (entries.isEmpty()) return
    val maxCount = entries.maxOf { it.count }
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = title)
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            shape = PanelShape,
            color = solidContainerColor(),
            border = flatBorder(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                entries.forEach { entry ->
                    DistributionBar(label = entry.label, count = entry.count, maxCount = maxCount)
                }
            }
        }
    }
}
