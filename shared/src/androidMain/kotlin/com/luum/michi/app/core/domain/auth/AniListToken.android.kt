package com.luum.michi.app.core.domain.auth

internal actual fun currentEpochSeconds(): Long = System.currentTimeMillis() / 1000L
