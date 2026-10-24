package com.luum.michi.app.core.auth.domain

internal actual fun currentEpochSeconds(): Long = System.currentTimeMillis() / 1000L
