package org.hyperskill.musicplayer.model

import java.nio.channels.Selector

data class Playlist(
    val name: String = "",
    val tracks: List<Item.Track> = emptyList()
)

fun Playlist.toIds() = this.tracks.map { it.id }
fun List<Item.SongSelector>.toIds() = this.map { it.id }
