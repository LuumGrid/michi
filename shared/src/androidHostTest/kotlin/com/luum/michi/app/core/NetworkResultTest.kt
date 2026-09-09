package com.luum.michi.app.core

import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.network.domain.map
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class NetworkResultTest {

    @Test
    fun mapTransformsSuccess() {
        val result = NetworkResult.Success(2).map { it * 10 }
        assertEquals(NetworkResult.Success(20), result)
    }

    @Test
    fun mapPassesFailureThroughUntouched() {
        val failure: NetworkResult<Int> =
            NetworkResult.Failure(NetworkError.GraphQL(listOf("Bad")))
        assertSame(failure, failure.map { it * 10 })
    }
}
