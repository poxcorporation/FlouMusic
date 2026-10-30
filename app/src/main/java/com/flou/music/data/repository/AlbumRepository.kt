package com.flou.music.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Album(val id: Long, val name: String, val artist: String, val songCount: Int, val artUri: Uri?)

object AlbumRepository {
    suspend fun getAllAlbums(context: Context): List<Album> = withContext(Dispatchers.IO) {
        val map = mutableMapOf<Long, Triple<String, String, Int>>()
        val proj = arrayOf(MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.ARTIST)
        context.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj, null, null, null)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val artCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val name = c.getString(nameCol) ?: "Desconocido"
                val artist = c.getString(artCol) ?: "Desconocido"
                val existing = map[id]
                map[id] = Triple(name, artist, (existing?.third ?: 0) + 1)
            }
        }
        map.map { (id, triple) ->
            Album(id, triple.first, triple.second, triple.third,
                ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), id))
        }.sortedBy { it.name }
    }
}