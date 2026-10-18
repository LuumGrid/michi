package com.luum.michi.app.profile

import com.luum.michi.app.core.favourite.domain.FavouriteTarget
import com.luum.michi.app.profile.domain.model.ProfileFavoritesCategory
import com.luum.michi.app.profile.repository.ProfileRepositoryImpl
import com.luum.michi.app.profile.ui.state.ProfileFavoritesGridStateHolder
import com.luum.michi.app.profile.ui.state.ProfileStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

private fun profileHolder(
    graphQL: FakeProfileGraphQL,
    favourite: FakeFavouriteRepository,
): ProfileStateHolder = ProfileStateHolder(
    ProfileRepositoryImpl(graphQL),
    favourite,
    CoroutineScope(Dispatchers.Unconfined),
)

private fun gridHolder(
    graphQL: FakeProfileGraphQL,
    favourite: FakeFavouriteRepository,
): ProfileFavoritesGridStateHolder = ProfileFavoritesGridStateHolder(
    ProfileRepositoryImpl(graphQL),
    favourite,
    CoroutineScope(Dispatchers.Unconfined),
)

/**
 * The shared favourite revision: a toggle made from a detail/editor leaves the
 * profile and the favourites grid stale, so their next open reloads. A removal
 * made in the grid keeps both fresh.
 */
class ProfileFavoritesStalenessTest {

    @Test
    fun anExternalFavouriteChangeReloadsTheProfileOnNextOpen() {
        val page = profileJson(anime = listOf(1 to "A"))
        val graphQL = FakeProfileGraphQL(listOf(page, page))
        val favourite = FakeFavouriteRepository()
        val holder = profileHolder(graphQL, favourite)

        holder.load(7)
        assertEquals(1, graphQL.calls)

        // A detail heart or the editor toggled somewhere else: the shared
        // revision advances, so the profile is stale on its next open.
        runBlocking { favourite.toggleFavourite(FavouriteTarget.Character(1)) }
        holder.load(7)

        assertEquals(2, graphQL.calls, "an external change bypasses the TTL")
    }

    @Test
    fun anExternalFavouriteChangeForgetsTheLoadedGrid() {
        val page = favouritesPage("anime", "A", hasNextPage = false)
        val graphQL = FakeProfileGraphQL(listOf(page, page))
        val favourite = FakeFavouriteRepository()
        val holder = gridHolder(graphQL, favourite)

        holder.load(7, ProfileFavoritesCategory.ANIME)
        assertEquals(1, graphQL.calls)

        runBlocking { favourite.toggleFavourite(FavouriteTarget.Character(1)) }
        holder.load(7, ProfileFavoritesCategory.ANIME)

        assertEquals(2, graphQL.calls, "the grid drops its in-memory page and refetches")
    }

    @Test
    fun ourOwnRemovalKeepsTheGridFresh() {
        val page = favouritesPageOf("anime", hasNextPage = false, 1 to "A", 2 to "B")
        val graphQL = FakeProfileGraphQL(listOf(page))
        val favourite = FakeFavouriteRepository()
        val holder = gridHolder(graphQL, favourite)

        holder.load(7, ProfileFavoritesCategory.ANIME)
        holder.removeFavorite(1)
        holder.load(7, ProfileFavoritesCategory.ANIME)

        assertEquals(1, graphQL.calls, "the grid's own removal is not an external change")
    }
}
