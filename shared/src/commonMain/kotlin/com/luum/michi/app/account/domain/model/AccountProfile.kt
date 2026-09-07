package com.luum.michi.app.account.domain.model

internal data class AccountProfileDraft(
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val bannerUrl: String? = null,
    val bio: String,
)
