package com.luum.michi.app.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * AniList GraphQL response envelope. Either `data` or `errors` (or both) are
 * present in any response.
 */
@Serializable
internal data class AniListGraphQLEnvelope(
    val data: JsonElement? = null,
    val errors: List<AniListGraphQLError>? = null,
)

@Serializable
internal data class AniListGraphQLError(
    val message: String,
    val status: Int? = null,
)
