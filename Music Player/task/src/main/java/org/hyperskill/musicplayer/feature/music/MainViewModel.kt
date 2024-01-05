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
import org.hyperskill.musicplayer.model.toIds
import org.hyperskill.musicplayer.model.toSelector
import org.hyperskill.musicplayer.model.toTrack
import javax.inject.Inject

val defaultTracks = (1..10).map {
    Item.Track(
        Song(
            id = it,
            artist = "artist$it",
            title = "title$it",
            duration = 215_000
        )
    )
}
val defaultPlaylist = Playlist("All Songs", defaultTracks)

data class MainActivityUiState(
    val currentPlayList: Playlist = Playlist(),
    val loadedPlaylist: List<Item.SongSelector> = emptyList(),
    val playlistList: List<Playlist> = emptyList(),
    val state: State = PLAY_MUSIC,
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

    private val _currentTrack: MutableStateFlow<Item.Track?> = MutableStateFlow(null)
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

    private val mainViewModelState get() = _mainUiState.value
    private val currentTrack get() = _currentTrack.value
    private var updatePositionJob: Job? = null

    fun onSearchClick() {
        addPlaylist(defaultPlaylist)
        if (mainViewModelState.state == PLAY_MUSIC) {
            _mainUiState.update {
                it.copy(currentPlayList = defaultPlaylist)
            }
            if (currentTrack == null) {
                _currentTrack.value = defaultTracks[0]
            }
        } else {
            _mainUiState.update {
                it.copy(loadedPlaylist = defaultPlaylist.tracks.map { song -> song.toSelector() })
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
        if (mainViewModelState.currentPlayList.tracks.isEmpty()) {
            return "no songs loaded, click search to load songs"
        }
        _mainUiState.update {
            it.copy(
                state = ADD_PLAYLIST,
                loadedPlaylist = defaultTracks.map { song -> song.toSelector() })
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
                    .map { selector -> selector.toTrack() }
            ))
        resetAddMusicState()
        _mainUiState.update { it.copy(state = PLAY_MUSIC) }
        return ""
    }

    fun onCancelClick() {
        resetAddMusicState()
        _mainUiState.update {
            it.copy(state = PLAY_MUSIC)
        }
    }

    private fun addPlaylist(playlist: Playlist) {
        if (mainViewModelState.playlistList.none { it.name == playlist.name }) {
            _mainUiState.update {
                it.copy(playlistList = it.playlistList + playlist)
            }
        }
    }

    private fun resetAddMusicState() {
        addPlaylistEtPlaylistName = ""
        _mainUiState.update {
            it.copy(loadedPlaylist = emptyList())
        }
    }
    //endregion

    //region Player Controller
    fun loadPlaylist(position: Int) {
        val newPlaylist = mainViewModelState.playlistList[position]
        when (mainViewModelState.state) {
            PLAY_MUSIC -> {
                val shouldKeepCurrentTrack = currentTrack?.id in newPlaylist.toIds()
                _mainUiState.update { it.copy(currentPlayList = newPlaylist) }
                if (!shouldKeepCurrentTrack) {
                    resetPlayMusicState()
                    _currentTrack.value = newPlaylist.tracks[0]
                } else {
                    updateStateOfTrackInPlaylist(currentTrack!!.id, Item.Track.TrackState.PLAYING)
                }
            }

            ADD_PLAYLIST -> {
                _mainUiState.update {
                    val includedInOldListAndSelectedIds = mainViewModelState.loadedPlaylist
                        .filter { selector ->
                            selector.id in newPlaylist.toIds() && selector.isSelected
                        }
                        .map { selector -> selector.id }
                    it.copy(
                        loadedPlaylist = it.playlistList[position].tracks.map { track ->
                            track.toSelector(track.id in includedInOldListAndSelectedIds)
                        }
                    )
                }
            }
        }
    }

    fun deletePlaylist(position: Int) {
        val thisPlaylist = mainViewModelState.playlistList[position]
        _mainUiState.update {
            it.copy(
                playlistList = it.playlistList.toMutableList().apply { remove(thisPlaylist) },
                currentPlayList = if (it.currentPlayList.toIds() == thisPlaylist.toIds()) {
                    defaultPlaylist
                } else it.currentPlayList,
                loadedPlaylist = if (it.loadedPlaylist.toIds() == thisPlaylist.toIds() &&
                    it.state == ADD_PLAYLIST
                ) {
                    defaultPlaylist.tracks.map { song -> song.toSelector() }
                } else it.loadedPlaylist
            )
        }
    }

    fun onPlayOrPauseClick(position: Int) {
        if (currentTrack != null &&
            currentTrack?.id == mainViewModelState.currentPlayList.tracks[position].id
        ) {
            switchCurrentTrackState()
            return
        }
        startTrack(mainViewModelState.currentPlayList.tracks[position])

    }

    fun onPlayOrPauseClick() {
        if (mainViewModelState.currentPlayList.tracks.isEmpty()) return
        if (currentTrack != null) {
            switchCurrentTrackState()
            return
        }
        startTrack(mainViewModelState.currentPlayList.tracks[0])
    }

    fun onStopClick() = resetPlayMusicState()

    fun onTouchStart() {
        updatePositionJob?.cancel()
        if (currentTrack?.state == Item.Track.TrackState.STOPPED) {
            mediaPlayer.createWithoutPlay { resetPlayMusicState() }
        }
    }

    fun onTouchStop(value: Int) {
        mediaPlayer.update(value)
        updatePosition()
        if (currentTrack?.state == Item.Track.TrackState.STOPPED) {
            _currentTrack.update { it?.copy(state = Item.Track.TrackState.PAUSED) }
        }
    }

    private fun switchCurrentTrackState() {
        when (currentTrack?.state) {
            Item.Track.TrackState.PLAYING -> {
                _currentTrack.update { it?.copy(state = Item.Track.TrackState.PAUSED) }
                updateStateOfTrackInPlaylist(currentTrack!!.id, Item.Track.TrackState.PAUSED)
                mediaPlayer.pause()
            }

            Item.Track.TrackState.PAUSED -> {
                _currentTrack.update { it?.copy(state = Item.Track.TrackState.PLAYING) }
                updateStateOfTrackInPlaylist(currentTrack!!.id, Item.Track.TrackState.PLAYING)
                mediaPlayer.resume()
            }

            Item.Track.TrackState.STOPPED -> {
                _currentTrack.update { it?.copy(state = Item.Track.TrackState.PLAYING) }
                updateStateOfTrackInPlaylist(currentTrack!!.id, Item.Track.TrackState.PLAYING)
                mediaPlayer.play { resetPlayMusicState() }
                updatePosition()
            }

            else -> throw UnsupportedOperationException()
        }
    }

    private fun startTrack(track: Item.Track) {
        resetPlayMusicState()
        _currentTrack.value = track.copy(state = Item.Track.TrackState.PLAYING)
        updateStateOfTrackInPlaylist(track.id, Item.Track.TrackState.PLAYING)
        mediaPlayer.play { resetPlayMusicState() }
        updatePosition()
    }

    private fun updatePosition() {
        updatePositionJob = viewModelScope.launch {
            _currentPosition.value = mediaPlayer.position
            delay(100)
            updatePosition()
        }
    }

    private fun updateStateOfTrackInPlaylist(trackId: Int, state: Item.Track.TrackState) {
        _mainUiState.update { currentState ->
            currentState.copy(
                currentPlayList = currentState.currentPlayList.copy(
                    tracks = currentState.currentPlayList.tracks.map { currentTrack ->
                        if (currentTrack.id == trackId) currentTrack.copy(
                            state = state
                        ) else if (state == Item.Track.TrackState.PLAYING) currentTrack.copy(
                            state = Item.Track.TrackState.STOPPED
                        ) else currentTrack
                    }
                ))
        }
    }

    private fun resetPlayMusicState() {
        _currentPosition.value = 0
        updatePositionJob?.cancel()
        _currentTrack.update { it?.copy(state = Item.Track.TrackState.STOPPED) }
        _mainUiState.update { currentState ->
            currentState.copy(
                currentPlayList = currentState.currentPlayList.copy(
                    tracks = currentState.currentPlayList.tracks.map { currentTrack ->
                        currentTrack.copy(state = Item.Track.TrackState.STOPPED)
                    }
                ))
        }
        mediaPlayer.stop()
    }
    //endregion
}