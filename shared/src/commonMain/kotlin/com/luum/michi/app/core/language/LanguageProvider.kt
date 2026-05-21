package com.luum.michi.app.core.language

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

interface LanguageStrings {
    val languageLabel: String
    val logoutAction: String
    val notificationsAction: String
    val tabAccount: String
    val settingsAction: String
    val inUseLabel: String
    val addAccountAction: String
    val accountPostsLabel: String
    val accountFollowersLabel: String
    val accountFollowingLabel: String
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
    val settingsAccountSection: String
    val settingsManageAccountTitle: String
    val settingsManageAccountSubtitle: String
    val settingsPrivacyTitle: String
    val settingsPrivacySubtitle: String
    val settingsSecurityTitle: String
    val settingsSecuritySubtitle: String
    val settingsContentSection: String
    val settingsContentPreferencesTitle: String
    val settingsContentPreferencesSubtitle: String
    val settingsHistoryTitle: String
    val settingsHistorySubtitle: String
    val settingsInteractionsTitle: String
    val settingsInteractionsSubtitle: String
    val settingsExperienceSection: String
    val settingsNotificationsSubtitle: String
    val settingsLanguageTitle: String
    val settingsLanguageSubtitle: String
    val settingsSubtitlesTitle: String
    val settingsSubtitlesSubtitle: String
    val settingsAccessibilityTitle: String
    val settingsAccessibilitySubtitle: String
    val settingsDarkModeTitle: String
    val settingsDarkModeEnabledSubtitle: String
    val settingsLightModeEnabledSubtitle: String
    val settingsDataPlaybackTitle: String
    val settingsDataPlaybackSubtitle: String
    val settingsToolsSection: String
    val settingsCreatorToolsTitle: String
    val settingsCreatorToolsSubtitle: String
    val settingsHelpTitle: String
    val settingsHelpSubtitle: String
    val settingsAboutTitle: String
    val settingsAboutSubtitle: String
    val backButton: String
}

object LanguageProvider {
    val strings: LanguageStrings
        @Composable
        @ReadOnlyComposable
        get() = LocalLanguageStrings.current
}

val LocalLanguageStrings = staticCompositionLocalOf<LanguageStrings> { SpanishLanguageStrings }

@Composable
fun ProvideLanguageStrings(
    language: AppLanguage,
    content: @Composable () -> Unit,
) {
    val strings = when (language.code) {
        "en" -> EnglishLanguageStrings
        else -> SpanishLanguageStrings
    }
    CompositionLocalProvider(LocalLanguageStrings provides strings) {
        content()
    }
}

private object SpanishLanguageStrings : LanguageStrings {
    override val languageLabel = "Idioma"
    override val logoutAction = "Cerrar sesion"
    override val notificationsAction = "Notificaciones"
    override val tabAccount = "Cuenta"
    override val settingsAction = "Configuracion"
    override val inUseLabel = "en uso"
    override val addAccountAction = "Agregar cuenta"
    override val accountPostsLabel = "Publicaciones"
    override val accountFollowersLabel = "Seguidores"
    override val accountFollowingLabel = "Seguidos"
    override val accountEditProfileAction = "Editar perfil"
    override val accountShareProfileAction = "Compartir perfil"
    override val accountDownloadProfileQrAction = "Descargar"
    override val accountChangeProfilePhotoAction = "Cambiar foto de perfil"
    override val accountEditNameLabel = "Nombre"
    override val accountEditUsernameLabel = "Nombre de usuario"
    override val accountEditBioLabel = "Presentacion"
    override val accountEditLinksLabel = "Enlaces"
    override val accountAddLinkAction = "Agregar enlace"
    override val accountEditLinkTitleLabel = "Titulo"
    override val accountEditLinkUrlLabel = "URL"
    override val accountEditGenderLabel = "Genero"
    override val accountEditCustomGenderLabel = "Genero personalizado"
    override val accountGenderMale = "Male"
    override val accountGenderFemale = "Female"
    override val accountGenderPreferNotToSay = "Prefiero no decir"
    override val accountGenderCustom = "Custom"
    override val accountEditEmailLabel = "Correo electronico"
    override val accountEditBirthDateLabel = "Fecha de nacimiento"
    override val accountSelectDateAction = "Calendario"
    override val accountSelectDateConfirmAction = "Seleccionar"
    override val accountVisibilityPublic = "Publico"
    override val accountVisibilityPrivate = "Privado"
    override val accountVisibilitySubtitle = "Controla si este dato aparece en tu perfil"
    override val accountEditAvatarUrlLabel = "URL de foto"
    override val accountSaveProfileAction = "Guardar"
    override val settingsAccountSection = "Cuenta"
    override val settingsManageAccountTitle = "Administrar cuenta"
    override val settingsManageAccountSubtitle = "Informacion personal, seguridad, contrasena, acceso..."
    override val settingsPrivacyTitle = "Privacidad"
    override val settingsPrivacySubtitle = "Quien puede verte, etiquetarte y contactarte"
    override val settingsSecurityTitle = "Seguridad"
    override val settingsSecuritySubtitle = "Contrasena, sesiones y verificacion"
    override val settingsContentSection = "Contenido y actividad"
    override val settingsContentPreferencesTitle = "Preferencias de contenido"
    override val settingsContentPreferencesSubtitle = "Temas, recomendaciones y contenido sensible"
    override val settingsHistoryTitle = "Historial y vistos"
    override val settingsHistorySubtitle = "Busquedas, anime visto y actividad reciente"
    override val settingsInteractionsTitle = "Guardados e interacciones"
    override val settingsInteractionsSubtitle = "Likes, comentarios, favoritos y compartidos"
    override val settingsExperienceSection = "Experiencia"
    override val settingsNotificationsSubtitle = "Push, email, directos y menciones"
    override val settingsLanguageTitle = "Idioma y traduccion"
    override val settingsLanguageSubtitle = "Idioma de la app, traducciones y subtitulos"
    override val settingsSubtitlesTitle = "Subtitulaje"
    override val settingsSubtitlesSubtitle = "Preferencias de subtitulos y traducciones"
    override val settingsAccessibilityTitle = "Accesibilidad"
    override val settingsAccessibilitySubtitle = "Subtitulos, lectura y controles visuales"
    override val settingsDarkModeTitle = "Modo oscuro"
    override val settingsDarkModeEnabledSubtitle = "La app usa colores oscuros"
    override val settingsLightModeEnabledSubtitle = "La app usa colores claros"
    override val settingsDataPlaybackTitle = "Uso de datos y reproduccion"
    override val settingsDataPlaybackSubtitle = "Autoplay, calidad y descarga en Wi-Fi"
    override val settingsToolsSection = "Herramientas"
    override val settingsCreatorToolsTitle = "Herramientas para creadores"
    override val settingsCreatorToolsSubtitle = "Panel, monetizacion y estado de contenido"
    override val settingsHelpTitle = "Ayuda y soporte"
    override val settingsHelpSubtitle = "Reportar un problema y centro de ayuda"
    override val settingsAboutTitle = "Acerca de Michi"
    override val settingsAboutSubtitle = "Politicas, version y terminos"
    override val backButton = "Volver"
}

