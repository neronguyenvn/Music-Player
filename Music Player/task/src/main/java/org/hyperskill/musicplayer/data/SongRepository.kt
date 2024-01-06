package org.hyperskill.musicplayer.data

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import org.hyperskill.musicplayer.model.Song
import javax.inject.Inject

class SongRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun getSongsFromDevice(): List<Song> {
        val uri =
            if (Build.VERSION.SDK_INT >= 29) {
                MediaStore.Audio.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL
                )
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DURATION
        )
        val query = context.contentResolver.query(
            uri, projection,
            null, null, null
        )
        val songs = mutableListOf<Song>()
        query?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getInt(idColumn)
                val artist = cursor.getString(artistColumn)
                val title = cursor.getString(titleColumn)
                val duration = cursor.getInt(durationColumn)
                songs.add(Song(id = id, artist = artist, title = title, duration = duration))
            }
        }
        return songs
    }
}