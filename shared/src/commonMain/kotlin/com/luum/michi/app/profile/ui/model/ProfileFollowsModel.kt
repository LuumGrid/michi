package com.luum.michi.app.profile.ui.model

import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.profile.domain.model.ProfileFollow
import com.luum.michi.app.profile.domain.model.ProfileFollowsKind

/** Toolbar title of a follows list. */
internal fun ProfileFollowsKind.title(strings: LanguageStrings): String = when (this) {
    ProfileFollowsKind.FOLLOWING -> strings.profileFollowingLabel
    ProfileFollowsKind.FOLLOWERS -> strings.profileFollowersLabel
}

/** Canonical AniList profile URL of a followed user. */
internal fun ProfileFollow.aniListUrl(): String = "https://anilist.co/user/$name"
