package com.luum.michi.app.settings.data

import com.luum.michi.app.core.network.AniListGraphQLClient
import com.luum.michi.app.core.network.AniListGraphQLRequest
import com.luum.michi.app.core.network.AniListJson
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.core.network.map
import com.luum.michi.app.settings.presentation.model.ListSort
import com.luum.michi.app.settings.presentation.model.NotificationPreferences
import com.luum.michi.app.settings.presentation.model.ScoreFormat
import com.luum.michi.app.settings.presentation.model.TitleLanguage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

private const val UserSettingsQuery = """
query UserSettings {
  Viewer {
    options { titleLanguage staffNameLanguage displayAdultContent airingNotifications notificationOptions { type enabled } }
    mediaListOptions { scoreFormat rowOrder animeList { splitCompletedSectionByFormat advancedScoringEnabled } mangaList { splitCompletedSectionByFormat } }
  }
}
"""

private const val UpdateUserMutation = """
mutation UpdateUser(${'$'}titleLanguage: UserTitleLanguage, ${'$'}displayAdultContent: Boolean, ${'$'}airingNotifications: Boolean, ${'$'}scoreFormat: ScoreFormat, ${'$'}rowOrder: String, ${'$'}notificationOptions: [NotificationOptionInput], ${'$'}splitCompletedAnime: Boolean, ${'$'}splitCompletedManga: Boolean, ${'$'}advancedScoringEnabled: Boolean) {
  UpdateUser(titleLanguage: ${'$'}titleLanguage, displayAdultContent: ${'$'}displayAdultContent, airingNotifications: ${'$'}airingNotifications, scoreFormat: ${'$'}scoreFormat, rowOrder: ${'$'}rowOrder, notificationOptions: ${'$'}notificationOptions, animeListOptions: { splitCompletedSectionByFormat: ${'$'}splitCompletedAnime, advancedScoringEnabled: ${'$'}advancedScoringEnabled }, mangaListOptions: { splitCompletedSectionByFormat: ${'$'}splitCompletedManga }) { id }
}
"""

@Serializable
private data class UserSettingsResponseDto(
    @SerialName("Viewer") val viewer: ViewerSettingsDto? = null,
)

@Serializable
private data class ViewerSettingsDto(
    val options: ViewerOptionsDto? = null,
    val mediaListOptions: ViewerMediaListOptionsDto? = null,
)

@Serializable
private data class ViewerOptionsDto(
    val titleLanguage: String? = null,
    val staffNameLanguage: String? = null,
    val displayAdultContent: Boolean? = null,
    val airingNotifications: Boolean? = null,
    val notificationOptions: List<ViewerNotificationOptionDto> = emptyList(),
)

@Serializable
private data class ViewerNotificationOptionDto(
    val type: String? = null,
    val enabled: Boolean = false,
)

@Serializable
private data class ViewerMediaListOptionsDto(
    val scoreFormat: String? = null,
    val rowOrder: String? = null,
    val animeList: ViewerAnimeListOptionsDto? = null,
    val mangaList: ViewerMangaListOptionsDto? = null,
)

@Serializable
private data class ViewerAnimeListOptionsDto(
    val splitCompletedSectionByFormat: Boolean? = null,
    val advancedScoringEnabled: Boolean? = null,
)

@Serializable
private data class ViewerMangaListOptionsDto(
    val splitCompletedSectionByFormat: Boolean? = null,
)

@Serializable
private data class UpdateUserResponseDto(
    @SerialName("UpdateUser") val user: UpdateUserPayloadDto? = null,
)

@Serializable
private data class UpdateUserPayloadDto(
    val id: Int? = null,
)

private fun TitleLanguage.toApiValue(): String = when (this) {
    TitleLanguage.ROMAJI -> "ROMAJI"
    TitleLanguage.ENGLISH -> "ENGLISH"
    TitleLanguage.NATIVE -> "NATIVE"
}

private fun String?.toTitleLanguage(): TitleLanguage = when (this) {
    "ENGLISH" -> TitleLanguage.ENGLISH
    "NATIVE" -> TitleLanguage.NATIVE
    else -> TitleLanguage.ROMAJI
}

private fun ScoreFormat.toApiValue(): String = when (this) {
    ScoreFormat.POINT_100 -> "POINT_100"
    ScoreFormat.POINT_10_DECIMAL -> "POINT_10_DECIMAL"
    ScoreFormat.POINT_10 -> "POINT_10"
    ScoreFormat.POINT_5_STARS -> "POINT_5"
    ScoreFormat.POINT_3_SMILEYS -> "POINT_3"
}

private fun String?.toScoreFormat(): ScoreFormat = when (this) {
    "POINT_100" -> ScoreFormat.POINT_100
    "POINT_10" -> ScoreFormat.POINT_10
    "POINT_5" -> ScoreFormat.POINT_5_STARS
    "POINT_3" -> ScoreFormat.POINT_3_SMILEYS
    else -> ScoreFormat.POINT_10_DECIMAL
}

