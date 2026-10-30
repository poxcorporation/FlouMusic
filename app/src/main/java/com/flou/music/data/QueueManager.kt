package com.flou.music.data

import com.flou.music.data.model.Song

object QueueManager {
    var currentQueue: List<Song> = emptyList()
    var originalQueue: List<Song> = emptyList() // Para restaurar al salir de aleatorio
    var currentIndex: Int = 0
    var isShuffled: Boolean = false
    var repeatMode: Int = 0 // 0=no repeat, 1=repeat all, 2=repeat one

    fun setQueue(songs: List<Song>, startIndex: Int) {
        currentQueue = songs
        originalQueue = songs
        currentIndex = startIndex.coerceIn(0, songs.size - 1)
        isShuffled = false
    }

    fun shuffleQueue() {
        if (currentQueue.isEmpty()) return
        val currentSong = getCurrentSong()
        val shuffled = currentQueue.toMutableList()
        shuffled.shuffle()
        
        // Poner la canción actual al inicio
        currentSong?.let { song ->
            shuffled.remove(song)
            shuffled.add(0, song)
        }
        
        currentQueue = shuffled
        currentIndex = 0
        isShuffled = true
    }

    fun unshuffleQueue() {
        if (originalQueue.isEmpty()) return
        val currentSong = getCurrentSong()
        currentQueue = originalQueue
        currentIndex = originalQueue.indexOfFirst { it.uri == currentSong?.uri }.coerceAtLeast(0)
        isShuffled = false
    }

    fun getNextSong(): Song? {
        if (currentQueue.isEmpty()) return null
        
        return when (repeatMode) {
            2 -> currentQueue[currentIndex] // Repetir la misma
            else -> {
                currentIndex = (currentIndex + 1) % currentQueue.size
                if (currentIndex == 0 && repeatMode == 0 && !isShuffled) {
                    // Al final sin repetir, no avanza
                    null
                } else {
                    currentQueue[currentIndex]
                }
            }
        }
    }

    fun getPreviousSong(): Song? {
        if (currentQueue.isEmpty()) return null
        currentIndex = if (currentIndex - 1 < 0) currentQueue.size - 1 else currentIndex - 1
        return currentQueue[currentIndex]
    }

    fun getCurrentSong(): Song? {
        if (currentQueue.isEmpty()) return null
        return currentQueue.getOrNull(currentIndex)
    }
}