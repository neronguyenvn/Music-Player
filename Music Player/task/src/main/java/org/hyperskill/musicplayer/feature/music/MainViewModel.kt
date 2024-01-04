package org.hyperskill.musicplayer.feature.music

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

    private val _mainUiState = MutableStateFlow(MainActivityUiState())
    val mainUiState = _mainUiState.asStateFlow()

    var addPlaylistEtPlaylistName = ""

    private val mainUiStateValue get() = mainUiState.value
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
        if (mainUiStateValue.state == PLAY_MUSIC) {
            _mainUiState.update {
                it.copy(currentPlayList = defaultPlaylist)
            }
        } else {
            _mainUiState.update {
                it.copy(loadedPlaylist = defaultPlaylist.songs.map { it.toSelector() })
            }
        }
    }

    //region Add A Playlist
    fun onItemLongClick(position: Int) {
        onMenuAddClick()
        _mainUiState.update {
            it.copy(
                loadedPlaylist = it.loadedPlaylist.toMutableList().apply {
                    this[position] = this[position].copy(isSelected = true)
                }
            )
        }
    }

    fun onMenuAddClick(): String {
        if (mainUiStateValue.currentPlayList.songs.isEmpty()) {
            return "no songs loaded, click search to load songs"
        }
        _mainUiState.update {
            it.copy(
                state = ADD_PLAYLIST,
                loadedPlaylist = defaultSongs.map { song -> song.toSelector() })
        }
        return ""
    }

    fun onItemCheck(isChecked: Boolean, position: Int) {
        _mainUiState.update {
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
        if (mainUiStateValue.loadedPlaylist.none { it.isSelected }) return "Add at least one song to your playlist"
        if (addPlaylistEtPlaylistName.isBlank()) return "Add a name to your playlist"
        if (addPlaylistEtPlaylistName == "All Songs") return "All Songs is a reserved name choose another playlist name"
        addPlaylist(
            Playlist(
                addPlaylistEtPlaylistName,
                mainUiStateValue.loadedPlaylist.filter { it.isSelected }
                    .map { selector -> selector.song }
            ))
        addPlaylistEtPlaylistName = ""
        _mainUiState.update { it.copy(state = PLAY_MUSIC) }
        return ""
    }

    fun onCancelClick() {
        addPlaylistEtPlaylistName = ""
        _mainUiState.update {
            it.copy(
                state = PLAY_MUSIC,
                loadedPlaylist = emptyList()
            )
        }
    }

    private fun addPlaylist(playlist: Playlist) {
        if (mainUiStateValue.playlistList.none { it.name == playlist.name }) {
            _mainUiState.update {
                it.copy(playlistList = it.playlistList + playlist)
            }
        }
    }
    //endregion

    fun loadPlaylist(position: Int) {
        val newPlaylist = mainUiStateValue.playlistList[position]
        if (mainUiStateValue.state == PLAY_MUSIC) {
            val shouldKeepCurrentTrack =
                mainUiStateValue.currentTrack?.song in mainUiStateValue.playlistList[position].songs
            _mainUiState.update {
                it.copy(
                    currentTrack = if (shouldKeepCurrentTrack) it.currentTrack else null,
                    currentPlayList = newPlaylist
                )
            }
        } else {
            _mainUiState.update {

                val includedInOldListAndSelectedIds = mainUiStateValue.loadedPlaylist
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
        val thisPlaylist = mainUiStateValue.playlistList[position]
        _mainUiState.update {
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

    fun onPlayOrPauseClick(position: Int) {
        if (mainUiStateValue.currentTrack != null &&
            mainUiStateValue.currentPlayList.songs[position].id == mainUiStateValue.currentTrack!!.song.id
        ) {
            switchCurrentTrackState()
            return
        }

        _mainUiState.update {
            it.copy(
                currentTrack = Item.Track(
                    song = mainUiStateValue.currentPlayList.songs[position],
                    state = Item.Track.TrackState.PLAYING
                )
            )
        }
    }

    fun onPlayOrPauseClick() {
        if (mainUiStateValue.currentPlayList.songs.isEmpty()) return
        if (mainUiStateValue.currentTrack != null) {
            switchCurrentTrackState()
            return
        }
        _mainUiState.update {
            it.copy(
                currentTrack = Item.Track(
                    song = mainUiStateValue.currentPlayList.songs[0],
                    state = Item.Track.TrackState.PLAYING
                )
            )
        }
    }

    fun onStopClick() {
        _mainUiState.update { it.copy(currentTrack = null) }
    }

    private fun switchCurrentTrackState() {
        _mainUiState.update {
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