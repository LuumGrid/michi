package com.luum.michi.app.discover.domain.genre.model

import io.ktor.http.encodeURLParameter

/**
 * Percent-encodes the receiver as a URL query value. Delegates to Ktor so
 * non-ASCII goes through a real UTF-8 encoder instead of a hand-rolled one; a
 * space becomes `%20` (not `+`), which is what a query value in a path-style
 * link like `anilist.co/search/anime?genres=…` needs. Pure (no Compose).
 */
internal fun String.urlEncodeQueryValue(): String = encodeURLParameter(spaceToPlus = false)
