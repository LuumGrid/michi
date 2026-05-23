package com.luum.michi.app.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
internal data class AniListGraphQLRequest(
    val query: String,
    val variables: JsonObject? = null,
    val operationName: String? = null,
)