private object EnglishLanguageStrings : LanguageStrings {
    override val languageLabel = "Language"
    override val logoutAction = "Log out"
    override val notificationsAction = "Notifications"
    override val tabAccount = "Account"
    override val settingsAction = "Settings"
    override val inUseLabel = "in use"
    override val addAccountAction = "Add account"
    override val accountPostsLabel = "Posts"
    override val accountFollowersLabel = "Followers"
    override val accountFollowingLabel = "Following"
    override val accountEditProfileAction = "Edit profile"
    override val accountShareProfileAction = "Share profile"
    override val accountDownloadProfileQrAction = "Download"
    override val accountChangeProfilePhotoAction = "Change profile photo"
    override val accountEditNameLabel = "Name"
    override val accountEditUsernameLabel = "Username"
    override val accountEditBioLabel = "Bio"
    override val accountEditLinksLabel = "Links"
    override val accountAddLinkAction = "Add link"
    override val accountEditLinkTitleLabel = "Title"
    override val accountEditLinkUrlLabel = "URL"
    override val accountEditGenderLabel = "Gender"
    override val accountEditCustomGenderLabel = "Custom gender"
    override val accountGenderMale = "Male"
    override val accountGenderFemale = "Female"
    override val accountGenderPreferNotToSay = "Prefer not to say"
    override val accountGenderCustom = "Custom"
    override val accountEditEmailLabel = "Email"
    override val accountEditBirthDateLabel = "Birth date"
    override val accountSelectDateAction = "Calendar"
    override val accountSelectDateConfirmAction = "Select"
    override val accountVisibilityPublic = "Public"
    override val accountVisibilityPrivate = "Private"
    override val accountVisibilitySubtitle = "Controls whether this appears on your profile"
    override val accountEditAvatarUrlLabel = "Photo URL"
    override val accountSaveProfileAction = "Save"
    override val settingsAccountSection = "Account"
    override val settingsManageAccountTitle = "Manage account"
    override val settingsManageAccountSubtitle = "Personal information, security, password, access..."
    override val settingsPrivacyTitle = "Privacy"
    override val settingsPrivacySubtitle = "Who can see you, tag you, and contact you"
    override val settingsSecurityTitle = "Security"
    override val settingsSecuritySubtitle = "Password, sessions, and verification"
    override val settingsContentSection = "Content and activity"
    override val settingsContentPreferencesTitle = "Content preferences"
    override val settingsContentPreferencesSubtitle = "Topics, recommendations, and sensitive content"
    override val settingsHistoryTitle = "History and watched"
    override val settingsHistorySubtitle = "Searches, watched anime, and recent activity"
    override val settingsInteractionsTitle = "Saved and interactions"
    override val settingsInteractionsSubtitle = "Likes, comments, favorites, and shares"
    override val settingsExperienceSection = "Experience"
    override val settingsNotificationsSubtitle = "Push, email, live, and mentions"
    override val settingsLanguageTitle = "Language and translation"
    override val settingsLanguageSubtitle = "App language, translations, and subtitles"
    override val settingsSubtitlesTitle = "Subtitles"
    override val settingsSubtitlesSubtitle = "Subtitle and translation preferences"
    override val settingsAccessibilityTitle = "Accessibility"
    override val settingsAccessibilitySubtitle = "Subtitles, reading, and visual controls"
    override val settingsDarkModeTitle = "Dark mode"
    override val settingsDarkModeEnabledSubtitle = "The app uses dark colors"
    override val settingsLightModeEnabledSubtitle = "The app uses light colors"
    override val settingsDataPlaybackTitle = "Data usage and playback"
    override val settingsDataPlaybackSubtitle = "Autoplay, quality, and Wi-Fi downloads"
    override val settingsToolsSection = "Tools"
    override val settingsCreatorToolsTitle = "Creator tools"
    override val settingsCreatorToolsSubtitle = "Dashboard, monetization, and content status"
    override val settingsHelpTitle = "Help and support"
    override val settingsHelpSubtitle = "Report a problem and visit the help center"
    override val settingsAboutTitle = "About Michi"
    override val settingsAboutSubtitle = "Policies, version, and terms"
    override val backButton = "Back"
}
