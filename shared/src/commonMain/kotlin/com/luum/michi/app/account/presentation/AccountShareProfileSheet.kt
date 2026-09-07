package com.luum.michi.app.account.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.luum.michi.app.ui.language.LanguageProvider
import com.luum.michi.app.ui.Icons
import com.luum.michi.app.ui.components.ModalSheet
import com.luum.michi.app.ui.setPlainText
import kotlinx.coroutines.launch

@Composable
internal fun AccountShareProfileSheet(
    username: String,
    displayName: String,
    avatarUrl: String?,
    onDismiss: () -> Unit,
) {
    val strings = LanguageProvider.strings
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val profileUrl = remember(username) { "https://anilist.co/user/${username.toProfilePathSegment()}" }

    ModalSheet(onDismiss = onDismiss) { modifier ->
        Column(
            modifier = modifier,
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

            Button(
                onClick = { scope.launch { clipboard.setPlainText(profileUrl) } },
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
            ) {
                Icon(
                    painter = Icons.Share,
                    contentDescription = strings.accountShareProfileAction,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(strings.accountShareProfileAction, maxLines = 1)
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
        modifier = Modifier.fillMaxWidth(0.95f),
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        ),
        contentColor = MaterialTheme.colorScheme.onSurface,
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}