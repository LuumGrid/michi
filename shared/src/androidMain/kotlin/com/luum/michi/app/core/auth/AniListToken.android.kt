package com.luum.michi.app.core.auth

internal actual fun currentEpochSeconds(): Long = System.currentTimeMillis() / 1000L
