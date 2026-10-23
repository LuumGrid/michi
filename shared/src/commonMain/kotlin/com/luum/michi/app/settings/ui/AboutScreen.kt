package com.luum.michi.app.settings.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.MichiBuildConfig
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.ui.components.FlatChip
import com.luum.michi.app.ui.components.OptionGroup
import com.luum.michi.app.ui.components.PanelShape
import com.luum.michi.app.ui.components.SettingsRow
import com.luum.michi.app.ui.components.flatBorder
import com.luum.michi.app.ui.icons.AppIcons
import com.luum.michi.app.ui.language.Strings

/**
 * About Michi, a page inside Settings (Root owns the toolbar: back +
 * settingsAboutTitle). Hero with the real logo, then the Project and Legal
 * groups; every row opens a link or the share sheet.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AboutScreen(
    bottomPadding: Dp,
    onOpenUrl: (String) -> Unit,
    onShare: (text: String, subject: String) -> Unit,
    strings: LanguageStrings = Strings.current,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 16.dp,
            end = 16.dp,
            bottom = 16.dp + bottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "hero") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // White on purpose, also in dark mode: it is the brand mark, a
                // black symbol on white. No tint, no color filter.
                Surface(
                    color = Color.White,
                    shape = PanelShape,
                    border = flatBorder(),
                    modifier = Modifier.size(88.dp),
                ) {
                    Image(
                        imageVector = AppIcons.MichiSymbol,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Michi",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${strings.settingsAboutVersionLabel} ${MichiBuildConfig.Version}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = strings.aboutTagline,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(
                        8.dp,
                        Alignment.CenterHorizontally,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FlatChip(strings.aboutChipFree)
                    FlatChip(strings.aboutChipNoAds)
                    FlatChip(strings.aboutChipOpenSource)
                }
            }
        }
        item(key = "project") {
            OptionGroup(title = strings.aboutProjectSection) {
                SettingsRow(
                    label = strings.aboutSourceCodeTitle,
                    value = "",
                    supportingText = "GitHub",
                    icon = AppIcons.Code,
                    onClick = { onOpenUrl(AboutLinks.SourceCode) },
                    divider = false,
                )
                SettingsRow(
                    label = strings.aboutReportIssueTitle,
                    value = "",
                    supportingText = strings.aboutReportIssueSubtitle,
                    icon = AppIcons.BugReport,
                    onClick = { onOpenUrl(AboutLinks.NewIssue) },
                )
                SettingsRow(
                    label = strings.aboutShareTitle,
                    value = "",
                    icon = AppIcons.Share,
                    onClick = { onShare(strings.aboutShareText(AboutLinks.LatestRelease), "Michi") },
                )
            }
        }
        item(key = "legal") {
            OptionGroup(title = strings.aboutLegalSection) {
                SettingsRow(
                    label = strings.aboutPrivacyTitle,
                    value = "",
                    icon = AppIcons.Shield,
                    onClick = { onOpenUrl(AboutLinks.PrivacyPolicy) },
                    divider = false,
                )
                SettingsRow(
                    label = strings.aboutLicenseTitle,
                    value = "",
                    supportingText = "GPL-3.0",
                    icon = AppIcons.Description,
                    onClick = { onOpenUrl(AboutLinks.License) },
                )
                SettingsRow(
                    label = strings.aboutDataByTitle,
                    value = "",
                    supportingText = strings.aboutNotAffiliatedText,
                    icon = AppIcons.AniList,
                    onClick = { onOpenUrl(AboutLinks.AniList) },
                )
            }
        }
    }
}

private object AboutLinks {
    const val SourceCode = "https://github.com/LuumGrid/michi"
    const val NewIssue = "https://github.com/LuumGrid/michi/issues/new"
    const val LatestRelease = "https://github.com/LuumGrid/michi/releases/latest"
    const val PrivacyPolicy =
        "https://github.com/LuumGrid/michi/blob/main/docs/privacy.md"
    const val License = "https://github.com/LuumGrid/michi/blob/main/LICENSE"
    const val AniList = "https://anilist.co"
}
