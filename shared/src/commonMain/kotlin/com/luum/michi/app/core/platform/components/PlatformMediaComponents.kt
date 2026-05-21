package com.luum.michi.app.core.platform.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.PlatformIcons

private val StandardCardHeight = 148.dp
private val ImageCornerRadius = 12.dp

@Composable
fun PlatformMediaListCard(
    title: String,
    subtitle: String,
    score: String,
    primaryProgressLabel: String,
    primaryProgressRatio: Float,
    palette: List<Color>,
    icon: Painter,
    isComplete: Boolean,
    statusLabel: String,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onIncrementPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    primaryIncrementLabel: String = "+1",
    secondaryProgressLabel: String? = null,
    secondaryProgressRatio: Float? = null,
    secondaryIncrementLabel: String = "+1",
    onIncrementSecondary: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .height(StandardCardHeight)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onOpen,
                onLongClick = onEdit,
            ),
        shape = RoundedCornerShape(16.dp), // Card rounding
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Image / Placeholder (Full height with rounding)
            Box(
                modifier = Modifier
                    .padding(8.dp) // Slight padding so corners are visible
                    .fillMaxHeight()
                    .width(86.dp)
                    .clip(RoundedCornerShape(ImageCornerRadius)) // Rounding on all 4 corners
                    .background(Brush.verticalGradient(palette)),
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(34.dp),
                )
            }

            // Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        PlatformScorePill(score = score)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProgressRow(
                        label = primaryProgressLabel,
                        ratio = primaryProgressRatio,
                        onIncrement = onIncrementPrimary,
                        incrementLabel = primaryIncrementLabel,
                        isComplete = isComplete,
                        color = palette.firstOrNull() ?: MaterialTheme.colorScheme.primary,
                    )

                    if (secondaryProgressLabel != null && secondaryProgressRatio != null && onIncrementSecondary != null) {
                        ProgressRow(
                            label = secondaryProgressLabel,
                            ratio = secondaryProgressRatio,
                            onIncrement = onIncrementSecondary,
                            incrementLabel = secondaryIncrementLabel,
                            isComplete = isComplete,
                            color = palette.getOrNull(1) ?: palette.firstOrNull() ?: MaterialTheme.colorScheme.secondary,
                        )
                    }
                }

                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ProgressRow(
    label: String,
    ratio: Float,
    onIncrement: () -> Unit,
    incrementLabel: String,
    isComplete: Boolean,
    color: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(
                onClick = onIncrement,
                enabled = !isComplete,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(30.dp),
            ) {
                Text(incrementLabel, style = MaterialTheme.typography.labelMedium)
            }
        }

        LinearProgressIndicator(
            progress = { ratio },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f),
        )
    }
}

@Composable
fun PlatformScorePill(score: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                painter = PlatformIcons.Like,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = score,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun PlatformSectionHeader(
    title: String,
    count: Int,
    modifier: Modifier = Modifier,
) {
    val strings = LanguageProvider.strings

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        PlatformCountPill(text = "$count ${strings.entriesLabel}")
    }
}

@Composable
fun PlatformCountPill(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
fun PlatformStepperField(
    label: String,
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlatformOutlinedFieldFrame(label = label, modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onMinus, modifier = Modifier.weight(1f)) {
                Text("−", style = MaterialTheme.typography.headlineSmall)
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = onPlus, modifier = Modifier.weight(1f)) {
                Text("+", style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}

@Composable
fun PlatformScoreField(
    score: Float,
    onScoreChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LanguageProvider.strings

    PlatformOutlinedFieldFrame(label = strings.scoreLabel, modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Slider(
                value = score,
                onValueChange = onScoreChange,
                valueRange = 0f..10f,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(18.dp))
            Text(
                text = (kotlin.math.round(score * 10f) / 10f).toString(),
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Composable
fun PlatformDateField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    PlatformOutlinedFieldFrame(label = label, modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = value.ifBlank { "—" },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = PlatformIcons.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
fun PlatformBooleanRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
fun PlatformOutlinedFieldFrame(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color.Transparent,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            content()
        }
    }
}