// AniList has no dedicated "release date" rowOrder, so RELEASE falls back to the
// same rowOrder as UPDATED on save; on load, an unrecognized rowOrder maps to UPDATED.
private fun ListSort.toApiRowOrder(): String = when (this) {
    ListSort.TITLE -> "title"
    ListSort.SCORE -> "score"
    ListSort.UPDATED -> "updatedAt"
    ListSort.ADDED -> "id"
    ListSort.RELEASE -> "updatedAt"
}

private fun String?.toListSort(): ListSort = when (this) {
    "title" -> ListSort.TITLE
    "score" -> ListSort.SCORE
    "id" -> ListSort.ADDED
    else -> ListSort.UPDATED
}

/** AniList NotificationType values grouped under each [NotificationPreferences] bucket. */
private val AiringTypes = listOf("AIRING")
private val ActivityTypes = listOf(
    "ACTIVITY_LIKE",
    "ACTIVITY_REPLY",
    "ACTIVITY_REPLY_LIKE",
    "ACTIVITY_MENTION",
    "ACTIVITY_REPLY_SUBSCRIBED",
)
private val FollowingTypes = listOf("FOLLOWING")
private val ForumTypes = listOf(
    "THREAD_COMMENT_MENTION",
    "THREAD_SUBSCRIBED",
    "THREAD_COMMENT_REPLY",
    "THREAD_LIKE",
    "THREAD_COMMENT_LIKE",
)
private val MessagesTypes = listOf("ACTIVITY_MESSAGE")
private val MediaTypes = listOf(
    "RELATED_MEDIA_ADDITION",
    "MEDIA_DATA_CHANGE",
    "MEDIA_MERGE",
    "MEDIA_DELETION",
)

private fun ViewerOptionsDto?.toNotificationPreferences(): NotificationPreferences {
    val byType = this?.notificationOptions.orEmpty().associate { it.type to it.enabled }
    fun bucketEnabled(types: List<String>, default: Boolean): Boolean {
        if (byType.isEmpty()) return default
        val relevant = types.mapNotNull { byType[it] }
        if (relevant.isEmpty()) return default
        return relevant.any { it }
    }
    return NotificationPreferences(
        airing = this?.airingNotifications ?: bucketEnabled(AiringTypes, default = true),
        activity = bucketEnabled(ActivityTypes, default = true),
        following = bucketEnabled(FollowingTypes, default = true),
        forum = bucketEnabled(ForumTypes, default = false),
        messages = bucketEnabled(MessagesTypes, default = true),
        media = bucketEnabled(MediaTypes, default = true),
    )
}

private fun notificationOptionsPayload(preferences: NotificationPreferences): List<Pair<String, Boolean>> =
    buildList {
        AiringTypes.forEach { add(it to preferences.airing) }
        ActivityTypes.forEach { add(it to preferences.activity) }
        FollowingTypes.forEach { add(it to preferences.following) }
        ForumTypes.forEach { add(it to preferences.forum) }
        MessagesTypes.forEach { add(it to preferences.messages) }
        MediaTypes.forEach { add(it to preferences.media) }
    }

internal class SettingsRepositoryImpl(
    private val graphQLClient: AniListGraphQLClient,
) : SettingsRepository {

    override suspend fun loadSettings(): NetworkResult<SettingsData> {
        val request = AniListGraphQLRequest(
            query = UserSettingsQuery,
            operationName = "UserSettings",
        )

        return graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(UserSettingsResponseDto.serializer(), dataJson)
        }.map { response ->
            val viewer = response.viewer
            val options = viewer?.options
            val listOptions = viewer?.mediaListOptions
            SettingsData(
                titleLanguage = options?.titleLanguage.toTitleLanguage(),
                scoreFormat = listOptions?.scoreFormat.toScoreFormat(),
                displayAdultContent = options?.displayAdultContent ?: false,
                listSort = listOptions?.rowOrder.toListSort(),
                splitCompletedAnime = listOptions?.animeList?.splitCompletedSectionByFormat ?: true,
                splitCompletedManga = listOptions?.mangaList?.splitCompletedSectionByFormat ?: false,
                advancedScoring = listOptions?.animeList?.advancedScoringEnabled ?: false,
                notifications = options.toNotificationPreferences(),
            )
        }
    }

    override suspend fun saveSettings(data: SettingsData): NetworkResult<Unit> {
        val variables = buildJsonObject {
            put("titleLanguage", data.titleLanguage.toApiValue())
            put("displayAdultContent", data.displayAdultContent)
            put("airingNotifications", data.notifications.airing)
            put("scoreFormat", data.scoreFormat.toApiValue())
            put("rowOrder", data.listSort.toApiRowOrder())
            put("splitCompletedAnime", data.splitCompletedAnime)
            put("splitCompletedManga", data.splitCompletedManga)
            put("advancedScoringEnabled", data.advancedScoring)
            putJsonArray("notificationOptions") {
                notificationOptionsPayload(data.notifications).forEach { (type, enabled) ->
                    add(
                        buildJsonObject {
                            put("type", type)
                            put("enabled", enabled)
                        },
                    )
                }
            }
        }

        val request = AniListGraphQLRequest(
            query = UpdateUserMutation,
            variables = variables,
            operationName = "UpdateUser",
        )

        return graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(UpdateUserResponseDto.serializer(), dataJson)
        }.map { }
    }
}
