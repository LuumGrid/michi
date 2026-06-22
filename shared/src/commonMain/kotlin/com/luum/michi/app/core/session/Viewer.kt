package com.luum.michi.app.core.session

internal data class Viewer(
    val id: Int,
    val name: String,
    val avatarUrl: String? = null,
    val bannerUrl: String? = null,
    val about: String? = null,
    val createdAtEpochSeconds: Long? = null,
    val isDonator: Boolean = false,
    val isModerator: Boolean = false,
    val titleLanguage: String? = null,
    val staffNameLanguage: String? = null,
    val displayAdultContent: Boolean = false,
    val scoreFormat: String? = null,
)
