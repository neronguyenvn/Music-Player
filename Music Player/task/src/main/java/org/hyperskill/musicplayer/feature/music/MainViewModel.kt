package org.hyperskill.musicplayer.feature.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.hyperskill.musicplayer.feature.music.MainActivityUiState.State.ADD_PLAYLIST
import org.hyperskill.musicplayer.feature.music.MainActivityUiState.State.PLAY_MUSIC
import org.hyperskill.musicplayer.model.Item
import org.hyperskill.musicplayer.model.Playlist
import org.hyperskill.musicplayer.model.Song
import org.hyperskill.musicplayer.model.toSelector
import javax.inject.Inject

val defaultSongs = (1..10).map {
    Song(
        id = it,
        artist = "artist$it",
        title = "title$it",
        duration = 215_000
    )
}
val defaultPlaylist = Playlist("All Songs", defaultSongs)

data class MainActivityUiState(
    val currentPlayList: Playlist = Playlist(),
    val loadedPlaylist: List<Item.SongSelector> = emptyList(),
    val playlistList: List<Playlist> = emptyList(),
    val state: State = PLAY_MUSIC
) {
    enum class State { PLAY_MUSIC, ADD_PLAYLIST }
}

data class PlayerControllerUiState(
    val currentTrack: Item.Track? = null,
    val currentPosition: Int = 0
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val mediaPlayer: MainMediaPlayer
) : ViewModel() {

    private val _mainUiState = MutableStateFlow(MainActivityUiState())
    val mainUiState = _mainUiState.asStateFlow()

    private val _currentPosition = MutableStateFlow(0)
    private val _currentTrack: MutableStateFlow<Item.Track?> = MutableStateFlow(null)
    val playerControllerUiState = combine(_currentPosition, _currentTrack) { position, track ->
        PlayerControllerUiState(
            currentPosition = position,
            currentTrack = track
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PlayerControllerUiState()
    )


    var addPlaylistEtPlaylistName = ""

    private val mainViewModelState get() = _mainUiState.value
    private val currentTrack get() = _currentTrack.value
    private var updatePositionJob: Job? = null

    fun onSearchClick() {
        addPlaylist(defaultPlaylist)
        if (mainViewModelState.state == PLAY_MUSIC) {
            _mainUiState.update {
                it.copy(currentPlayList = defaultPlaylist)
            }
        } else {
            _mainUiState.update {
                it.copy(loadedPlaylist = defaultPlaylist.songs.map { song -> song.toSelector() })
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
        if (mainViewModelState.currentPlayList.songs.isEmpty()) {
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
        if (mainViewModelState.playlistList.none { it.name == playlist.name }) {
            _mainUiState.update {
                it.copy(playlistList = it.playlistList + playlist)
            }
        }
    }
    //endregion

    //region Player Controller
    fun loadPlaylist(position: Int) {
        val newPlaylist = mainViewModelState.playlistList[position]
        if (mainViewModelState.state == PLAY_MUSIC) {
            val shouldKeepCurrentTrack =
                currentTrack?.song in mainViewModelState.playlistList[position].songs
            _mainUiState.update { it.copy(currentPlayList = newPlaylist) }
            if (!shouldKeepCurrentTrack) startTrack(null)
        } else {
            _mainUiState.update {

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
        if (currentTrack != null &&
            mainViewModelState.currentPlayList.songs[position].id == currentTrack!!.song.id
        ) {
            switchCurrentTrackState()
            return
        }

        startTrack(
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
        startTrack(
            Item.Track(
                song = mainViewModelState.currentPlayList.songs[0],
                state = Item.Track.TrackState.PLAYING
            )
        )
    }

    fun onStopClick() {
        startTrack(null)
        mediaPlayer.stop()
    }

    fun onControllerSeekBarChange(value: Int) {
        _currentPosition.value = value
        updatePositionJob?.cancel()
    }

    fun onStopTouch(value: Int) {
        mediaPlayer.update(value)
        updatePosition()
    }

    private fun switchCurrentTrackState() {
        if (currentTrack?.state == Item.Track.TrackState.PLAYING) {
            _currentTrack.update { it?.copy(state = Item.Track.TrackState.PAUSED) }
            mediaPlayer.pause()
        } else {
            _currentTrack.update { it?.copy(state = Item.Track.TrackState.PLAYING) }
            mediaPlayer.resume()
        }
    }

    private fun startTrack(track: Item.Track?) {
        _currentTrack.value = track
        _currentPosition.value = 0
        mediaPlayer.stop()
        if (track != null) {
            mediaPlayer.play()
            mediaPlayer.onComplete {
                startTrack(null)
            }
            updatePosition()
        }
    }

    private fun updatePosition() {
        updatePositionJob = viewModelScope.launch {
            delay(1000)
            _currentPosition.value = mediaPlayer.position
            updatePosition()
        }
    }
    //endregion
}