package com.luum.michi.app.mediaDetail.data

import com.luum.michi.app.core.anilist.dto.MediaViewerListEntryDto
import com.luum.michi.app.core.media.CalendarDateParts
import com.luum.michi.app.core.media.millisToCalendarParts
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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

private const val SaveMediaListEntryMutation = """
mutation SaveMediaListEntry(
  ${'$'}mediaId: Int!,
  ${'$'}status: MediaListStatus,
  ${'$'}progress: Int,
  ${'$'}progressVolumes: Int,
  ${'$'}score: Float,
  ${'$'}notes: String,
  ${'$'}repeat: Int,
  ${'$'}priority: Int,
  ${'$'}private: Boolean,
  ${'$'}hiddenFromStatusLists: Boolean,
  ${'$'}startedAt: FuzzyDateInput,
  ${'$'}completedAt: FuzzyDateInput
) {
  SaveMediaListEntry(
    mediaId: ${'$'}mediaId,
    status: ${'$'}status,
    progress: ${'$'}progress,
    progressVolumes: ${'$'}progressVolumes,
    score: ${'$'}score,
    notes: ${'$'}notes,
    repeat: ${'$'}repeat,
    priority: ${'$'}priority,
    private: ${'$'}private,
    hiddenFromStatusLists: ${'$'}hiddenFromStatusLists,
    startedAt: ${'$'}startedAt,
    completedAt: ${'$'}completedAt
  ) {
    id status progress progressVolumes score notes repeat priority private hiddenFromStatusLists
    startedAt { year month day }
    completedAt { year month day }
  }
}
"""

private fun fuzzyDateInput(millis: Long?): JsonElement {
    if (millis == null) {
        return buildJsonObject {
            put("year", JsonNull)
            put("month", JsonNull)
            put("day", JsonNull)
        }
    }
    val parts = millisToCalendarParts(millis)
    return buildJsonObject {
        put("year", JsonPrimitive(parts.year))
        put("month", JsonPrimitive(parts.month))
        put("day", JsonPrimitive(parts.day))
    }
}

@Serializable
private data class SaveMediaListEntryResponse(
    @SerialName("SaveMediaListEntry") val entry: MediaViewerListEntryDto? = null,
)

private const val ToggleFavouriteMutation = """
mutation ToggleFavourite(${'$'}animeId: Int, ${'$'}mangaId: Int) {
  ToggleFavourite(animeId: ${'$'}animeId, mangaId: ${'$'}mangaId) {
    anime { nodes { id } }
    manga { nodes { id } }
  }
}
"""

@Serializable
private data class ToggleFavouriteResponse(
    @SerialName("ToggleFavourite") val payload: JsonElement? = null,
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
        priority: Int,
        isPrivate: Boolean,
        hiddenFromStatusLists: Boolean,
        startedAtMillis: Long?,
        completedAtMillis: Long?,
    ): NetworkResult<MediaDetailViewerEntry> {
        val variables = buildMap<String, JsonElement> {
            put("mediaId", JsonPrimitive(mediaId))
            put("status", JsonPrimitive(status.toApiValue()))
            put("progress", JsonPrimitive(progress))
            if (progressVolumes != null) put("progressVolumes", JsonPrimitive(progressVolumes))
            put("score", JsonPrimitive(score))
            put("notes", JsonPrimitive(notes))
            put("repeat", JsonPrimitive(repeat))
            put("priority", JsonPrimitive(priority))
            put("private", JsonPrimitive(isPrivate))
            put("hiddenFromStatusLists", JsonPrimitive(hiddenFromStatusLists))
            put("startedAt", fuzzyDateInput(startedAtMillis))
            put("completedAt", fuzzyDateInput(completedAtMillis))
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
                priority = entry?.priority ?: priority,
                isPrivate = entry?.isPrivate ?: isPrivate,
                hiddenFromStatusLists = entry?.hiddenFromStatusLists ?: hiddenFromStatusLists,
                startedAtMillis = entry?.startedAt?.let {
                    val y = it.year; val m = it.month; val d = it.day
                    if (y != null && m != null && d != null) com.luum.michi.app.core.media.calendarPartsToMillis(
                        CalendarDateParts(y, m, d)
                    ) else null
                } ?: startedAtMillis,
                completedAtMillis = entry?.completedAt?.let {
                    val y = it.year; val m = it.month; val d = it.day
                    if (y != null && m != null && d != null) com.luum.michi.app.core.media.calendarPartsToMillis(
                        CalendarDateParts(y, m, d)
                    ) else null
                } ?: completedAtMillis,
            )
        }
    }

    override suspend fun toggleFavourite(mediaId: Int, isManga: Boolean): NetworkResult<Unit> {
        val variables = buildMap<String, JsonElement> {
            if (isManga) put("mangaId", JsonPrimitive(mediaId))
            else put("animeId", JsonPrimitive(mediaId))
        }
        val request = AniListGraphQLRequest(
            query = ToggleFavouriteMutation,
            variables = JsonObject(variables),
            operationName = "ToggleFavourite",
        )
        return graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromString(ToggleFavouriteResponse.serializer(), dataJson)
        }.map { }
    }
}
