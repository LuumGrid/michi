package com.luum.michi.app.core.model

/** Una opción seleccionable dentro de una fila de chips o un grupo de filtro. */
internal data class FilterOption(
    val id: String,
    val label: String,
    /** Conteo opcional mostrado entre paréntesis (usado por Anime/Manga). */
    val count: Int? = null,
)
