package com.luum.michi.app.root.state

import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.navigation.domain.TabSection
import com.luum.michi.app.discover.domain.model.DashboardMediaType
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListSection
import com.luum.michi.app.mediaList.domain.anime.model.animeStatusSections
import com.luum.michi.app.mediaList.domain.anime.model.label
import com.luum.michi.app.mediaList.domain.manga.model.MangaListSection
import com.luum.michi.app.mediaList.domain.manga.model.mangaStatusSections
import com.luum.michi.app.mediaList.domain.manga.model.label
import com.luum.michi.app.settings.domain.model.StartCatalogOption
import com.luum.michi.app.settings.domain.model.StartTabOption
import com.luum.michi.app.settings.domain.model.label
import com.luum.michi.app.settings.ui.model.SettingsChoice
import com.luum.michi.app.settings.ui.model.StartDestinationChoice

/** What the shell opens on: the tab plus its dependent sub-choice, resolved. */
internal data class StartDestination(
    val tab: TabSection = TabSection.DISCOVER,
    val animeSection: AnimeListSection = AnimeListSection.ALL,
    val mangaSection: MangaListSection = MangaListSection.ALL,
    val catalogType: DashboardMediaType = DashboardMediaType.ANIME,
)

/** Dashboard type of the "Pestaña al abrir" sub-choice (settings keeps the enum). */
internal fun StartCatalogOption.toDashboardMediaType(): DashboardMediaType = when (this) {
    StartCatalogOption.ANIME -> DashboardMediaType.ANIME
    StartCatalogOption.MANGA -> DashboardMediaType.MANGA
}

/**
 * Live sections the ANIME "Pestaña al abrir" sub-choice offers: ALL plus the
 * split-aware status sections. Single source for the settings picker
 * ([startDestinationChoice]) and the startup resolution ([resolveStartDestination]),
 * so the two can never disagree on what a valid id is.
 */
internal fun animeStartSections(splitCompleted: Boolean): List<AnimeListSection> =
    listOf(AnimeListSection.ALL) + animeStatusSections(splitCompleted)

/** MANGA twin of [animeStartSections]. */
internal fun mangaStartSections(splitCompleted: Boolean): List<MangaListSection> =
    listOf(MangaListSection.ALL) + mangaStatusSections(splitCompleted)

/**
 * Assembles the localized "Pestaña al abrir" picker for [tab]: the dependent
 * sub-choice options plus the value to show. A stored id that no longer exists
 * (split toggled off, enum renamed) is shown as the value it resolves to (the
 * first option), never as a blank row; PROFILE has no sub-choice. Pure.
 */
internal fun startDestinationChoice(
    tab: StartTabOption,
    catalogType: StartCatalogOption,
    animeSectionId: String,
    mangaSectionId: String,
    splitCompletedAnime: Boolean,
    splitCompletedManga: Boolean,
    strings: LanguageStrings,
): StartDestinationChoice {
    val choices = when (tab) {
        StartTabOption.DISCOVER ->
            StartCatalogOption.entries.map { SettingsChoice(it.name, it.label(strings)) }
        StartTabOption.ANIME ->
            animeStartSections(splitCompletedAnime).map { SettingsChoice(it.name, it.label(strings)) }
        StartTabOption.MANGA ->
            mangaStartSections(splitCompletedManga).map { SettingsChoice(it.name, it.label(strings)) }
        StartTabOption.PROFILE -> emptyList()
    }
    val storedValue = when (tab) {
        StartTabOption.DISCOVER -> catalogType.name
        StartTabOption.ANIME -> animeSectionId
        StartTabOption.MANGA -> mangaSectionId
        StartTabOption.PROFILE -> ""
    }
    return StartDestinationChoice(
        tab = tab,
        sectionTitle = strings.settingsStartSectionTitle.takeIf { tab != StartTabOption.PROFILE },
        sectionChoices = choices,
        sectionValue = storedValue.takeIf { value -> choices.any { it.value == value } }
            ?: choices.firstOrNull()?.value.orEmpty(),
    )
}

/**
 * Resolves the "Pestaña al abrir" setting into a concrete start state. Pure.
 *
 * - A guest can only open DISCOVER: ANIME/MANGA/PROFILE need a session.
 * - A section id that is not in the live section list (split-completed toggled
 *   off, or the enum renamed) falls back to ALL instead of erroring.
 */
internal fun resolveStartDestination(
    startTab: StartTabOption,
    startCatalogType: StartCatalogOption,
    animeSectionId: String,
    mangaSectionId: String,
    splitCompletedAnime: Boolean,
    splitCompletedManga: Boolean,
    isGuest: Boolean,
): StartDestination {
    val tab = when (startTab) {
        StartTabOption.DISCOVER -> TabSection.DISCOVER
        StartTabOption.ANIME -> TabSection.ANIME
        StartTabOption.MANGA -> TabSection.MANGA
        StartTabOption.PROFILE -> TabSection.PROFILE
    }.let { if (isGuest && it != TabSection.DISCOVER) TabSection.DISCOVER else it }
    return StartDestination(
        tab = tab,
        animeSection = animeStartSections(splitCompletedAnime)
            .firstOrNull { it.name == animeSectionId }
            ?: AnimeListSection.ALL,
        mangaSection = mangaStartSections(splitCompletedManga)
            .firstOrNull { it.name == mangaSectionId }
            ?: MangaListSection.ALL,
        catalogType = startCatalogType.toDashboardMediaType(),
    )
}
