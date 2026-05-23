package com.luum.michi.app.reading.presentation.model

import com.luum.michi.app.core.language.LanguageStrings

internal enum class ReadingListSection {
    ALL,
    READING,
    COMPLETED,
    PAUSED,
    DROPPED,
    PLANNING,
    REREADING,
}

internal val ReadingStatusSections: List<ReadingListSection> = listOf(
    ReadingListSection.READING,
    ReadingListSection.COMPLETED,
    ReadingListSection.PAUSED,
    ReadingListSection.DROPPED,
    ReadingListSection.PLANNING,
    ReadingListSection.REREADING,
)

internal fun ReadingListSection.label(strings: LanguageStrings): String = when (this) {
    ReadingListSection.ALL -> strings.sectionAll
    ReadingListSection.READING -> strings.sectionReading
    ReadingListSection.COMPLETED -> strings.sectionCompleted
    ReadingListSection.PAUSED -> strings.sectionPaused
    ReadingListSection.DROPPED -> strings.sectionDropped
    ReadingListSection.PLANNING -> strings.sectionPlanning
    ReadingListSection.REREADING -> strings.sectionRereading
}
