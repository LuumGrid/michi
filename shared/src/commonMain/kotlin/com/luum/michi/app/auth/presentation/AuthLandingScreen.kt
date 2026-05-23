package com.luum.michi.app.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.auth.AniListOAuthConfig
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.MichiAppName

private val MichiBrandColors = listOf(
    Color(0xFFFFC857),
    Color(0xFFFF5C8A),
    Color(0xFF6C63FF),
)

@Composable
internal fun AuthLandingScreen(
    onLoginClick: () -> Unit,
    errorMessage: String? = null,
) {
    val strings = LanguageProvider.strings
    val isConfigured = AniListOAuthConfig.isConfigured

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(MichiBrandColors)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = MichiAppName.take(1).uppercase(),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.surface,
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = strings.authWelcomeTitle,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = strings.authWelcomeSubtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(40.dp))

        Button(
            onClick = onLoginClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            enabled = isConfigured,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(
                text = strings.authLoginAction,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        if (!isConfigured) {
            Spacer(Modifier.height(16.dp))
            AuthFootnote(
                text = strings.authConfigurationMissing,
                tone = MaterialTheme.colorScheme.error,
            )
        } else if (!errorMessage.isNullOrBlank()) {
            Spacer(Modifier.height(16.dp))
            AuthFootnote(
                text = errorMessage,
                tone = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun AuthFootnote(text: String, tone: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall,
            color = tone,
            textAlign = TextAlign.Center,
        )
    }
}
