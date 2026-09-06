package com.luum.michi.app.core.language

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.luum.michi.app.core.model.MediaReleaseDateTime

interface LanguageStrings {
    val languageLabel: String
    val logoutAction: String
    val notificationsAction: String
    val tabDiscover: String
    val tabAnime: String
    val tabManga: String
    val tabAccount: String
    val filterByLabel: String
    val orderByLabel: String
    val settingsAction: String
    val inUseLabel: String
    val discoverTrendingAnimeTitle: String
    val discoverTrendingMangaTitle: String
    val searchTitle: String
    val searchTypeAll: String
    val searchEmptyQueryHint: String
    val searchNoResultsLabel: String
    val discoverTitle: String
    val exploreThisSeasonTitle: String
    val exploreUpcomingNextSeasonTitle: String
    val exploreAllTimePopularAnimeTitle: String
    val exploreAllTimePopularMangaTitle: String
    val exploreTopAnimeTitle: String
    val exploreTopMangaTitle: String
    val exploreCharactersCategoryLabel: String
    val exploreStudiosCategoryLabel: String
    val exploreFilterTypeLabel: String
    val exploreFilterSeasonLabel: String
    val exploreFilterGenreLabel: String
    val exploreFilterFormatLabel: String
    val exploreFilterYearLabel: String
    val exploreAnySeasonLabel: String
    val exploreAnyYearLabel: String
    val exploreSeasonWinterLabel: String
    val exploreSeasonSpringLabel: String
    val exploreSeasonSummerLabel: String
    val exploreSeasonFallLabel: String
    val exploreFormatMovieLabel: String
    val exploreFormatSpecialLabel: String
    val exploreFormatMangaLabel: String
    val exploreFormatNovelLabel: String
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
    val mediaDetailTitle: String
    val mediaDetailOverviewTitle: String
    val mediaDetailDescriptionTitle: String
    val mediaDetailGenresTitle: String
    val mediaDetailStudiosTitle: String
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
    val mediaDetailTabOverview: String
    val mediaDetailTabConnections: String
    val mediaDetailTabCharacters: String
    val mediaDetailTabStaff: String
    val mediaDetailTabRecommendations: String
    val mediaDetailTabStats: String
    val mediaDetailCharacterRoleMain: String
    val mediaDetailCharacterRoleSupporting: String
    val mediaDetailCharacterRoleBackground: String
    val mediaDetailNoCharactersLabel: String
    val mediaDetailNoStaffLabel: String
    val mediaDetailNoRecommendationsLabel: String
    val mediaDetailLoadMoreAction: String
    val mediaDetailScoreDistributionTitle: String
    val mediaDetailStatusDistributionTitle: String
    val mediaStatsStatusCurrent: String
    val mediaStatsStatusPlanning: String
    val mediaStatsStatusCompleted: String
    val mediaStatsStatusDropped: String
    val mediaStatsStatusPaused: String
    val mediaStatsStatusRepeating: String
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
    val mediaDetailFormatLabel: String
    val mediaDetailStatusLabel: String
    val mediaDetailEpisodesLabel: String
    val mediaDetailChaptersLabel: String
    val mediaDetailVolumesLabel: String
    val mediaDetailDurationLabel: String
    val mediaDetailMeanScoreLabel: String
    val mediaDetailPopularityLabel: String
    val mediaDetailFavoritesLabel: String
    val mediaDetailSourceLabel: String
    val mediaDetailSeasonLabel: String
    val mediaDetailStartedLabel: String
    val mediaDetailEndedLabel: String
    val mediaDetailLoadingLabel: String
    val mediaDetailErrorLabel: String
    val mediaDetailAddToListAction: String
    val mediaDetailEditEntryAction: String
    val mediaDetailEditorTitleEdit: String
    val mediaDetailEditorSavingLabel: String
    val mediaDetailEditorSaveErrorLabel: String
    val mediaDetailEditorCancelAction: String
    val mediaDetailEditorDeleteAction: String
    val deleteEntryConfirmTitle: String
    val deleteEntryConfirmMessage: String
    val confirmDeleteAction: String
    val sectionAll: String
    val sectionWatching: String
    val sectionCurrent: String
    val sectionCompleted: String
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
    val dateNotSetLabel: String
    val datePickerOkAction: String
    val datePickerCancelAction: String
    val datePickerClearAction: String
    val todayLabel: String
    val totalRewatchesLabel: String
    val totalRereadsLabel: String
    val favouriteLabel: String
    val priorityLabel: String
    val privateLabel: String
    val hiddenFromStatusListsLabel: String
    val saveAction: String
    val entriesLabel: String
    fun episodesBehind(count: Int): String
    fun chaptersBehind(count: Int): String
    fun nextEpisodeReleaseLabel(episodeNumber: Int, releaseDateTime: MediaReleaseDateTime): String
    fun nextChapterReleaseLabel(chapterNumber: Int, releaseDateTime: MediaReleaseDateTime): String
    fun nextVolumeReleaseLabel(volumeNumber: Int, releaseDateTime: MediaReleaseDateTime): String
    fun notificationDateLabel(dateTime: MediaReleaseDateTime): String
    val accountAnimeLabel: String
    val accountMangaLabel: String
    val accountFollowersLabel: String
    val accountFollowingLabel: String
    val accountFavoriteAnimeTitle: String
    val accountFavoriteMangaTitle: String
    val accountFavoriteCharactersTitle: String
    val accountFavoriteStaffTitle: String
    val accountFavoriteStudiosTitle: String
    fun accountJoinedLabel(month: Int, year: Int): String
    val accountEditProfileAction: String
    val accountShareProfileAction: String
    val accountDownloadProfileQrAction: String
    val accountWebOnlyFieldsNote: String
    val accountUpdateProfileOnWebAction: String
    val accountManageAccountOnWebAction: String
    val accountSettingsTitle: String
    val accountListSettingsOnWebAction: String
    val accountImportListOnWebAction: String
    val accountStatsTitle: String
    val accountStatsOverviewLabel: String
    val accountStatsEpisodesWatchedLabel: String
    val accountStatsChaptersReadLabel: String
    val accountStatsDaysWatchedLabel: String
    val accountStatsVolumesReadLabel: String
    val accountStatsMeanScoreLabel: String
    val accountStatsStandardDeviationLabel: String
    val accountStatsScoreDistributionTitle: String
    val accountStatsFormatDistributionTitle: String
    val accountStatsStatusDistributionTitle: String
    val accountStatsTopGenresTitle: String
    val accountStatsEmptyLabel: String
    val accountFavoritesGridEmptyLabel: String
    val settingsAppSection: String
    val settingsAniListSection: String
    val settingsListsSection: String
    val settingsNotificationsSection: String
    val settingsAccountSection: String
    val settingsAboutSection: String
    val settingsThemeTitle: String
    val settingsThemeSubtitle: String
    val settingsThemeSystem: String
    val settingsThemeLight: String
    val settingsThemeDark: String
    val settingsLanguageTitle: String
    val settingsLanguageSubtitle: String
    val settingsDiscoverTabTitle: String
    val settingsDiscoverTabSubtitle: String
    val settingsTitleLanguageTitle: String
    val settingsTitleLanguageSubtitle: String
    val settingsTitleLanguageRomaji: String
    val settingsTitleLanguageEnglish: String
    val settingsTitleLanguageNative: String
    val settingsAdultContentTitle: String
    val settingsAdultContentSubtitle: String
    val settingsScoreFormatTitle: String
    val settingsScoreFormatSubtitle: String
    val settingsScoreFormatPoint100: String
    val settingsScoreFormatPoint10Decimal: String
    val settingsScoreFormatPoint10: String
    val settingsScoreFormatPoint5Stars: String
    val settingsScoreFormatPoint3Smileys: String
    val settingsListSortTitle: String
    val settingsListSortSubtitle: String
    val settingsListSortByTitle: String
    val settingsListSortByScore: String
    val settingsListSortByUpdated: String
    val settingsListSortByAdded: String
    val settingsListSortByRelease: String
    val settingsSplitCompletedAnimeTitle: String
    val settingsSplitCompletedAnimeSubtitle: String
    val settingsSplitCompletedMangaTitle: String
    val settingsSplitCompletedMangaSubtitle: String
    val settingsAdvancedScoringTitle: String
    val settingsAdvancedScoringSubtitle: String
    val settingsNotificationsSubtitle: String
    val settingsNotifAiringTitle: String
    val settingsNotifActivityTitle: String
    val settingsNotifFollowingTitle: String
    val settingsNotifForumTitle: String
    val settingsNotifMessagesTitle: String
    val settingsNotifMediaTitle: String
    val settingsManageAccountTitle: String
    val settingsManageAccountSubtitle: String
    val settingsHelpTitle: String
    val settingsHelpSubtitle: String
    val settingsAboutTitle: String
    val settingsAboutSubtitle: String
    val settingsAboutVersionLabel: String
    val settingsAboutCreditsLabel: String
    val backButton: String
    val authWelcomeTitle: String
    val authWelcomeSubtitle: String
    val authLoginAction: String
    val authConfigurationMissing: String
    val authLoadingLabel: String
    val listsLoadingLabel: String
    val listsEmptyLabel: String
    val listsErrorLabel: String
    val errorNoConnectionLabel: String
    val errorUnauthorizedLabel: String
    val errorRateLimitedLabel: String
    val errorServerLabel: String
    val errorUnknownLabel: String

