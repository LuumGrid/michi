package com.luum.michi.app.account.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luum.michi.app.account.presentation.components.AccountProfileQrCode
import com.luum.michi.app.account.presentation.components.AccountShareAvatar
import com.luum.michi.app.account.presentation.util.toProfilePathSegment
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.PlatformIcons
import com.luum.michi.app.core.platform.setPlainText
import kotlinx.coroutines.launch

@Composable
internal fun AccountShareProfileScreen(
    username: String,
    displayName: String,
    avatarUrl: String?,
) {
    val strings = LanguageProvider.strings
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val profileUrl = remember(username) { "https://luum.lat/${username.toProfilePathSegment()}" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 24.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))

        AccountShareCard(
            username = username,
            displayName = displayName,
            avatarUrl = avatarUrl,
            profileUrl = profileUrl,
        )

        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = { scope.launch { clipboard.setPlainText(profileUrl) } },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
            ) {
                Icon(
                    painter = PlatformIcons.Share,
                    contentDescription = strings.accountShareProfileAction,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(strings.accountShareProfileAction, maxLines = 1)
            }

            OutlinedButton(
                onClick = { },
                enabled = false,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
            ) {
                Icon(
                    painter = PlatformIcons.Download,
                    contentDescription = strings.accountDownloadProfileQrAction,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(strings.accountDownloadProfileQrAction, maxLines = 1)
            }
        }
    }
}

@Composable
private fun AccountShareCard(
    username: String,
    displayName: String,
    avatarUrl: String?,
    profileUrl: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AccountShareAvatar(
                username = username,
                avatarUrl = avatarUrl,
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = "@$username",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.74f),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(22.dp))

            Box(
                modifier = Modifier
                    .size(252.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                AccountProfileQrCode(
                    seed = profileUrl,
                    modifier = Modifier.fillMaxSize(),
                )
                AccountShareAvatar(
                    username = username,
                    avatarUrl = avatarUrl,
                    modifier = Modifier.size(54.dp),
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = profileUrl,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.82f),
                textAlign = TextAlign.Center,
            )
        }
    }
}
