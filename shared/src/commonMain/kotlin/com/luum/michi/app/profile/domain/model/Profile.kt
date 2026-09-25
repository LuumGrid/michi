package com.luum.michi.app.profile.domain.model

internal data class ProfileDraft(
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val bannerUrl: String? = null,
    val bio: String,
)
