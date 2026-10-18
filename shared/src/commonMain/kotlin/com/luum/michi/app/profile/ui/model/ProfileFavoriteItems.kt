package com.luum.michi.app.profile.ui.model

import com.luum.michi.app.profile.domain.model.ProfileFavorite
import com.luum.michi.app.ui.components.MediaRailItem

/** A favourite as a rail/grid card: name + cover (a studio has no image, so its
 *  card never reaches a rail and is painted by the settings-style rows instead). */
internal fun ProfileFavorite.toRailItem(): MediaRailItem = MediaRailItem(
    id = id,
    title = name,
    image = image,
    placeholderHex = paletteHex,
)
