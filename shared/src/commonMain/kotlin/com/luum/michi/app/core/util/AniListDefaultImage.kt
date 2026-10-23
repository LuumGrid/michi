package com.luum.michi.app.core.util

/** AniList's shared "no image" art (media covers and people) always ends in `/default.jpg`. */
internal fun String.isAniListDefaultImage(): Boolean = endsWith("/default.jpg", ignoreCase = true)
