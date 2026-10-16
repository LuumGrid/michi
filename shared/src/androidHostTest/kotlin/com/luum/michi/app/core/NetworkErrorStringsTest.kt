package com.luum.michi.app.core

import com.luum.michi.app.core.language.domain.EnglishStrings
import com.luum.michi.app.core.language.domain.SpanishStrings
import com.luum.michi.app.core.language.domain.networkErrorMessage
import com.luum.michi.app.core.network.domain.NetworkError
import kotlin.test.Test
import kotlin.test.assertEquals

class NetworkErrorStringsTest {

    @Test
    fun notFoundUsesAGenericMessageThatNamesNoEntity() {
        val error = NetworkError.NotFound(entity = "Media", id = 5)
        assertEquals("This no longer exists on AniList", EnglishStrings.networkErrorMessage(error))
        assertEquals("Esto ya no existe en AniList", SpanishStrings.networkErrorMessage(error))
    }

    @Test
    fun otherErrorsKeepTheirLabels() {
        assertEquals(
            EnglishStrings.errorServerLabel,
            EnglishStrings.networkErrorMessage(NetworkError.GraphQL(listOf("boom"))),
        )
    }
}
