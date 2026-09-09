package com.luum.michi.app.core

import com.luum.michi.app.calendar.domain.model.CalendarSeasonFilter
import com.luum.michi.app.calendar.domain.model.CalendarStatusFilter
import com.luum.michi.app.calendar.domain.model.label
import com.luum.michi.app.core.domain.language.EnglishStrings
import com.luum.michi.app.core.domain.language.LanguageStrings
import com.luum.michi.app.core.domain.language.SpanishStrings
import com.luum.michi.app.core.domain.medialist.MediaListStatus
import com.luum.michi.app.core.domain.medialist.label
import com.luum.michi.app.core.domain.model.UserListOrder
import com.luum.michi.app.core.domain.model.UserListSort
import com.luum.michi.app.core.domain.model.label
import com.luum.michi.app.core.domain.navigation.TabSection
import com.luum.michi.app.core.domain.navigation.label
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListSection
import com.luum.michi.app.mediaList.domain.anime.model.label
import com.luum.michi.app.mediaList.domain.manga.model.MangaListSection
import com.luum.michi.app.mediaList.domain.manga.model.label
import com.luum.michi.app.notifications.domain.model.NotificationFilter
import com.luum.michi.app.notifications.domain.model.label
import com.luum.michi.app.settings.domain.model.DiscoverTabOption
import com.luum.michi.app.settings.domain.model.ListSort
import com.luum.michi.app.settings.domain.model.ScoreFormat
import com.luum.michi.app.settings.domain.model.ThemeMode
import com.luum.michi.app.settings.domain.model.TitleLanguage
import com.luum.michi.app.settings.domain.model.label
import kotlin.test.Test
import kotlin.test.assertTrue

private val checkLanguages = listOf("en" to EnglishStrings, "es" to SpanishStrings)

private fun assertAllLabeled(name: String, labels: (LanguageStrings) -> List<String>) {
    checkLanguages.forEach { (code, strings) ->
        labels(strings).forEachIndexed { index, label ->
            assertTrue(label.isNotBlank(), "$name[$index] blank in $code")
        }
    }
}

class LabelsCoverageTest {

    @Test
    fun sortsAndOrders() {
        assertAllLabeled("UserListSort") { strings ->
            UserListSort.entries.map { it.label(strings) }
        }
        assertAllLabeled("UserListOrder") { strings ->
            UserListOrder.entries.map { it.label(strings) }
        }
    }

    @Test
    fun notificationFilters() {
        assertAllLabeled("NotificationFilter") { strings ->
            NotificationFilter.entries.map { it.label(strings) }
        }
    }

    @Test
    fun calendarFilters() {
        assertAllLabeled("CalendarSeasonFilter") { strings ->
            CalendarSeasonFilter.entries.map { it.label(strings) }
        }
        assertAllLabeled("CalendarStatusFilter") { strings ->
            CalendarStatusFilter.entries.map { it.label(strings) }
        }
    }

    @Test
    fun listSections() {
        assertAllLabeled("AnimeListSection") { strings ->
            AnimeListSection.entries.map { it.label(strings) }
        }
        assertAllLabeled("MangaListSection") { strings ->
            MangaListSection.entries.map { it.label(strings) }
        }
    }

    @Test
    fun mediaListStatusBothListTypes() {
        assertAllLabeled("MediaListStatus/anime") { strings ->
            MediaListStatus.entries.map { it.label(strings, isManga = false) }
        }
        assertAllLabeled("MediaListStatus/manga") { strings ->
            MediaListStatus.entries.map { it.label(strings, isManga = true) }
        }
    }

    @Test
    fun settingsOptions() {
        assertAllLabeled("ThemeMode") { strings ->
            ThemeMode.entries.map { it.label(strings) }
        }
        assertAllLabeled("TitleLanguage") { strings ->
            TitleLanguage.entries.map { it.label(strings) }
        }
        assertAllLabeled("ScoreFormat") { strings ->
            ScoreFormat.entries.map { it.label(strings) }
        }
        assertAllLabeled("ListSort") { strings ->
            ListSort.entries.map { it.label(strings) }
        }
        assertAllLabeled("DiscoverTabOption") { strings ->
            DiscoverTabOption.entries.map { it.label(strings) }
        }
    }

    @Test
    fun navigationTabs() {
        assertAllLabeled("TabSection") { strings ->
            TabSection.entries.map { it.label(strings) }
        }
    }
}