    // Filter Sheet & Filter/Order Labels
    val filterPersistLabel: String
    val filterOrderDirectionTitle: String
    val filterSortCriterionTitle: String
    val filterResetAction: String
    val filterApplyAction: String
    val filterSaveAction: String

    val sortFollowList: String
    val sortTitle: String
    val sortScore: String
    val sortProgress: String
    val sortProgressManga: String
    val sortLastUpdated: String
    val sortLastAdded: String
    val sortStartDate: String
    val sortCompletedDate: String
    val sortReleaseDate: String
    val sortAverageScore: String
    val sortPopularity: String
    val sortFavorites: String
    val sortTrending: String
    val sortPriority: String
    val sortNextAiring: String

    val orderAscending: String
    val orderDescending: String

    val discoverSearchPlaceholder: String

    // Notifications
    val notificationsEmptyLabel: String
    val notificationsErrorLabel: String
    val notificationFilterAll: String
    val notificationFilterAiring: String
    val notificationFilterActivity: String
    val notificationFilterForum: String
    val notificationFilterFollows: String
    val notificationFilterMedia: String
    val notifAiringPrefix: String
    val notifAiringAired: String
    val notifFollowingLabel: String
    val notifActivityLabel: String
    val notifForumLabel: String
    val notifMessageLabel: String
    val notifMediaChangeLabel: String

