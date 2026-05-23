package com.luum.michi.app.account.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.luum.michi.app.account.presentation.util.toBirthDateInput
import com.luum.michi.app.account.presentation.util.toDayMonthYear
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.PlatformIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountBirthDateField(
    value: String,
    onValueChange: (String) -> Unit,
    isPublic: Boolean,
    onVisibilityChange: (Boolean) -> Unit,
) {
    val strings = LanguageProvider.strings
    var showCalendar by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    var textFieldValue by remember(value) {
        mutableStateOf(TextFieldValue(value, selection = TextRange(value.length)))
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { input ->
                val formattedValue = input.text.toBirthDateInput()
                textFieldValue = TextFieldValue(
                    text = formattedValue,
                    selection = TextRange(formattedValue.length),
                )
                onValueChange(formattedValue)
            },
            label = { Text(strings.accountEditBirthDateLabel) },
            placeholder = { Text("dd/MM/yyyy") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            trailingIcon = {
                IconButton(onClick = { showCalendar = true }) {
                    Icon(
                        painter = PlatformIcons.Calendar,
                        contentDescription = strings.accountSelectDateAction,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPublic) strings.accountVisibilityPublic else strings.accountVisibilityPrivate,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = strings.accountVisibilitySubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = isPublic, onCheckedChange = onVisibilityChange)
        }
    }

    if (showCalendar) {
        DatePickerDialog(
            onDismissRequest = { showCalendar = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onValueChange(millis.toDayMonthYear())
                        }
                        showCalendar = false
                    },
                ) {
                    Text(strings.accountSelectDateConfirmAction)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCalendar = false }) {
                    Text(strings.backButton)
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
