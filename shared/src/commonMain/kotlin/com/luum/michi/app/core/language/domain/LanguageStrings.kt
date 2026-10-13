package com.luum.michi.app.core.language.domain

import com.luum.michi.app.core.model.MediaReleaseDateTime

interface LanguageStrings {
    val languageLabel: String
    val logoutAction: String
    val notificationsLabel: String
    val discoverLabel: String
    val animeLabel: String
    val mangaLabel: String
    val profileLabel: String
    val settingsAction: String
    val discoverTrendingAnimeTitle: String
    val discoverTrendingMangaTitle: String
    val discoverNewReleasesMangaTitle: String
    val discoverLoadingLabel: String
    val discoverErrorLabel: String
    val discoverEmptyLabel: String
    val searchTitle: String
    val filterAction: String
    val sortAction: String
    val calendarAction: String
    val dismissAction: String
    val clearSearchAction: String
    val searchTypeAll: String
    val searchEmptyQueryHint: String
    val searchNoResultsLabel: String
    val exploreThisSeasonTitle: String
    val exploreUpcomingNextSeasonTitle: String
    val exploreAllTimePopularAnimeTitle: String
    val exploreAllTimePopularMangaTitle: String
    val exploreTopAnimeTitle: String
    val exploreTopMangaTitle: String
    fun exploreEpisodeCountLabel(count: Int): String
    fun exploreChapterCountLabel(count: Int): String
    val charactersLabel: String
    val studiosLabel: String
    val exploreFilterTypeLabel: String
    val seasonLabel: String
    val exploreFilterGenreLabel: String
    val formatLabel: String
    val exploreFilterYearLabel: String
    val exploreAnySeasonLabel: String
    val exploreAnyYearLabel: String
    /** Season names for preformatted detail labels (kept separate from the
     *  explore filter labels so each context can evolve independently). */
    val seasonWinterLabel: String
    val seasonSpringLabel: String
    val seasonSummerLabel: String
    val seasonFallLabel: String
    val calendarTitle: String
    val calendarEmptyLabel: String
    val tomorrowLabel: String
    fun calendarHeaderLabel(prefix: String, day: Int, month: Int, year: Int): String
    val dayMonday: String
    val dayTuesday: String
    val dayWednesday: String
    val dayThursday: String
    val dayFriday: String
    val daySaturday: String
    val daySunday: String
    val mediaDetailDescriptionTitle: String
    val mediaDetailGenresTitle: String
    val mediaDetailRelationsTitle: String
    val mediaDetailReadMoreAction: String
    val mediaDetailReadLessAction: String
    val mediaRelationSequel: String
    val mediaRelationPrequel: String
    val mediaRelationSideStory: String
    val mediaRelationSpinOff: String
    val mediaRelationParent: String
    val mediaRelationAdaptation: String
    val mediaRelationAlternative: String
    val mediaRelationSource: String
    val mediaRelationSummary: String
    val mediaRelationCharacter: String
    val mediaRelationOther: String
    val mediaDetailTabStaff: String
    val mediaDetailTabRecommendations: String
    val mediaDetailCharacterRoleMain: String
    val mediaDetailCharacterRoleSupporting: String
    val mediaDetailCharacterRoleBackground: String
    val mediaDetailNoCharactersLabel: String
    val mediaDetailCharactersTitle: String
    val mediaDetailVoiceLanguageLabel: String
    val mediaDetailLoadMoreAction: String
    val voiceLanguageJapanese: String
    val voiceLanguageEnglish: String
    val voiceLanguageKorean: String
    val voiceLanguageItalian: String
    val voiceLanguageSpanish: String
    val voiceLanguagePortuguese: String
    val voiceLanguageFrench: String
    val voiceLanguageGerman: String
    val voiceLanguageHebrew: String
    val voiceLanguageHungarian: String
    val mediaDetailEpisodesLabel: String
    val mediaDetailDurationLabel: String
    val popularityLabel: String
    val favoritesLabel: String
    val mediaDetailSourceLabel: String
    val mediaDetailEndedLabel: String
    val mediaDetailLoadingLabel: String
    val mediaDetailErrorLabel: String
    val editEntryAction: String
    val cancelAction: String
    val mediaDetailEditorDeleteAction: String
    val deleteEntryConfirmTitle: String
    val deleteEntryConfirmMessage: String
    val confirmDeleteAction: String
    val sectionAll: String
    val sectionWatching: String
    val sectionCurrent: String
    val sectionCompletedTv: String
    val sectionCompletedMovie: String
    val sectionCompletedOva: String
    val sectionCompletedOna: String
    val sectionCompletedTvShort: String
    val sectionCompletedSpecial: String
    val sectionPaused: String
    val sectionDropped: String
    val sectionPlanning: String
    val sectionRewatching: String
    val sectionRepeating: String
    val statusLabel: String
    val progressLabel: String
    val chaptersLabel: String
    val volumesLabel: String
    val scoreLabel: String
    val notesLabel: String
    val startedLabel: String
    val completedLabel: String
    val datePickerOkAction: String
    val selectDateAction: String
    val datePickerClearAction: String
    val todayLabel: String
    val favouriteLabel: String
    val priorityLabel: String
    val repeatLabel: String
    val privateLabel: String
    val hiddenFromStatusListsLabel: String
    val saveAction: String
    fun episodesBehind(count: Int): String
    fun chaptersBehind(count: Int): String
    fun nextEpisodeReleaseLabel(episodeNumber: Int, releaseDateTime: MediaReleaseDateTime): String
    fun airingCountdownLabel(days: Int, hours: Int, minutes: Int): String
    fun notificationDateLabel(dateTime: MediaReleaseDateTime): String
    /** Short month name used by preformatted date labels (media airing dates, birthdays). */
    fun monthShortName(month: Int): String
    val profileFollowersLabel: String
    val profileFollowingLabel: String
    val profileFavoriteAnimeTitle: String
    val profileFavoriteMangaTitle: String
    val profileFavoriteCharactersTitle: String
    val profileFavoriteStaffTitle: String
    val profileFavoriteStudiosTitle: String
    fun profileJoinedLabel(month: Int, year: Int): String
    val profileEditProfileAction: String
    val profileShareProfileAction: String
    val profileDownloadProfileQrAction: String
    val profileWebOnlyFieldsNote: String
    val profileUpdateProfileOnWebAction: String
    val profileManageProfileOnWebAction: String
    val profileListSettingsOnWebAction: String
    val profileImportListOnWebAction: String
    val settingsTitle: String
    val profileStatsEpisodesWatchedLabel: String
    val profileStatsChaptersReadLabel: String
    val profileStatsDaysWatchedLabel: String
    val profileStatsVolumesReadLabel: String
    val profileStatsMeanScoreLabel: String
    val profileStatsStandardDeviationLabel: String
    val profileStatsScoreDistributionTitle: String
    val profileStatsFormatDistributionTitle: String
    val profileStatsTopGenresTitle: String
    val profileStatsEmptyLabel: String
    val profileFavoritesGridEmptyLabel: String
    val settingsGeneralSection: String
    val settingsAniListSection: String
    val settingsListsSection: String
    val settingsInformationSection: String
    val settingsThemeTitle: String
    val settingsStyleTitle: String
    val settingsSurfaceTitle: String
    val settingsImageQualityTitle: String
    val settingsImageQualityAuto: String
    val settingsImageQualityHigh: String
    val settingsImageQualityLow: String
    val surfaceGlassLabel: String
    val surfaceSolidLabel: String
    val settingsThemeSystem: String
    val settingsThemeLight: String
    val settingsThemeDark: String
    val themePaletteSection: String
    val themeModeSection: String
    val themeFontSection: String
    val settingsTitleLanguageTitle: String
    val settingsTitleLanguageRomaji: String
    val settingsTitleLanguageEnglish: String
    val settingsTitleLanguageNative: String
    val settingsStaffNameTitle: String
    val settingsStaffNameWestern: String
    val settingsStaffNameRomaji: String
    val settingsStaffNameNative: String
    val settingsAdultContentTitle: String
    val settingsAdultContentSubtitle: String
    val settingsScoreFormatTitle: String
    val settingsScoreFormatPoint100: String
    val settingsScoreFormatPoint10Decimal: String
    val settingsScoreFormatPoint10: String
    val settingsScoreFormatPoint5Stars: String
    val settingsScoreFormatPoint3Smileys: String
    val settingsListSortTitle: String
    val settingsListSortByScore: String
    val settingsListSortByUpdated: String
    val settingsListSortByAdded: String
    val settingsSplitCompletedAnimeTitle: String
    val settingsSplitCompletedAnimeSubtitle: String
    val settingsSplitCompletedMangaTitle: String
    val settingsSplitCompletedMangaSubtitle: String
    val settingsAdvancedScoringTitle: String
    val settingsAdvancedScoringSubtitle: String
    val settingsPersistSortTitle: String
    val settingsPersistSortSubtitle: String
    val notificationsSection: String
    val notificationsAiringTitle: String
    val activityLabel: String
    val forumLabel: String
    val notificationsMessagesTitle: String
    val notificationsMediaTitle: String
    val settingsAboutTitle: String
    val settingsAboutSubtitle: String
    val settingsAboutVersionLabel: String
    val settingsAboutDataByText: String
    val settingsAboutUnofficialText: String
    val settingsAboutDescriptionFirst: String
    val settingsAboutDescriptionSecond: String
    val settingsAccountTitle: String
    val settingsAccountSettingsTitle: String
    val settingsSignInTitle: String
    val settingsSignOutTitle: String
    val settingsSignOutConfirmAction: String
    val settingsSignOutConfirmMessage: String
    val backButton: String
    val authWelcomeTitle: String
    val authModalSubtitle: String
    val authGuestPrompt: String
    val authLoginAction: String
    val authContinueAsGuestAction: String
    val authConfigurationMissing: String
    val authLoadingLabel: String
    val retryAction: String
    val viewAllAction: String
    val listsLoadingLabel: String
    val listsEmptyLabel: String
    val listsErrorLabel: String
    val listsIncrementEpisodeAction: String
    val listsIncrementChapterAction: String
    val listsIncrementVolumeAction: String
    val workStatusFinished: String
    val workStatusAiring: String
    val workStatusPublishing: String
    val workStatusUpcoming: String
    val workStatusCancelled: String
    val workStatusHiatus: String
    val errorNoConnectionLabel: String
    val errorUnauthorizedLabel: String
    val errorRateLimitedLabel: String
    val errorServerLabel: String
    val errorUnknownLabel: String

