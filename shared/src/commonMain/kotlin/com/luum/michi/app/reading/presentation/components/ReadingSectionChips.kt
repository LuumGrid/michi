package com.luum.michi.app.reading.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.components.PlatformChips
import com.luum.michi.app.reading.presentation.model.ReadingListSection
import com.luum.michi.app.reading.presentation.model.label

@Composable
internal fun ReadingSectionChips(
    selected: ReadingListSection,
    onSelect: (ReadingListSection) -> Unit,
    countForSection: (ReadingListSection) -> Int,
    modifier: Modifier = Modifier,
) {
    val strings = LanguageProvider.strings
    val sections = remember { ReadingListSection.entries }

    PlatformChips(
        items = sections,
        selectedItem = selected,
        onSelect = onSelect,
        label = { section -> "${section.label(strings)} ${countForSection(section)}" },
        modifier = modifier.fillMaxWidth(),
    )
}
