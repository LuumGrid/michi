package com.luum.michi.app.core.platform.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.AppLanguage
import com.luum.michi.app.core.platform.PlatformIcons
import com.luum.michi.app.core.language.LanguageProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlatformLanguageSwitch(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusManager = LocalFocusManager.current

    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val sheetHeight = with(density) { (windowInfo.containerSize.height * 0.57f).toDp() }
    val orderedLanguages = remember(selected) {
        listOf(selected) + AppLanguage.available
            .filterNot { it == selected }
            .sortedBy { it.displayName.lowercase() }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        TextButton(
            onClick = {
                focusManager.clearFocus(force = true)
                showSheet = true
            }
        ) {
            Text(
                text = selected.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                painter = PlatformIcons.ChevronUp,
                contentDescription = LanguageProvider.strings.languageLabel,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = sheetHeight)
            ) {
                item {
                    PlatformSelectorSheetTitle(text = LanguageProvider.strings.languageLabel)
                }

                item {
                    PlatformSelectorOptionGroup(items = orderedLanguages) { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelect(lang)
                                    showSheet = false
                                }
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = lang.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (lang == selected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                            if (lang == selected) {
                                PlatformInUseBadge()
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
