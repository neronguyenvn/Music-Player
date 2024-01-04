package org.hyperskill.musicplayer.feature.music

import android.content.Context
import android.media.MediaPlayer
import org.hyperskill.musicplayer.R
import javax.inject.Inject
import javax.inject.Singleton

class MainMediaPlayer(private val context: Context) {

    private var player: MediaPlayer? = null
    val position: Int get() = player?.currentPosition ?: 0

    fun play() {
        MediaPlayer.create(context, R.raw.wisdom).apply {
            player = this
            start()
        }
    }

    fun stop() {
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

    fun onComplete(action: () -> Unit) {
        player?.setOnCompletionListener { action() }
    }
}
