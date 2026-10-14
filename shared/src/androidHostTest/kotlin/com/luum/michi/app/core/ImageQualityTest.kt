package com.luum.michi.app.core

import com.luum.michi.app.core.model.ImageQuality
import com.luum.michi.app.core.model.resolved
import com.luum.michi.app.core.network.domain.AlwaysUnmeteredConnectivity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImageQualityTest {

    @Test
    fun autoFollowsTheConnection() {
        assertEquals(ImageQuality.HIGH, ImageQuality.AUTO.resolved(unmeteredConnection = true))
        assertEquals(ImageQuality.LOW, ImageQuality.AUTO.resolved(unmeteredConnection = false))
    }

    @Test
    fun explicitQualityIgnoresTheConnection() {
        assertEquals(ImageQuality.HIGH, ImageQuality.HIGH.resolved(unmeteredConnection = false))
        assertEquals(ImageQuality.LOW, ImageQuality.LOW.resolved(unmeteredConnection = true))
    }

    @Test
    fun defaultConnectivityAssumesUnmetered() {
        assertTrue(AlwaysUnmeteredConnectivity().isUnmetered.value)
    }
}
