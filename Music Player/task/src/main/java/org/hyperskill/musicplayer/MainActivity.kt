package org.hyperskill.musicplayer

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import org.hyperskill.musicplayer.adapter.ItemsAdapter
import org.hyperskill.musicplayer.databinding.ActivityMainBinding
import org.hyperskill.musicplayer.feature.music.MainActivityViewModelState
import org.hyperskill.musicplayer.feature.music.MainAddPlaylistFragment
import org.hyperskill.musicplayer.feature.music.MainPlayerControllerFragment
import org.hyperskill.musicplayer.feature.music.MainViewModel
import org.hyperskill.musicplayer.model.toTrackList


class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val viewModel: MainViewModel by viewModels()

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.mainFragmentContainer, MainPlayerControllerFragment(viewModel))
                .commit()
        }
        binding.mainButtonSearch.setOnClickListener {
            viewModel.onSearchClick()
        }
        val itemAdapter = ItemsAdapter(
            mutableListOf(),
            onTrackPlayOrPause = { position -> viewModel.onPlayOrPauseClick(position) },
            onTrackLongClick = { position -> viewModel.onItemLongClick(position) },
            onSongSelectorClick = { isChecked, position ->
                viewModel.onItemCheck(isChecked, position)
            }
        )
        binding.mainSongList.adapter = itemAdapter
        binding.mainSongList.layoutManager = LinearLayoutManager(this)
        lifecycleScope.launch {
            viewModel.mainUiState.collect {
                when (it.viewModelState.state) {
                    MainActivityViewModelState.State.PLAY_MUSIC -> {
                        itemAdapter.update(it.viewModelState.currentPlayList.toTrackList(it.currentTrack))
                        supportFragmentManager.beginTransaction()
                            .replace(
                                R.id.mainFragmentContainer,
                                MainPlayerControllerFragment(viewModel)
                            )
                            .addToBackStack(null)
                            .commit()
                    }

                    MainActivityViewModelState.State.ADD_PLAYLIST -> {
                        itemAdapter.update(it.viewModelState.loadedPlaylist)
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.mainFragmentContainer, MainAddPlaylistFragment(viewModel))
                            .addToBackStack(null)
                            .commit()
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return when (item.itemId) {
            R.id.mainMenuAddPlaylist -> {
                val errorString = viewModel.onMenuAddClick()
                if (errorString.isNotEmpty()) Toast.makeText(
                    this, errorString,
                    Toast.LENGTH_SHORT
                ).show()
                true
            }

            R.id.mainMenuLoadPlaylist, R.id.mainMenuDeletePlaylist -> {
                AlertDialog.Builder(this)
                    .setTitle(
                        "choose playlist to ${
                            if (item.itemId == R.id.mainMenuLoadPlaylist)
                                "load" else "delete"
                        }"
                    )
                    .setItems(
                        viewModel.mainUiState.value.viewModelState.playlistList.toMutableList()
                            .apply { if (item.itemId == R.id.mainMenuDeletePlaylist && this.isNotEmpty()) removeFirst() }
                            .map { it.name }
                            .toTypedArray()) { _, pos ->

                        if (item.itemId == R.id.mainMenuLoadPlaylist) {
                            viewModel.loadPlaylist(pos)
                        } else viewModel.deletePlaylist(pos + 1)
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}