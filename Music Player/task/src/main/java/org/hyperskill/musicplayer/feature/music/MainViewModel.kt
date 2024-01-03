package org.hyperskill.musicplayer.feature.music

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.hyperskill.musicplayer.feature.music.MainActivityUiState.State.ADD_PLAYLIST
import org.hyperskill.musicplayer.feature.music.MainActivityUiState.State.PLAY_MUSIC
import org.hyperskill.musicplayer.model.Item
import org.hyperskill.musicplayer.model.Playlist
import org.hyperskill.musicplayer.model.Song
import org.hyperskill.musicplayer.model.toSelector

data class MainActivityUiState(
    val currentTrack: Item.Track? = null,
    val currentPlayList: Playlist = Playlist(),
    val loadedPlaylist: List<Item.SongSelector> = emptyList(),
    val playlistList: List<Playlist> = emptyList(),
    val state: State = PLAY_MUSIC
) {
    enum class State { PLAY_MUSIC, ADD_PLAYLIST }
}

class MainViewModel : ViewModel() {

    private val _viewModelState = MutableStateFlow(MainActivityUiState())
    val uiState = _viewModelState.asStateFlow()

    var addPlaylistEtPlaylistName = ""

    private val viewModelState get() = uiState.value
    private val defaultSongs = (1..10).map {
        Song(
            id = it,
            artist = "artist$it",
            title = "title$it",
            duration = 215_000
        )
    }
    private val defaultPlaylist = Playlist("All Songs", defaultSongs)

    fun onSearchClick() {
        addPlaylist(defaultPlaylist)
        if (viewModelState.state == PLAY_MUSIC) {
            _viewModelState.update {
                it.copy(currentPlayList = defaultPlaylist)
            }
        } else {
            _viewModelState.update {
                it.copy(loadedPlaylist = defaultPlaylist.songs.map { it.toSelector() })
            }
        }
    }

    //region Add A Playlist
    fun onItemLongClick(position: Int) {
        onMenuAddClick()
        _viewModelState.update {
            it.copy(
                loadedPlaylist = it.loadedPlaylist.toMutableList().apply {
                    this[position] = this[position].copy(isSelected = true)
                }
            )
        }
    }

    fun onMenuAddClick(): String {
        if (viewModelState.currentPlayList.songs.isEmpty()) {
            return "no songs loaded, click search to load songs"
        }
        _viewModelState.update {
            it.copy(
                state = ADD_PLAYLIST,
                loadedPlaylist = defaultSongs.map { song -> song.toSelector() })
        }
        return ""
    }

    fun onItemCheck(isChecked: Boolean, position: Int) {
        _viewModelState.update {
            it.copy(
                loadedPlaylist = it.loadedPlaylist.toMutableList().apply {
                    this[position] = this[position].copy(isSelected = isChecked)
                }
            )
        }
    }

    fun onPlaylistNameChange(name: String) {
        addPlaylistEtPlaylistName = name
    }

    fun onAddOkClick(): String {
        if (viewModelState.loadedPlaylist.none { it.isSelected }) return "Add at least one song to your playlist"
        if (addPlaylistEtPlaylistName.isBlank()) return "Add a name to your playlist"
        if (addPlaylistEtPlaylistName == "All Songs") return "All Songs is a reserved name choose another playlist name"
        addPlaylist(
            Playlist(
                addPlaylistEtPlaylistName,
                viewModelState.loadedPlaylist.filter { it.isSelected }
                    .map { selector -> selector.song }
            ))
        _viewModelState.update { it.copy(state = PLAY_MUSIC) }
        return ""
    }

    fun onCancelClick() {
        _viewModelState.update {
            it.copy(
                state = PLAY_MUSIC,
                loadedPlaylist = emptyList()
            )
        }
    }

    private fun addPlaylist(playlist: Playlist) {
        if (viewModelState.playlistList.none { it.name == playlist.name }) {
            _viewModelState.update {
                it.copy(playlistList = it.playlistList + playlist)
            }
        }
    }
    //endregion

    fun loadPlaylist(position: Int) {
        val newPlaylist = viewModelState.playlistList[position]
        if (viewModelState.state == PLAY_MUSIC) {
            val shouldKeepCurrentTrack =
                viewModelState.currentTrack?.song in viewModelState.playlistList[position].songs
            _viewModelState.update {
                it.copy(
                    currentTrack = if (shouldKeepCurrentTrack) it.currentTrack else null,
                    currentPlayList = newPlaylist
                )
            }
        } else {
            _viewModelState.update {

                val includedInOldListAndSelectedIds = viewModelState.loadedPlaylist
                    .filter { selector -> selector.song in newPlaylist.songs && selector.isSelected }
                    .map { selector -> selector.song.id }
                it.copy(
                    loadedPlaylist = it.playlistList[position].songs.map { song ->
                        song.toSelector(
                            song.id in includedInOldListAndSelectedIds
                        )
                    }
                )
            }
        }
    }

    fun deletePlaylist(position: Int) {
        val thisPlaylist = viewModelState.playlistList[position]
        _viewModelState.update {
            it.copy(
                playlistList = it.playlistList.toMutableList().apply { remove(thisPlaylist) },
                currentPlayList = if (it.currentPlayList == thisPlaylist) defaultPlaylist else it.currentPlayList,
                loadedPlaylist = if (it.loadedPlaylist.map { selector -> selector.song } ==
                    thisPlaylist.songs && it.state == ADD_PLAYLIST) {
                    defaultPlaylist.songs.map { song -> song.toSelector() }
                } else it.loadedPlaylist
            )
        }
    }

    fun playOrPause(position: Int) {
        if (viewModelState.currentTrack != null &&
            viewModelState.currentPlayList.songs[position].id == viewModelState.currentTrack!!.song.id
        ) {
            switchCurrentTrackState()
            return
        }

        _viewModelState.update {
            it.copy(
                currentTrack = Item.Track(
                    song = viewModelState.currentPlayList.songs[position],
                    state = Item.Track.TrackState.PLAYING
                )
            )
        }
    }

    fun playPause() {
        if (viewModelState.currentPlayList.songs.isEmpty()) return
        if (viewModelState.currentTrack != null) {
            switchCurrentTrackState()
            return
        }
        _viewModelState.update {
            it.copy(
                currentTrack = Item.Track(
                    song = viewModelState.currentPlayList.songs[0],
                    state = Item.Track.TrackState.PLAYING
                )
            )
        }
    }

    fun stop() {
        _viewModelState.update { it.copy(currentTrack = null) }
    }

    private fun switchCurrentTrackState() {
        _viewModelState.update {
            it.copy(
                currentTrack = it.currentTrack?.copy(
                    state = if (it.currentTrack.state == Item.Track.TrackState.PLAYING) {
                        Item.Track.TrackState.PAUSED
                    } else Item.Track.TrackState.PLAYING
                )
            )
        }
    }
}