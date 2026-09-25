package com.luum.michi.app.root

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.AppLanguage
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.ui.components.GhostButton
import com.luum.michi.app.ui.components.ModalSheet
import com.luum.michi.app.ui.components.OptionGroup
import com.luum.michi.app.ui.components.OptionRow
import com.luum.michi.app.ui.components.ToolbarAction
import com.luum.michi.app.ui.icons.AppIcons
import com.luum.michi.app.ui.language.Strings

/**
 * The single sign-in surface (list/Profile placeholders, Settings
 * sign-in, first launch): landing toolbar (Michi + language) plus X,
 * welcome copy without a service name (multi-service future), the shared
 * service buttons, and the guest button (X alone is missed by users).
 * selections are hoisted — open/close lives in root State.
 */
@Composable
internal fun AuthModal(
    services: List<AuthService>,
    isConfigured: Boolean,
    configMissingLabel: String,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    guestLabel: String,
    onDismiss: () -> Unit,
    strings: LanguageStrings = Strings.current,
    modifier: Modifier = Modifier,
) {
    var languageSheet by remember { mutableStateOf(false) }
    ModalSheet(
        title = "Michi",
        dismissLabel = strings.dismissAction,
        onDismiss = onDismiss,
        modifier = modifier,
        headerActions = listOf(
            ToolbarAction(
                id = ACTION_LANGUAGE,
                icon = AppIcons.Language,
                contentDescription = strings.languageLabel,
            ),
        ),
        onHeaderAction = { id ->
            if (id == ACTION_LANGUAGE) languageSheet = true
        },
    ) {
        Text(
            text = strings.authWelcomeTitle,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = strings.authModalSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(24.dp))
        AuthServiceButtons(
            services = services,
            isConfigured = isConfigured,
            configMissingLabel = configMissingLabel,
        )
        Spacer(modifier = Modifier.height(8.dp))
        GhostButton(
            label = guestLabel,
            onClick = onDismiss,
        )
    }

    if (languageSheet) {
        ModalSheet(
            title = strings.languageLabel,
            dismissLabel = strings.dismissAction,
            onDismiss = { languageSheet = false },
        ) {
            OptionGroup(title = null) {
                AppLanguage.available.forEachIndexed { index, option ->
                    OptionRow(
                        label = option.displayName,
                        selected = option == language,
                        onClick = {
                            onLanguageChange(option)
                        },
                        divider = index > 0,
                    )
                }
            }
        }
    }
}

private const val ACTION_LANGUAGE = "language"
