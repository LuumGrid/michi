package com.luum.michi.app.core

import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.session.domain.SessionState
import com.luum.michi.app.core.session.domain.Viewer
import com.luum.michi.app.core.session.domain.loadIfAuthenticated
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun viewer() = Viewer(id = 7, name = "user")

class AuthenticatedLoadTest {

    @Test
    fun authenticatedLoadsWithViewerId() {
        var loadedId: Int? = null
        val ran = runBlocking {
            loadIfAuthenticated(SessionState.Authenticated(viewer())) { loadedId = it }
        }
        assertTrue(ran)
        assertEquals(7, loadedId)
    }

    @Test
    fun anonymousSkipsLoad() {
        var ran = false
        val loaded = runBlocking {
            loadIfAuthenticated(SessionState.Anonymous) { ran = true }
        }
        assertFalse(loaded)
        assertFalse(ran)
    }

    @Test
    fun loadingSkipsLoad() {
        var ran = false
        val loaded = runBlocking {
            loadIfAuthenticated(SessionState.Loading) { ran = true }
        }
        assertFalse(loaded)
        assertFalse(ran)
    }

    @Test
    fun errorSkipsLoad() {
        var ran = false
        val loaded = runBlocking {
            loadIfAuthenticated(SessionState.Error(NetworkError.Http(500, null))) { ran = true }
        }
        assertFalse(loaded)
        assertFalse(ran)
    }
}