    // Studio detail
    val studioDetailTitle: String
    val studioAnimeLabel: String
    val studioNoMediaLabel: String
    val sortByPopularity: String
    val sortByNewest: String
    val sortByOldest: String
    val sortByFavourites: String
    val sortByScore: String

    // Character detail
    val characterDetailTitle: String
    val characterTabOverview: String
    val characterTabMedia: String
    val characterNoMediaLabel: String
    val showSpoilersAction: String
    val hideSpoilersAction: String
    val alternativeNamesLabel: String
    val infoGenderLabel: String
    val infoAgeLabel: String
    val infoBirthdayLabel: String
    val infoBloodTypeLabel: String

    // Staff detail
    val staffDetailTitle: String
    val staffTabOverview: String
    val staffTabMedia: String
    val staffTabCharacters: String
    val infoDeathLabel: String
    val infoYearsActiveLabel: String
    val infoHometownLabel: String
    val infoOccupationsLabel: String
    val staffNoMediaLabel: String
    val staffNoCharactersLabel: String
    val yearsActivePresent: String
    val seeAllAction: String
}

val LocalLanguageStrings = staticCompositionLocalOf<LanguageStrings> { SpanishLanguageStrings }

fun getLanguageStrings(language: AppLanguage): LanguageStrings {
    return when (language.code) {
        "en" -> EnglishLanguageStrings
        "es" -> SpanishLanguageStrings
        else -> EnglishLanguageStrings
    }
}

object LanguageProvider {
    val strings: LanguageStrings
        @Composable
        @ReadOnlyComposable
        get() = LocalLanguageStrings.current
}

@Composable
fun ProvideLanguageStrings(
    language: AppLanguage,
    content: @Composable () -> Unit,
) {
    val strings = getLanguageStrings(language)
    CompositionLocalProvider(LocalLanguageStrings provides strings) {
        content()
    }
}
