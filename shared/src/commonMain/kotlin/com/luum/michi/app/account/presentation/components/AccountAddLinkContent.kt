package com.luum.michi.app.account.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luum.michi.app.account.presentation.model.AccountProfileLink
import com.luum.michi.app.account.presentation.util.isSafeProfileUrl
import com.luum.michi.app.account.presentation.util.sanitized
import com.luum.michi.app.core.language.LanguageProvider

@Composable
internal fun AccountAddLinkContent(
    onSave: (AccountProfileLink) -> Unit,
    onCancel: () -> Unit,
) {
    val strings = LanguageProvider.strings
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = strings.accountAddLinkAction,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        AccountEditField(
            value = title,
            onValueChange = { title = it },
            label = strings.accountEditLinkTitleLabel,
        )
        AccountEditField(
            value = url,
            onValueChange = { url = it },
            label = strings.accountEditLinkUrlLabel,
        )
        Button(
            onClick = {
                AccountProfileLink(title = title.trim(), url = url.trim()).sanitized()?.let(onSave)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = title.isNotBlank() && isSafeProfileUrl(url.trim()),
        ) {
            Text(strings.accountSaveProfileAction)
        }
        TextButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(strings.backButton)
        }
    }
}
