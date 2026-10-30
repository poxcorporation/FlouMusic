package com.flou.music.data.repository

import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Genre(val id: Long, val name: String, val songCount: Int)

object GenreRepository {
    suspend fun getAllGenres(context: Context): List<Genre> = withContext(Dispatchers.IO) {
        val genres = mutableListOf<Genre>()
        val proj = arrayOf(MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME)
        context.contentResolver.query(MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI, proj, null, null, null)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Genres._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val name = c.getString(nameCol)
                if (!name.isNullOrBlank()) {
                    // Contar canciones del género
                    val countProj = arrayOf(MediaStore.Audio.Media._ID)
                    val countUri = MediaStore.Audio.Genres.Members.getContentUri("external", id)
                    val count = context.contentResolver.query(countUri, countProj, null, null, null)?.use { it.count } ?: 0
                    if (count > 0) genres.add(Genre(id, name, count))
                }
            }
        }
        genres.sortedBy { it.name }
    }
}