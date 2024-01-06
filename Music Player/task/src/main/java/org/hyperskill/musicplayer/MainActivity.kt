package org.hyperskill.musicplayer

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.hyperskill.musicplayer.adapter.ItemsAdapter
import org.hyperskill.musicplayer.databinding.ActivityMainBinding
import org.hyperskill.musicplayer.feature.music.MainActivityUiState
import org.hyperskill.musicplayer.feature.music.MainAddPlaylistFragment
import org.hyperskill.musicplayer.feature.music.MainPlayerControllerFragment
import org.hyperskill.musicplayer.feature.music.MainViewModel

private const val READ_EXTERNAL_STORAGE_PERMISSION_CODE = 1

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val viewModel: MainViewModel by viewModels()
    private var previousState = MainActivityUiState.State.PLAY_MUSIC

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        supportFragmentManager.beginTransaction()
            .add(
                R.id.mainFragmentContainer,
                MainPlayerControllerFragment(viewModel),
            )
            .addToBackStack(null)
            .commit()
        binding.mainButtonSearch.setOnClickListener {
            if (!isReadStoragePermissionGranted(this)) {
                requestReadStoragePermission(this)
            } else {
                val result = viewModel.onSearchClick()
                if (result.isNotEmpty()) {
                    Toast.makeText(this, result, Toast.LENGTH_SHORT).show()
                }
            }
        }
        /*        val itemAdapter = ItemsAdapter(
            onTrackPlayOrPause = { position -> viewModel.onPlayOrPauseClick(position) },
            onTrackLongClick = { position -> viewModel.onItemLongClick(position) },
            onSongSelectorClick = { isChecked, position ->
                viewModel.onItemCheck(isChecked, position)
            }
        )*/
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
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.mainUiState.collect {
                    when (it.state) {
                        MainActivityUiState.State.PLAY_MUSIC -> {
                            itemAdapter.update(it.currentPlayList.tracks)
                            /*
                                                itemAdapter.submitList(it.currentPlayList.tracks)
                        */
                            if (it.state != previousState) {
                                supportFragmentManager.beginTransaction()
                                    .replace(
                                        R.id.mainFragmentContainer,
                                        MainPlayerControllerFragment(viewModel),
                                    )
                                    .addToBackStack(null)
                                    .commit()
                                previousState = MainActivityUiState.State.PLAY_MUSIC
                            }
                        }

                        MainActivityUiState.State.ADD_PLAYLIST -> {
                            itemAdapter.update(it.loadedPlaylist)
                            /*
                                                itemAdapter.submitList(it.loadedPlaylist)
                        */
                            if (it.state != previousState) {
                                supportFragmentManager.beginTransaction()
                                    .replace(
                                        R.id.mainFragmentContainer,
                                        MainAddPlaylistFragment(viewModel),
                                    )
                                    .addToBackStack(null)
                                    .commit()
                                previousState = MainActivityUiState.State.ADD_PLAYLIST
                            }
                        }
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
                        viewModel.mainUiState.value.playlistList.toMutableList()
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


    private fun isReadStoragePermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestReadStoragePermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            READ_EXTERNAL_STORAGE_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            READ_EXTERNAL_STORAGE_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    val result = viewModel.onSearchClick()
                    if (result.isNotEmpty()) {
                        Toast.makeText(this, result, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Songs cannot be loaded without permission",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}