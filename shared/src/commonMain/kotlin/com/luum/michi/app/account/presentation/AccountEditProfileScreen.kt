package com.luum.michi.app.account.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luum.michi.app.account.presentation.components.AccountEditableAvatar
import com.luum.michi.app.account.presentation.model.AccountProfileDraft
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.PlatformIcons

private const val AniListProfileSettingsUrl = "https://anilist.co/settings/profile"
private const val AniListAccountSettingsUrl = "https://anilist.co/settings/account"

@Composable
internal fun AccountEditProfileScreen(initialDraft: AccountProfileDraft) {
    val strings = LanguageProvider.strings
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(
                PaddingValues(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = 24.dp,
                ),
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AccountEditableAvatar(
            username = initialDraft.username,
            avatarUrl = initialDraft.avatarUrl,
        )

        Spacer(Modifier.height(18.dp))

        Text(
            text = initialDraft.displayName,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = strings.accountWebOnlyFieldsNote,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(22.dp))

        OutlinedButton(
            onClick = { uriHandler.openUri(AniListProfileSettingsUrl) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                painter = PlatformIcons.Language,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(strings.accountEditProfileOnWebAction)
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = { uriHandler.openUri(AniListAccountSettingsUrl) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                painter = PlatformIcons.Language,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(strings.accountEditAccountOnWebAction)
        }
    }
}
