package org.hyperskill.musicplayer.model

data class Playlist(
    val name: String = "",
    val songs: List<Song> = emptyList()
)

fun Playlist.toTrackList(currentTrack: Item.Track?): List<Item.Track> {
    return this.songs.map {
        if (currentTrack != null && it.id == currentTrack.song.id) {
            currentTrack
        } else Item.Track(it)
    }
}
