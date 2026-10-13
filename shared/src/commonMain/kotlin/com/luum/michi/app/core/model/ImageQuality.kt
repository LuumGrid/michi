package com.luum.michi.app.core.model

/** User-facing image quality: AUTO follows the connection; HIGH/LOW pin it. */
internal enum class ImageQuality { AUTO, HIGH, LOW }

/**
 * Resolves AUTO against the connection: unmetered (Wi-Fi/Ethernet) → HIGH,
 * metered (mobile data) → LOW. HIGH/LOW pass through.
 */
internal fun ImageQuality.resolved(unmeteredConnection: Boolean): ImageQuality = when (this) {
    ImageQuality.AUTO -> if (unmeteredConnection) ImageQuality.HIGH else ImageQuality.LOW
    ImageQuality.HIGH, ImageQuality.LOW -> this
}
