package com.luum.michi.app.core.network.repository

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun createAniListHttpClientEngine(): HttpClientEngine = OkHttp.create()
