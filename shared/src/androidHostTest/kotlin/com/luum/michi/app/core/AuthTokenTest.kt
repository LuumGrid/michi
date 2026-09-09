package com.luum.michi.app.core

import com.luum.michi.app.core.auth.domain.AniListOAuthConfig
import com.luum.michi.app.core.auth.domain.AniListToken
import com.luum.michi.app.core.auth.domain.InMemoryAniListTokenStorage
import com.luum.michi.app.core.auth.domain.currentEpochSeconds
import com.luum.michi.app.core.auth.domain.parseAniListOAuthCallback
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val PREFIX = "michi://oauth/callback"

class AuthTokenTest {

    @Test
    fun validCallbackParsesToken() {
        val token = parseAniListOAuthCallback(
            "$PREFIX#access_token=abc&token_type=Bearer&expires_in=3600",
        )
        assertEquals("abc", token?.accessToken)
        assertEquals("Bearer", token?.tokenType)
        assertEquals(3600L, token?.expiresInSeconds)
    }

    @Test
    fun callbackWithoutFragmentIsRejected() {
        assertNull(parseAniListOAuthCallback(PREFIX))
        assertNull(parseAniListOAuthCallback("$PREFIX#"))
    }

    @Test
    fun callbackWithBlankTokenIsRejected() {
        assertNull(parseAniListOAuthCallback("$PREFIX#access_token=&token_type=Bearer"))
        assertNull(parseAniListOAuthCallback("https://other.app/cb#access_token=abc"))
    }

    @Test
    fun missingTokenTypeDefaultsToBearer() {
        val token = parseAniListOAuthCallback("$PREFIX#access_token=abc")
        assertEquals("Bearer", token?.tokenType)
        assertNull(token?.expiresInSeconds)
    }

    @Test
    fun malformedExpiresInYieldsNullExpiry() {
        val token = parseAniListOAuthCallback("$PREFIX#access_token=abc&expires_in=soon")
        assertEquals("abc", token?.accessToken)
        assertNull(token?.expiresInSeconds)
    }

    @Test
    fun authorizeUrlCarriesGrantParameters() {
        val url = AniListOAuthConfig.buildAuthorizeUrl()
        assertTrue(url.contains("response_type=token"))
        assertTrue(url.startsWith("https://"))
    }

    @Test
    fun inMemoryStorageRoundtrips() {
        val storage = InMemoryAniListTokenStorage()
        val token = AniListToken(accessToken = "abc", savedAtEpochSeconds = 1L)
        runBlocking {
            assertNull(storage.load())
            storage.save(token)
            assertEquals(token, storage.load())
            storage.clear()
            assertNull(storage.load())
        }
    }

    @Test
    fun tokenWithoutExpiryNeverExpires() {
        assertFalse(AniListToken(accessToken = "a", savedAtEpochSeconds = 1L).isExpired)
    }

    @Test
    fun oldTokenIsExpired() {
        assertTrue(
            AniListToken(accessToken = "a", savedAtEpochSeconds = 1L, expiresInSeconds = 3600L).isExpired,
        )
    }

    @Test
    fun freshTokenIsNotExpired() {
        assertFalse(
            AniListToken(
                accessToken = "a",
                savedAtEpochSeconds = currentEpochSeconds(),
                expiresInSeconds = 3600L,
            ).isExpired,
        )
    }
}
