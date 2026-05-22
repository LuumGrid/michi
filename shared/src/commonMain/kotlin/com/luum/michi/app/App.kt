package com.luum.michi.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.luum.michi.app.core.language.AppLanguage
import com.luum.michi.app.core.language.ProvideLanguageStrings
import com.luum.michi.app.core.language.currentPlatformLanguageCode
import com.luum.michi.app.shell.ShellScreen

@Composable
@Preview
fun App(
    initialLanguage: AppLanguage = AppLanguage.fromCode(currentPlatformLanguageCode()),
) {
    val systemDark = isSystemInDarkTheme()
    var isDarkMode by remember { mutableStateOf(systemDark) }
    var language by remember(initialLanguage) { mutableStateOf(initialLanguage) }

    MaterialTheme(
        colorScheme = if (isDarkMode) darkColorScheme() else lightColorScheme(),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            ProvideLanguageStrings(language) {
                ShellScreen(
                    language = language,
                    onLanguageChange = { language = it },
                    isDarkMode = isDarkMode,
                    onToggleTheme = { isDarkMode = !isDarkMode },
                )
            }
        }
    }
}