    // Filter Sheet & Filter/Order Labels
    val filterOrderDirectionTitle: String
    val filterSortCriterionTitle: String
    val filterResetAction: String

    val sortFollowList: String
    val sortTitle: String
    val sortScore: String
    val sortLastUpdated: String
    val sortLastAdded: String
    val sortStartDate: String
    val sortCompletedDate: String
    val sortReleaseDate: String
    val sortAverageScore: String
    val sortTrending: String
    val sortNextAiring: String

    val orderAscending: String
    val orderDescending: String

    val discoverSearchPlaceholder: String

    // Notifications
    val notificationsEmptyLabel: String
    val notificationsErrorLabel: String
    val notificationFilterAll: String
    val notificationFilterAiring: String
    val notificationFilterReplies: String
    val notificationFilterFollows: String
    val notificationFilterMedia: String
    val calendarSeasonAll: String
    val calendarSeasonPrevious: String
    val calendarSeasonCurrent: String
    val calendarSeasonNext: String
    val otherLabel: String
    val calendarStatusAll: String
    val calendarStatusWatchingPlanning: String
    val calendarStatusNotInList: String
    val notifAiringPrefix: String
    val notifAiringAired: String
    val notifFollowingLabel: String
    val notifActivityLabel: String
    val notifForumLabel: String
    val notifMessageLabel: String
    val notifMediaChangeLabel: String

