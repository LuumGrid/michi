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
    val tabHome: String
    val tabAnimation: String
    val tabReading: String
    val tabAccount: String
    val filterAction: String
    val settingsAction: String
    val inUseLabel: String
    val addAccountAction: String
    val discoveryTitle: String
    val discoveryDescription: String
    val homeGreetingTitle: String
    val homeGreetingSubtitle: String
    val homeSearchPlaceholder: String
    val homeSeasonalAction: String
    val homeExploreAction: String
    val homeReviewsAction: String
    val homeCalendarAction: String
    val homeReleasingTodayTitle: String
    val homeTrendingAnimationTitle: String
    val homeTrendingReadingTitle: String
    val homeCommunityTitle: String
    val homeCommunitySubtitle: String
    val searchTitle: String
    val searchDescription: String
    val libraryTitle: String
    val libraryDescription: String
    val mediaDetailTitle: String
    val mediaDetailDescription: String
    val sectionAll: String
    val sectionWatching: String
    val sectionReading: String
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
    val sectionRereading: String
    val statusLabel: String
    val progressLabel: String
    val chaptersLabel: String
    val volumesLabel: String
    val scoreLabel: String
    val notesLabel: String
    val startedLabel: String
    val completedLabel: String
    val todayLabel: String
    val repeatLabel: String
    val privateLabel: String
    val hiddenFromStatusListsLabel: String
    val removeAction: String
    val saveAction: String
    val entriesLabel: String
    fun episodesBehind(count: Int): String
    fun chaptersBehind(count: Int): String
    fun nextEpisodeReleaseLabel(episodeNumber: Int, releaseDateTime: MediaReleaseDateTime): String
    fun nextChapterReleaseLabel(chapterNumber: Int, releaseDateTime: MediaReleaseDateTime): String
    val accountAnimeLabel: String
    val accountMangaLabel: String
    val accountFollowersLabel: String
    val accountFollowingLabel: String
    val accountFavoriteAnimeTitle: String
    val accountFavoriteMangaTitle: String
    val accountFavoriteCharactersTitle: String
    val accountFavoriteStaffTitle: String
    val accountFavoriteStudiosTitle: String
    val accountSeeMoreAction: String
    fun accountJoinedLabel(month: Int, year: Int): String
    val accountEditProfileAction: String
    val accountShareProfileAction: String
    val accountDownloadProfileQrAction: String
    val accountChangeProfilePhotoAction: String
    val accountEditNameLabel: String
    val accountEditUsernameLabel: String
    val accountEditBioLabel: String
    val accountEditLinksLabel: String
    val accountAddLinkAction: String
    val accountEditLinkTitleLabel: String
    val accountEditLinkUrlLabel: String
    val accountEditGenderLabel: String
    val accountEditCustomGenderLabel: String
    val accountGenderMale: String
    val accountGenderFemale: String
    val accountGenderPreferNotToSay: String
    val accountGenderCustom: String
    val accountEditEmailLabel: String
    val accountEditBirthDateLabel: String
    val accountSelectDateAction: String
    val accountSelectDateConfirmAction: String
    val accountVisibilityPublic: String
    val accountVisibilityPrivate: String
    val accountVisibilitySubtitle: String
    val accountEditAvatarUrlLabel: String
    val accountSaveProfileAction: String
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
    val settingsHomeTabTitle: String
    val settingsHomeTabSubtitle: String
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
}

val LocalLanguageStrings = staticCompositionLocalOf<LanguageStrings> { SpanishLanguageStrings }

fun getLanguageStrings(language: AppLanguage): LanguageStrings {
    return when (language.code) {
        "en" -> EnglishLanguageStrings
        else -> SpanishLanguageStrings
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
