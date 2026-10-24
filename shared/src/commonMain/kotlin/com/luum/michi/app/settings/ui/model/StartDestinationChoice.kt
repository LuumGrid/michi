package com.luum.michi.app.settings.ui.model

import com.luum.michi.app.settings.domain.model.StartTabOption

/** One already-localized picker option; [value] is the id the host validates. */
internal data class SettingsChoice(val value: String, val label: String)

/**
 * "Pestaña al abrir" plus its dependent sub-choice, resolved and localized by
 * the host (Root): the sub-choice options are feature enums/labels, and settings
 * must never import a feature. [sectionTitle] is null when the tab has none.
 */
internal data class StartDestinationChoice(
    val tab: StartTabOption = StartTabOption.DISCOVER,
    val sectionTitle: String? = null,
    val sectionChoices: List<SettingsChoice> = emptyList(),
    val sectionValue: String = "",
)
