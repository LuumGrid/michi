package com.luum.michi.app.shell.state

internal sealed interface DetailDestination {
    data class Media(val id: Int) : DetailDestination
    data class Character(val id: Int) : DetailDestination
    data class Staff(val id: Int) : DetailDestination
    data class Studio(val id: Int) : DetailDestination
}
