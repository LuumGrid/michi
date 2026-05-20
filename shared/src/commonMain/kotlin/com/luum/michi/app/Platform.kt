package com.luum.michi.app

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform