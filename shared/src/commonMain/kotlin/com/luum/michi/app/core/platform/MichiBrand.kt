package com.luum.michi.app.core.platform

const val MichiAppName = "Michi"

/**
 * Nombres de marca de Michi. No se traducen entre idiomas: son identificadores
 * comerciales. Centralizados aquí para que renombrar una sección sea un cambio
 * de un solo lugar.
 */
enum class MichiBrand(
    val appName: String,
    val postsLabel: String = "Posts",
    val animationLabel: String = "Animations",
    val readingLabel: String = "Readings",
) {
    ANIMATION(
        appName = "Animations",
        postsLabel = "Posts",
        animationLabel = "Anime",
        readingLabel = "Manga",
    ),
    READING(
        appName = "Readings",
        postsLabel = "Posts",
        animationLabel = "Anime",
        readingLabel = "Manga",
    ),
}
