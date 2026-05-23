package com.luum.michi.app.core.session

import com.luum.michi.app.core.network.AniListGraphQLClient
import com.luum.michi.app.core.network.AniListGraphQLRequest
import com.luum.michi.app.core.network.AniListJson
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.core.network.map

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
            AniListJson.decodeFromString(ViewerResponseDto.serializer(), dataJson)
        }.map { it.viewer.toDomain() }
    }
}
