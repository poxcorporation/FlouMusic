package com.flou.music.util

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.flou.music.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object M3UHandler {

    suspend fun exportPlaylist(context: Context, songs: List<Song>, fileName: String): Uri? = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val contentValues = android.content.ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, "$fileName.m3u8")
                put(MediaStore.Downloads.MIME_TYPE, "audio/x-mpegurl")
                put(MediaStore.Downloads.RELATIVE_PATH, "Music/FlouMusic/")
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                resolver.openOutputStream(it)?.use { output ->
                    output.write("#EXTM3U\n".toByteArray())
                    songs.forEach { song ->
                        val path = getRealPath(context, song.uri)
                        output.write("#EXTINF:${song.duration / 1000},${song.artist} - ${song.title}\n".toByteArray())
                        output.write("${path ?: song.uri.toString()}\n".toByteArray())
                    }
                }
            }
            uri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun importPlaylist(context: Context, uri: Uri): List<String> = withContext(Dispatchers.IO) {
        val paths = mutableListOf<String>()
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input)).useLines { lines ->
                    lines.forEach { line ->
                        val trimmed = line.trim()
                        if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                            paths.add(trimmed)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        paths
    }

    private fun getRealPath(context: Context, uri: Uri): String? {
        val proj = arrayOf(MediaStore.Audio.Media.DATA)
        context.contentResolver.query(uri, proj, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            if (cursor.moveToFirst()) return cursor.getString(idx)
        }
        return null
    }
}