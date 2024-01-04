package org.hyperskill.musicplayer.model

sealed class Item(open val song: Song) {

    data class Track(
        override val song: Song,
        val state: TrackState = TrackState.STOPPED,
    ) : Item(song) {
        enum class TrackState { PLAYING, PAUSED, STOPPED }
    }

    data class SongSelector(
        override val song: Song,
        val isSelected: Boolean,
    ) : Item(song)

    val id get() = song.id
}

data class Song(
    val id: Int,
    val title: String,
    val artist: String,
    val duration: Int
)

fun Item.Track.toSelector(isSelected: Boolean = false): Item.SongSelector {
    return Item.SongSelector(
        song = this.song,
        isSelected = isSelected
    )
}

fun Item.SongSelector.toTrack(): Item.Track {
    return Item.Track(this.song)
}

