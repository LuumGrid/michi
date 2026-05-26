package com.luum.michi.app.feed.presentation.components

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.components.PlatformBooleanRow
import com.luum.michi.app.core.platform.components.PlatformModalSheet
import com.luum.michi.app.feed.data.FeedActivityFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FeedFilterSheet(
    current: FeedActivityFilter,
    onDismiss: () -> Unit,
    onApply: (FeedActivityFilter) -> Unit,
) {
    val strings = LanguageProvider.strings

    var statuses by remember(current) { mutableStateOf(current.statuses) }
    var animationProgress by remember(current) { mutableStateOf(current.animationProgress) }
    var readingProgress by remember(current) { mutableStateOf(current.readingProgress) }
    var messages by remember(current) { mutableStateOf(current.messages) }
    var myActivities by remember(current) { mutableStateOf(current.myActivities) }

    PlatformModalSheet(
        onDismiss = onDismiss,
        maxHeightFraction = 0.86f,
    ) { modifier ->
        Column(modifier = modifier) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 14.dp, start = 8.dp, end = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = strings.feedFilterTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item {
                        PlatformBooleanRow(
                            label = strings.feedFilterStatuses,
                            checked = statuses,
                            onCheckedChange = { statuses = it },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item {
                        PlatformBooleanRow(
                            label = strings.feedFilterAnimationProgress,
                            checked = animationProgress,
                            onCheckedChange = { animationProgress = it },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item {
                        PlatformBooleanRow(
                            label = strings.feedFilterReadingProgress,
                            checked = readingProgress,
                            onCheckedChange = { readingProgress = it },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item {
                        PlatformBooleanRow(
                            label = strings.feedFilterMessages,
                            checked = messages,
                            onCheckedChange = { messages = it },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item {
                        PlatformBooleanRow(
                            label = strings.feedFilterMyActivities,
                            checked = myActivities,
                            onCheckedChange = { myActivities = it },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 0.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            val defaults = FeedActivityFilter()
                            onApply(defaults)
                        },
                        shape = RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .padding(end = 6.dp),
                    ) {
                        Text(
                            text = strings.filterResetAction,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Button(
                        onClick = {
                            onApply(
                                FeedActivityFilter(
                                    statuses = statuses,
                                    animationProgress = animationProgress,
                                    readingProgress = readingProgress,
                                    messages = messages,
                                    myActivities = myActivities,
                                )
                            )
                        },
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .padding(start = 6.dp),
                    ) {
                        Text(
                            text = strings.filterSaveAction,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}
