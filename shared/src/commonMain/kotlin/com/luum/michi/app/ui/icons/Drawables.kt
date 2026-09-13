@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package com.luum.michi.app.ui.icons

import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ResourceItem

/**
 * Hand-written drawable references mirroring the generated `Res.drawable.*`
 * accessors one-to-one (same ids, same packaged paths).
 *
 * Why not the generated ones: under `androidMultiplatformLibrary` the
 * plugin-generated `commonMainResourceAccessors` source set never reaches
 * the Android compilation, so `Res.drawable.*` is invisible from common
 * code (verified via build logs + K2 fragment validation). These constants
 * use only the public `DrawableResource`/`ResourceItem` API, so they keep
 * working regardless of generator wiring. If the plugin wiring ever gets
 * fixed, swapping back is mechanical.
 *
 * Grouped like [AppIcons]: toolbar primitives first, tab destinations after.
 */
private const val MD: String = "composeResources/com.luum.michi.app.resources/"

private fun drawable(name: String): DrawableResource = DrawableResource(
    "drawable:$name",
    setOf(ResourceItem(setOf(), "${MD}drawable/$name.xml", -1, -1)),
)

internal object LocalDrawables {
    // ---- Toolbar ----
    val search: DrawableResource by lazy { drawable("search") }
    val keyboardArrowLeft: DrawableResource by lazy { drawable("keyboard_arrow_left") }
    val close: DrawableResource by lazy { drawable("close") }
    val settings: DrawableResource by lazy { drawable("settings") }

    // ---- TabBar ----
    val explore: DrawableResource by lazy { drawable("explore") }
    val tv: DrawableResource by lazy { drawable("tv") }
    val manga: DrawableResource by lazy { drawable("manga") }
    val person: DrawableResource by lazy { drawable("person") }
}
