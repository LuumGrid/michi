package com.luum.michi.app.core.language.domain

import com.luum.michi.app.core.network.domain.NetworkError

internal fun LanguageStrings.networkErrorMessage(error: NetworkError): String = when (error) {
    NetworkError.NoConnection    -> errorNoConnectionLabel
    is NetworkError.Unauthorized -> errorUnauthorizedLabel
    is NetworkError.RateLimited  -> errorRateLimitedLabel
    is NetworkError.Http         -> errorServerLabel
    is NetworkError.GraphQL      -> errorServerLabel
    is NetworkError.Unknown      -> errorUnknownLabel
}
