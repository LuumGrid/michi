package com.luum.michi.app.core.repository.session

import com.luum.michi.app.core.domain.network.AniListGraphQLClient
import com.luum.michi.app.core.domain.network.AniListGraphQLRequest
import com.luum.michi.app.core.domain.session.AniListViewerRepository
import com.luum.michi.app.core.domain.session.Viewer
import com.luum.michi.app.core.repository.network.AniListJson
import com.luum.michi.app.core.domain.network.NetworkResult
import com.luum.michi.app.core.domain.network.map

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
