package com.luum.michi.app.account.presentation.model

internal data class AccountProfileDraft(
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val bannerUrl: String? = null,
    val bio: String,
    val links: List<AccountProfileLink> = emptyList(),
    val gender: AccountProfileGender = AccountProfileGender.PREFER_NOT_TO_SAY,
    val customGender: String = "",
    val email: String = "",
    val isEmailPublic: Boolean = false,
    val birthDate: String = "",
    val isBirthDatePublic: Boolean = false,
)

internal data class AccountProfileLink(
    val title: String,
    val url: String,
)

internal enum class AccountProfileGender {
    MALE,
    FEMALE,
    PREFER_NOT_TO_SAY,
    CUSTOM,
}
