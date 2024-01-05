package org.hyperskill.musicplayer.feature.music

import android.content.Context
import android.media.MediaPlayer
import org.hyperskill.musicplayer.R

class MainMediaPlayer(private val context: Context) {

    private var player: MediaPlayer? = null
    val position: Int get() = player?.currentPosition ?: 0

    fun play(action: () -> Unit) {
        MediaPlayer.create(context, R.raw.wisdom).apply {
            player = this
            start()
            setOnCompletionListener { action() }
        }
    }

    fun createWithoutPlay(action: () -> Unit) {
        MediaPlayer.create(context, R.raw.wisdom).apply {
            player = this
            setOnCompletionListener { action() }
        }
    }

    fun stop() {
        player?.seekTo(0)
        player?.stop()
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
