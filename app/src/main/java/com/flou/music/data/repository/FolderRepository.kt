package com.flou.music.data.repository

import android.content.Context
import android.provider.MediaStore
import com.flou.music.data.model.Folder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FolderRepository {
    suspend fun getAllFolders(context: Context): List<Folder> = withContext(Dispatchers.IO) {
        val folderMap = mutableMapOf<String, Int>()
        val projection = arrayOf(MediaStore.Audio.Media.DATA)
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            while (cursor.moveToNext()) {
                val filePath = cursor.getString(dataCol)
                val folderPath = filePath.substringBeforeLast("/")
                folderMap[folderPath] = folderMap.getOrDefault(folderPath, 0) + 1
            }
        }

        folderMap.map { (path, count) ->
            Folder(
                name = path.substringAfterLast("/"),
                songCount = count,
                path = path
            )
        }.sortedBy { it.name }
    }
}