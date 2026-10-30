package com.flou.music.data.repository

import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Artist(val name: String, val songCount: Int)

object ArtistRepository {
    suspend fun getAllArtists(context: Context): List<Artist> = withContext(Dispatchers.IO) {
        val map = mutableMapOf<String, Int>()
        val proj = arrayOf(MediaStore.Audio.Media.ARTIST)
        context.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj, null, null, null)?.use { c ->
            val col = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            while (c.moveToNext()) {
                val artist = c.getString(col) ?: "Desconocido"
                map[artist] = map.getOrDefault(artist, 0) + 1
            }
        }
        map.map { Artist(it.key, it.value) }.sortedBy { it.name }
    }
}