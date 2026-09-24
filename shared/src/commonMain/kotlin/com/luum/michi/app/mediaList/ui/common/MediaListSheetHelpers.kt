package com.luum.michi.app.mediaList.ui.common

/** Option id for the "any" season row (matches mediaListSeasonOptions). */
internal const val ANY_SEASON_ID = "any"

internal fun List<String>.toggled(value: String): List<String> =
    if (value in this) this - value else this + value
