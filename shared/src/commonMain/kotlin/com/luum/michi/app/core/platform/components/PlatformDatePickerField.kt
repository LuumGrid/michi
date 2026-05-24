package com.luum.michi.app.core.platform.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.media.CalendarDateParts
import com.luum.michi.app.core.media.millisToCalendarParts
import com.luum.michi.app.core.platform.PlatformIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlatformDatePickerField(
    label: String,
    valueMillis: Long?,
    onValueChange: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LanguageProvider.strings
    var showDialog by remember { mutableStateOf(false) }

    PlatformOutlinedFieldFrame(label = label, modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clickable { showDialog = true }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = valueMillis?.let { formatDate(it) } ?: strings.dateNotSetLabel,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                color = if (valueMillis != null) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (valueMillis != null) {
                Icon(
                    painter = PlatformIcons.Close,
                    contentDescription = strings.datePickerClearAction,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onValueChange(null) },
                )
            } else {
                Icon(
                    painter = PlatformIcons.Calendar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }

    if (showDialog) {
        val state = rememberDatePickerState(initialSelectedDateMillis = valueMillis)
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    onValueChange(state.selectedDateMillis)
                    showDialog = false
                }) { Text(strings.datePickerOkAction) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(strings.datePickerCancelAction)
                }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

private fun formatDate(millis: Long): String {
    val parts: CalendarDateParts = millisToCalendarParts(millis)
    val month = monthShort(parts.month)
    return "$month ${parts.day}, ${parts.year}"
}

private fun monthShort(month: Int): String = when (month) {
    1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"; 5 -> "May"; 6 -> "Jun"
    7 -> "Jul"; 8 -> "Aug"; 9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
    else -> ""
}
