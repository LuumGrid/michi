package com.luum.michi.app.mediaList.ui.common

/**
 * The list's view settings, grouped so the anime/manga screens and the scaffold
 * take them as one value instead of six parameters. Built once per tab in Root
 * (remembered). The sheet open flags live outside on purpose: they flip on
 * every open/close and are not "how the list looks".
 */
internal data class MediaListScreenSettings(
    val query: String = "",
    val sortPersist: Boolean = false,
    val splitCompleted: Boolean = true,
    val hideAdult: Boolean = false,
    val isSmileyFormat: Boolean = false,
    val formatSmiley: (Float) -> String = { "" },
)
