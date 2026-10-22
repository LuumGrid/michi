package com.luum.michi.app.profile

import com.luum.michi.app.profile.domain.model.ProfileFavorite
import com.luum.michi.app.profile.ui.model.toPersonItem
import com.luum.michi.app.profile.ui.model.toRailItem
import com.luum.michi.app.ui.components.PersonRailItem
import com.luum.michi.app.ui.components.PersonRailPerson
import kotlin.test.Test
import kotlin.test.assertEquals

/** The profile's category items keep their second line when they become cards. */
class ProfileFavoriteItemsTest {

    @Test
    fun aFavoriteMapsToASimplePersonCard() {
        val favorite = ProfileFavorite(1, "Levi", null, null, "SnK")

        assertEquals(
            PersonRailItem(PersonRailPerson(1, "Levi", null, "SnK")),
            favorite.toPersonItem(),
        )
    }

    @Test
    fun theSecondLineReachesTheMediaCard() {
        val favorite = ProfileFavorite(1, "A", null, null, "TV · 2013")

        assertEquals("TV · 2013", favorite.toRailItem().subtitle)
    }
}
