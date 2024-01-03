package org.hyperskill.musicplayer.model

sealed class Item {

    data class Track(
        val song: Song,
        val state: TrackState = TrackState.STOPPED
    ) : Item() {
        enum class TrackState { PLAYING, PAUSED, STOPPED }
    }

    data class SongSelector(
        val song: Song,
        val isSelected: Boolean
    ) : Item()
}

data class Song(
    val id: Int,
    val title: String,
    val artist: String,
    val duration: Int
)

fun Song.toSelector(isSelected: Boolean = false): Item.SongSelector {
    return Item.SongSelector(
        song = this,
        isSelected = isSelected
    )
}



