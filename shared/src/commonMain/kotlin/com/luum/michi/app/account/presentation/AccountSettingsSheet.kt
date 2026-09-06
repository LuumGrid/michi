package com.luum.michi.app.account.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.PlatformIcons
import com.luum.michi.app.core.platform.components.PlatformModalSheet

private const val AniListUpdateProfileUrl = "https://anilist.co/settings"
private const val AniListAccountSettingsUrl = "https://anilist.co/settings/account"
private const val AniListListSettingsUrl = "https://anilist.co/settings/lists"
private const val AniListImportListUrl = "https://anilist.co/settings/import"

@Composable
internal fun AccountSettingsSheet(onDismiss: () -> Unit) {
    val strings = LanguageProvider.strings
    val uriHandler = LocalUriHandler.current

    PlatformModalSheet(onDismiss = onDismiss) { modifier ->
        Column(
            modifier = modifier.padding(horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))

            Text(
                text = strings.accountSettingsTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = strings.accountWebOnlyFieldsNote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(20.dp))

            AccountSettingsLinkButton(strings.accountUpdateProfileOnWebAction, AniListUpdateProfileUrl, uriHandler)
            Spacer(Modifier.height(12.dp))
            AccountSettingsLinkButton(strings.accountManageAccountOnWebAction, AniListAccountSettingsUrl, uriHandler)
            Spacer(Modifier.height(12.dp))
            AccountSettingsLinkButton(strings.accountListSettingsOnWebAction, AniListListSettingsUrl, uriHandler)
            Spacer(Modifier.height(12.dp))
            AccountSettingsLinkButton(strings.accountImportListOnWebAction, AniListImportListUrl, uriHandler)
        }
    }
}

@Composable
private fun AccountSettingsLinkButton(label: String, url: String, uriHandler: UriHandler) {
    Button(
        onClick = { uriHandler.openUri(url) },
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(24.dp),
    ) {
        Icon(painter = PlatformIcons.Language, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(label, maxLines = 1)
    }
}
