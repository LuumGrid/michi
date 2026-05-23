package com.luum.michi.app.animation.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.luum.michi.app.animation.presentation.model.AnimationListSection
import com.luum.michi.app.animation.presentation.model.label
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.components.PlatformChips

@Composable
internal fun AnimationSectionChips(
    selected: AnimationListSection,
    onSelect: (AnimationListSection) -> Unit,
    countForSection: (AnimationListSection) -> Int,
    modifier: Modifier = Modifier,
) {
    val strings = LanguageProvider.strings
    val sections = remember { AnimationListSection.entries }

    PlatformChips(
        items = sections,
        selectedItem = selected,
        onSelect = onSelect,
        label = { section -> "${section.label(strings)} ${countForSection(section)}" },
        modifier = modifier.fillMaxWidth(),
    )
}
