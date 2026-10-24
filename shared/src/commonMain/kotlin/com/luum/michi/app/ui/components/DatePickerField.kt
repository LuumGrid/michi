package com.luum.michi.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.model.millisToCalendarParts
import com.luum.michi.app.ui.language.Strings

/**
 * Date button opening the system-style M3 calendar dialog (CLEAR / CANCEL
 * / OK, like the reference editor). Single dialog host per field; OK with
 * no selection changes nothing, CLEAR wipes the date (callers give it
 * meaning: the entry editor clears the field, the calendar jumps to
 * today). A null [label] renders the button alone for tight bars.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DatePickerField(
    millis: Long?,
    onConfirm: (Long) -> Unit,
    onClear: () -> Unit,
    strings: LanguageStrings = Strings.current,
    modifier: Modifier = Modifier,
    label: String? = null,
    buttonLabel: String? = null,
    selectableDates: SelectableDates = AllDates,
) {
    var showPicker by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        AppButton(
            label = buttonLabel ?: millis?.let { formatDateLabel(it, strings) } ?: strings.selectDateAction,
            onClick = { showPicker = true },
            emphasis = ButtonEmphasis.Secondary,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = millis,
            selectableDates = selectableDates,
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let(onConfirm)
                        showPicker = false
                    },
                ) {
                    Text(text = strings.datePickerOkAction)
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            onClear()
                            showPicker = false
                        },
                    ) {
                        Text(text = strings.datePickerClearAction)
                    }
                    TextButton(onClick = { showPicker = false }) {
                        Text(text = strings.cancelAction)
                    }
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

/** Default range: every date selectable (the editor has no bounds). */
private object AllDates : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean = true
    override fun isSelectableYear(year: Int): Boolean = true
}

/**
 * The picker speaks UTC calendar dates, so the label is formatted in UTC (a
 * local-zone read would shift the day behind west of Greenwich) with the
 * localized month names.
 */
private fun formatDateLabel(millis: Long, strings: LanguageStrings): String {
    val parts = millisToCalendarParts(millis)
    return "${parts.day.toString().padStart(2, '0')}-${strings.monthShortName(parts.month)}-${parts.year}"
}
