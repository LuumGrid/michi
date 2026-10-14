package com.luum.michi.app.root.state

import com.luum.michi.app.core.navigation.domain.TabSection
import com.luum.michi.app.discover.domain.model.DashboardMediaType
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListSection
import com.luum.michi.app.mediaList.domain.anime.model.animeStatusSections
import com.luum.michi.app.mediaList.domain.manga.model.MangaListSection
import com.luum.michi.app.mediaList.domain.manga.model.mangaStatusSections
import com.luum.michi.app.settings.domain.model.StartCatalogOption
import com.luum.michi.app.settings.domain.model.StartTabOption

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
        animeSection = animeStatusSections(splitCompletedAnime)
            .firstOrNull { it.name == animeSectionId }
            ?: AnimeListSection.ALL,
        mangaSection = mangaStatusSections(splitCompletedManga)
            .firstOrNull { it.name == mangaSectionId }
            ?: MangaListSection.ALL,
        catalogType = startCatalogType.toDashboardMediaType(),
    )
}
