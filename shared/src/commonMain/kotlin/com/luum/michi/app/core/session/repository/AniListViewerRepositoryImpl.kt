package com.luum.michi.app.core.session.repository

import com.luum.michi.app.core.network.domain.AniListGraphQLClient
import com.luum.michi.app.core.network.domain.AniListGraphQLRequest
import com.luum.michi.app.core.session.domain.AniListViewerRepository
import com.luum.michi.app.core.session.domain.Viewer
import com.luum.michi.app.core.network.repository.AniListJson
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.network.domain.map

private const val ViewerQuery = """
query Viewer {
  Viewer {
    id
    name
    about(asHtml: false)
    bannerImage
    avatar {
      large
      medium
    }
    createdAt
    donatorTier
    moderatorRoles
    options {
      titleLanguage
      staffNameLanguage
      displayAdultContent
    }
    mediaListOptions {
      scoreFormat
    }
  }
}
"""

internal class AniListViewerRepositoryImpl(
    private val graphQLClient: AniListGraphQLClient,
) : AniListViewerRepository {

    override suspend fun fetchViewer(): NetworkResult<Viewer> {
        val request = AniListGraphQLRequest(
            query = ViewerQuery,
            operationName = "Viewer",
        )
        return graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(ViewerResponseDto.serializer(), dataJson)
        }.map { it.viewer.toDomain() }
    }
}
