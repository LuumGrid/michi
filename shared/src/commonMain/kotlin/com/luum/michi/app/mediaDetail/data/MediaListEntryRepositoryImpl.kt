package com.luum.michi.app.mediaDetail.data

import com.luum.michi.app.core.anilist.dto.MediaViewerListEntryDto
import com.luum.michi.app.core.network.AniListGraphQLClient
import com.luum.michi.app.core.network.AniListGraphQLRequest
import com.luum.michi.app.core.network.AniListJson
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.core.network.map
import com.luum.michi.app.mediaDetail.presentation.model.MediaDetailViewerEntry
import com.luum.michi.app.mediaDetail.presentation.model.MediaListStatus
import com.luum.michi.app.mediaDetail.presentation.model.parseMediaListStatus
import com.luum.michi.app.mediaDetail.presentation.model.toApiValue
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private const val SaveMediaListEntryMutation = """
mutation SaveMediaListEntry(
  ${'$'}mediaId: Int!,
  ${'$'}status: MediaListStatus,
  ${'$'}progress: Int,
  ${'$'}progressVolumes: Int,
  ${'$'}score: Float,
  ${'$'}notes: String,
  ${'$'}repeat: Int,
  ${'$'}private: Boolean,
  ${'$'}hiddenFromStatusLists: Boolean
) {
  SaveMediaListEntry(
    mediaId: ${'$'}mediaId,
    status: ${'$'}status,
    progress: ${'$'}progress,
    progressVolumes: ${'$'}progressVolumes,
    score: ${'$'}score,
    notes: ${'$'}notes,
    repeat: ${'$'}repeat,
    private: ${'$'}private,
    hiddenFromStatusLists: ${'$'}hiddenFromStatusLists
  ) {
    id status progress progressVolumes score notes repeat private hiddenFromStatusLists
  }
}
"""

@Serializable
private data class SaveMediaListEntryResponse(
    @SerialName("SaveMediaListEntry") val entry: MediaViewerListEntryDto? = null,
)

internal class MediaListEntryRepositoryImpl(
    private val graphQLClient: AniListGraphQLClient,
) : MediaListEntryRepository {

    override suspend fun saveEntry(
        mediaId: Int,
        status: MediaListStatus,
        progress: Int,
        progressVolumes: Int?,
        score: Float,
        notes: String,
        repeat: Int,
        isPrivate: Boolean,
        hiddenFromStatusLists: Boolean,
    ): NetworkResult<MediaDetailViewerEntry> {
        val variables = buildMap<String, kotlinx.serialization.json.JsonElement> {
            put("mediaId", JsonPrimitive(mediaId))
            put("status", JsonPrimitive(status.toApiValue()))
            put("progress", JsonPrimitive(progress))
            if (progressVolumes != null) put("progressVolumes", JsonPrimitive(progressVolumes))
            put("score", JsonPrimitive(score))
            put("notes", JsonPrimitive(notes))
            put("repeat", JsonPrimitive(repeat))
            put("private", JsonPrimitive(isPrivate))
            put("hiddenFromStatusLists", JsonPrimitive(hiddenFromStatusLists))
        }
        val request = AniListGraphQLRequest(
            query = SaveMediaListEntryMutation,
            variables = JsonObject(variables),
            operationName = "SaveMediaListEntry",
        )

        return graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromString(SaveMediaListEntryResponse.serializer(), dataJson)
        }.map { response ->
            val entry = response.entry
            MediaDetailViewerEntry(
                id = entry?.id ?: 0,
                status = parseMediaListStatus(entry?.status) ?: status,
                progress = entry?.progress ?: progress,
                progressVolumes = entry?.progressVolumes ?: progressVolumes,
                score = entry?.score?.toFloat() ?: score,
                notes = entry?.notes.orEmpty(),
                repeat = entry?.repeat ?: repeat,
                isPrivate = entry?.isPrivate ?: isPrivate,
                hiddenFromStatusLists = entry?.hiddenFromStatusLists ?: hiddenFromStatusLists,
            )
        }
    }
}
