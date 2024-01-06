package org.hyperskill.musicplayer.feature.music

import android.content.ContentUris
import android.content.Context
import android.media.MediaPlayer

class MainMediaPlayer(private val context: Context) {

    private var player: MediaPlayer? = null
    val position: Int get() = player?.currentPosition ?: 0

    fun play(songId: Int, action: () -> Unit) {
        val songUri = ContentUris.withAppendedId(
            android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            songId.toLong()
        )
        MediaPlayer.create(context, songUri).apply {
            player = this
            start()
            setOnCompletionListener { action() }
        }
    }

    fun createWithoutPlay(songId: Int, action: () -> Unit) {
        val songUri = ContentUris.withAppendedId(
            android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            songId.toLong()
        )
        MediaPlayer.create(context, songUri).apply {
            player = this
            setOnCompletionListener { action() }
        }
    }

    fun stop() {
        player?.seekTo(0)
        player?.stop()
        player?.release()
        player = null
    }

    fun pause() {
        player?.pause()
    }

    fun resume() {
        player?.start()
    }

    fun update(value: Int) {
        player?.seekTo(value)
    }
}
