package org.hyperskill.musicplayer.feature.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.hyperskill.musicplayer.feature.music.MainActivityViewModelState.State.ADD_PLAYLIST
import org.hyperskill.musicplayer.feature.music.MainActivityViewModelState.State.PLAY_MUSIC
import org.hyperskill.musicplayer.model.Item
import org.hyperskill.musicplayer.model.Playlist
import org.hyperskill.musicplayer.model.Song
import org.hyperskill.musicplayer.model.toSelector

val defaultSongs = (1..10).map {
    Song(
        id = it,
        artist = "artist$it",
        title = "title$it",
        duration = 215_000
    )
}
val defaultPlaylist = Playlist("All Songs", defaultSongs)

data class MainActivityViewModelState(
    val currentPlayList: Playlist = Playlist(),
    val loadedPlaylist: List<Item.SongSelector> = emptyList(),
    val playlistList: List<Playlist> = emptyList(),
    val state: State = PLAY_MUSIC
) {
    enum class State { PLAY_MUSIC, ADD_PLAYLIST }
}

data class MainActivityUiState(
    val viewModelState: MainActivityViewModelState = MainActivityViewModelState(),
    val currentTrack: Item.Track? = null
)

data class PlayerControllerUiState(
    val currentTrack: Item.Track? = null,
    val currentPosition: Int = 0
)

class MainViewModel : ViewModel() {

    private val _currentTrack: MutableStateFlow<Item.Track?> = MutableStateFlow(null)
    private val _mainViewModelState = MutableStateFlow(MainActivityViewModelState())

    val mainUiState = combine(_mainViewModelState, _currentTrack) { viewModelState, track ->
        MainActivityUiState(
            viewModelState = viewModelState,
            currentTrack = track
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainActivityUiState()
    )

    private val _currentPosition = MutableStateFlow(0)
    val playerControllerUiState = combine(_currentPosition, _currentTrack) { position, track ->
        PlayerControllerUiState(
            currentPosition = position,
            currentTrack = track
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlayerControllerUiState()
    )


    var addPlaylistEtPlaylistName = ""

    private val mainViewModelState get() = _mainViewModelState.value
    private val currentTrack get() = _currentTrack.value

    fun onSearchClick() {
        addPlaylist(defaultPlaylist)
        if (mainViewModelState.state == PLAY_MUSIC) {
            _mainViewModelState.update {
                it.copy(currentPlayList = defaultPlaylist)
            }
        } else {
            _mainViewModelState.update {
                it.copy(loadedPlaylist = defaultPlaylist.songs.map { it.toSelector() })
            }
        }
    }

    //region Add A Playlist
    fun onItemLongClick(position: Int) {
        onMenuAddClick()
        _mainViewModelState.update {
            it.copy(
                loadedPlaylist = it.loadedPlaylist.toMutableList().apply {
                    this[position] = this[position].copy(isSelected = true)
                }
            )
        }
    }

    fun onMenuAddClick(): String {
        if (mainViewModelState.currentPlayList.songs.isEmpty()) {
            return "no songs loaded, click search to load songs"
        }
        _mainViewModelState.update {
            it.copy(
                state = ADD_PLAYLIST,
                loadedPlaylist = defaultSongs.map { song -> song.toSelector() })
        }
        return ""
    }

    fun onItemCheck(isChecked: Boolean, position: Int) {
        _mainViewModelState.update {
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
        if (mainViewModelState.loadedPlaylist.none { it.isSelected }) return "Add at least one song to your playlist"
        if (addPlaylistEtPlaylistName.isBlank()) return "Add a name to your playlist"
        if (addPlaylistEtPlaylistName == "All Songs") return "All Songs is a reserved name choose another playlist name"
        addPlaylist(
            Playlist(
                addPlaylistEtPlaylistName,
                mainViewModelState.loadedPlaylist.filter { it.isSelected }
                    .map { selector -> selector.song }
            ))
        addPlaylistEtPlaylistName = ""
        _mainViewModelState.update { it.copy(state = PLAY_MUSIC) }
        return ""
    }

    fun onCancelClick() {
        addPlaylistEtPlaylistName = ""
        _mainViewModelState.update {
            it.copy(
                state = PLAY_MUSIC,
                loadedPlaylist = emptyList()
            )
        }
    }

    private fun addPlaylist(playlist: Playlist) {
        if (mainViewModelState.playlistList.none { it.name == playlist.name }) {
            _mainViewModelState.update {
                it.copy(playlistList = it.playlistList + playlist)
            }
        }
    }
    //endregion

    fun loadPlaylist(position: Int) {
        val newPlaylist = mainViewModelState.playlistList[position]
        if (mainViewModelState.state == PLAY_MUSIC) {
            val shouldKeepCurrentTrack =
                currentTrack?.song in mainViewModelState.playlistList[position].songs
            _mainViewModelState.update { it.copy(currentPlayList = newPlaylist) }
            if (!shouldKeepCurrentTrack) changeCurrentTrack(null)
        } else {
            _mainViewModelState.update {

                val includedInOldListAndSelectedIds = mainViewModelState.loadedPlaylist
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
        val thisPlaylist = mainViewModelState.playlistList[position]
        _mainViewModelState.update {
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
        if (currentTrack != null &&
            mainViewModelState.currentPlayList.songs[position].id == currentTrack!!.song.id
        ) {
            switchCurrentTrackState()
            return
        }

        changeCurrentTrack(
            Item.Track(
                song = mainViewModelState.currentPlayList.songs[position],
                state = Item.Track.TrackState.PLAYING
            )
        )
    }

    fun onPlayOrPauseClick() {
        if (mainViewModelState.currentPlayList.songs.isEmpty()) return
        if (currentTrack != null) {
            switchCurrentTrackState()
            return
        }
        changeCurrentTrack(
            Item.Track(
                song = mainViewModelState.currentPlayList.songs[0],
                state = Item.Track.TrackState.PLAYING
            )
        )
    }

    fun onStopClick() = changeCurrentTrack(null)

    private fun switchCurrentTrackState() {
        _currentTrack.update {
            it?.copy(
                state = if (it.state == Item.Track.TrackState.PLAYING) {
                    Item.Track.TrackState.PAUSED
                } else Item.Track.TrackState.PLAYING
            )
        }
    }

    fun onControllerSeekBarChange(value: Int) {
        _currentPosition.value = value
    }

    private fun changeCurrentTrack(track: Item.Track?) {
        _currentTrack.value = track
        _currentPosition.value = 0
    }
}