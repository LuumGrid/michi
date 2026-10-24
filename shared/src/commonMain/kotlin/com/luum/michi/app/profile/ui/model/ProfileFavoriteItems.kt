package com.luum.michi.app.profile.ui.model

import com.luum.michi.app.profile.domain.model.ProfileFavorite
import com.luum.michi.app.ui.components.MediaRailItem
import com.luum.michi.app.ui.components.PersonRailItem
import com.luum.michi.app.ui.components.PersonRailPerson

/** A favourite as a media rail/grid card: name + cover + second line (a studio
 *  has no image, so its card never reaches a rail and is painted by the
 *  settings-style rows instead). */
internal fun ProfileFavorite.toRailItem(): MediaRailItem = MediaRailItem(
    id = id,
    title = name,
    image = image,
    subtitle = subtitle,
    placeholderHex = paletteHex,
)

/** A favourite as a simple person card: one person, no actor row. */
internal fun ProfileFavorite.toPersonItem(): PersonRailItem = PersonRailItem(
    primary = PersonRailPerson(
        id = id,
        name = name,
        image = image,
        subtitle = subtitle,
    ),
)
