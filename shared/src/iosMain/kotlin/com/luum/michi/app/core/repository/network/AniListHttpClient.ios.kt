package com.luum.michi.app.core.repository.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

internal actual fun createAniListHttpClientEngine(): HttpClientEngine = Darwin.create()
