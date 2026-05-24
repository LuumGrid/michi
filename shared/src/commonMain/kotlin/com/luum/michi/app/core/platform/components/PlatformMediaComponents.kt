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

private val StandardCardHeight = 158.dp
private val StandardCoverWidth = PlatformCoverSize.RowPosterWidth
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
    releaseLabel: String?,
    behindLabel: String?,
    fallbackStatusLabel: String,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onIncrementPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    coverUrl: String? = null,
    primaryIncrementLabel: String = "+1",
    primaryIncrementValueLabel: String? = null,
    primaryIncrementEnabled: Boolean = !isComplete,
    secondaryProgressLabel: String? = null,
    secondaryProgressRatio: Float? = null,
    secondaryIncrementLabel: String = "+1",
    secondaryIncrementValueLabel: String? = null,
    secondaryIncrementEnabled: Boolean = !isComplete,
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
            PlatformMediaCover(
                coverUrl = coverUrl,
                palette = palette,
                contentDescription = title,
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxHeight()
                    .width(StandardCoverWidth),
                cornerRadius = ImageCornerRadius,
                fallbackIcon = icon,
                fallbackIconSize = 37.dp,
            )

            // Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = 12.dp, horizontal = 8.dp),
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

                ProgressActionsColumn(
                    incrementValueLabel = primaryIncrementValueLabel,
                    incrementLabel = primaryIncrementLabel,
                    onIncrement = onIncrementPrimary,
                    secondaryIncrementValueLabel = secondaryIncrementValueLabel,
                    secondaryIncrementLabel = secondaryIncrementLabel,
                    onIncrementSecondary = onIncrementSecondary,
                    primaryEnabled = primaryIncrementEnabled,
                    secondaryEnabled = secondaryIncrementEnabled,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )

                Column(
                    modifier = Modifier.align(Alignment.BottomStart),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    val statusLines = listOfNotNull(
                        behindLabel,
                        releaseLabel,
                    ).ifEmpty { listOf(fallbackStatusLabel) }

                    statusLines.forEach { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    ProgressRow(
                        ratio = primaryProgressRatio,
                        onIncrement = onIncrementPrimary,
                        incrementLabel = primaryIncrementLabel,
                        incrementValueLabel = primaryIncrementValueLabel,
                        secondaryIncrementLabel = secondaryIncrementLabel,
                        secondaryIncrementValueLabel = secondaryIncrementValueLabel,
                        onIncrementSecondary = onIncrementSecondary,
                        primaryIncrementEnabled = primaryIncrementEnabled,
                        secondaryIncrementEnabled = secondaryIncrementEnabled,
                        color = palette.firstOrNull() ?: MaterialTheme.colorScheme.primary,
                        showActions = false,
                    )

                    if (secondaryProgressLabel != null && secondaryProgressRatio != null && onIncrementSecondary != null) {
                        ProgressRow(
                            ratio = secondaryProgressRatio,
                            onIncrement = onIncrementSecondary,
                            incrementLabel = secondaryIncrementLabel,
                            incrementValueLabel = null,
                            secondaryIncrementLabel = null,
                            secondaryIncrementValueLabel = null,
                            onIncrementSecondary = null,
                            primaryIncrementEnabled = secondaryIncrementEnabled,
                            secondaryIncrementEnabled = false,
                            color = palette.getOrNull(1) ?: palette.firstOrNull() ?: MaterialTheme.colorScheme.secondary,
                            showActions = false,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressRow(
    ratio: Float,
    onIncrement: () -> Unit,
    incrementLabel: String,
    incrementValueLabel: String?,
    secondaryIncrementLabel: String?,
    secondaryIncrementValueLabel: String?,
    onIncrementSecondary: (() -> Unit)?,
    primaryIncrementEnabled: Boolean,
    secondaryIncrementEnabled: Boolean,
    color: Color,
    showActions: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
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

            if (showActions) {
                Spacer(modifier = Modifier.width(10.dp))

                ProgressActionsColumn(
                    incrementValueLabel = incrementValueLabel,
                    incrementLabel = incrementLabel,
                    onIncrement = onIncrement,
                    secondaryIncrementValueLabel = secondaryIncrementValueLabel,
                    secondaryIncrementLabel = secondaryIncrementLabel,
                    onIncrementSecondary = onIncrementSecondary,
                    primaryEnabled = primaryIncrementEnabled,
                    secondaryEnabled = secondaryIncrementEnabled,
                )
            }
        }
    }
}

@Composable
private fun ProgressActionsColumn(
    incrementValueLabel: String?,
    incrementLabel: String,
    onIncrement: () -> Unit,
    secondaryIncrementValueLabel: String?,
    secondaryIncrementLabel: String?,
    onIncrementSecondary: (() -> Unit)?,
    primaryEnabled: Boolean,
    secondaryEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.End,
    ) {
        ProgressIncrementAction(
            valueLabel = incrementValueLabel,
        buttonLabel = incrementLabel,
        onClick = onIncrement,
        enabled = primaryEnabled,
    )

    if (secondaryIncrementLabel != null && onIncrementSecondary != null) {
            ProgressIncrementAction(
                valueLabel = secondaryIncrementValueLabel,
            buttonLabel = secondaryIncrementLabel,
            onClick = onIncrementSecondary,
            enabled = secondaryEnabled,
        )
    }
}
}

@Composable
private fun ProgressIncrementAction(
    valueLabel: String?,
    buttonLabel: String,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (valueLabel != null) {
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }

        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(30.dp),
        ) {
            Text(buttonLabel, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun PlatformScorePill(score: String) {
    val hasUserScore = score != "-" && score != "0" && score.isNotBlank()
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
                painter = if (hasUserScore) PlatformIcons.StarFilled else PlatformIcons.Star,
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
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onMinus, modifier = Modifier.weight(1f)) {
                Text("−", style = MaterialTheme.typography.titleLarge)
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = onPlus, modifier = Modifier.weight(1f)) {
                Text("+", style = MaterialTheme.typography.titleLarge)
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
                .height(64.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Slider(
                value = score,
                onValueChange = onScoreChange,
                valueRange = 0f..10f,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = (kotlin.math.round(score * 10f) / 10f).toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
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
                .height(56.dp)
                .padding(horizontal = 16.dp),
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
                modifier = Modifier.size(20.dp),
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
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
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
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(start = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color.Transparent,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            content()
        }
    }
}
