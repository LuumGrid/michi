package com.luum.michi.app.core.model

/** A person's external link (biography anchors): display [label] + [url]. */
internal data class PersonLink(
    val label: String,
    val url: String,
)
