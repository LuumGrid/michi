package com.luum.michi.app.notifications.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.components.PlatformModalSheet
import com.luum.michi.app.notifications.presentation.model.NotificationFilter
import com.luum.michi.app.notifications.presentation.model.label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotificationFilterSheet(
    selected: NotificationFilter,
    onSelect: (NotificationFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LanguageProvider.strings

    PlatformModalSheet(
        onDismiss = onDismiss,
        maxHeightFraction = 0.6f,
    ) { modifier ->
        Column(
            modifier = modifier,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 14.dp, start = 8.dp, end = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = strings.notificationsFilterTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(NotificationFilter.entries.toList()) { filter ->
                    val isSelected = filter == selected
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelect(filter) }
                            .padding(vertical = 2.dp, horizontal = 6.dp),
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelect(filter) },
                            modifier = Modifier.size(36.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = filter.label(strings),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}