    // Studio detail
    val sortByNewest: String
    val sortByOldest: String
    val sortByScore: String

    // Staff detail
    val yearsActivePresent: String

    // Media detail redesign
    val mediaDetailSeasonLabel: String
    val mediaDetailStudioLabel: String
    val mediaDetailFormatLabel: String
    val mediaDetailStatusLabel: String
    val mediaDetailStartDateLabel: String
    val mediaDetailRomajiLabel: String
    val mediaDetailSynonymsLabel: String
    val mediaDetailVideosTitle: String
    val mediaDetailInformationTitle: String
    val mediaDetailTagsTitle: String
    val mediaDetailExternalLinksTitle: String
    val mediaDetailStreamingLinksTitle: String
    fun mediaDetailNextEpisodeReleaseLabel(
        episode: Int,
        releaseDateTime: MediaReleaseDateTime,
        countdown: String,
    ): String
    val mediaDetailOfficialSiteLabel: String
    val mediaDetailOpenInAniListAction: String
    val mediaDetailShareAction: String
    val mediaDetailAverageScoreLabel: String
    val mediaDetailAllTimeLabel: String
    val moreAction: String

    // Shared detail labels (character/staff/studio)
    val sortByPopularity: String
    val sortByFavourites: String
    val mediaDetailGenderLabel: String
    val mediaDetailAgeLabel: String
    val mediaDetailBloodTypeLabel: String
    val mediaDetailBirthdayLabel: String
    val mediaDetailDeathLabel: String
    val mediaDetailYearsActiveLabel: String
    val mediaDetailHometownLabel: String
    val mediaDetailOccupationsLabel: String
    val mediaDetailAppearancesTitle: String
    val mediaDetailRolesTitle: String
    val mediaDetailMediaTitle: String
    val mediaDetailNoMediaLabel: String
    val mediaDetailAnimeStudioLabel: String

    // Entry editor redesign
    val discardChangesConfirmTitle: String
    val discardChangesConfirmMessage: String
    val discardChangesConfirmAction: String
    val keepEditingAction: String

    // Person pages
    val mediaDetailNativeLabel: String
    val mediaDetailVoiceActorsTitle: String
    val mediaDetailNoVoiceActorsLabel: String
}

fun getLanguageStrings(language: AppLanguage): LanguageStrings {
    return when (language.code) {
        "en" -> EnglishStrings
        "es" -> SpanishStrings
        else -> EnglishStrings
    }
}
