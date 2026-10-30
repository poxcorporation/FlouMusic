package com.flou.music.ui.folder

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.flou.music.R
import com.flou.music.SongAdapter
import com.flou.music.data.QueueManager
import com.flou.music.data.model.Song
import com.flou.music.data.repository.MusicRepository
import com.flou.music.ui.player.PlayerActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class FolderDetailActivity : AppCompatActivity() {
    private lateinit var adapter: SongAdapter
    private var folderSongs: List<Song> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_folder_detail)

        val folderPath = intent.getStringExtra("FOLDER_PATH") ?: ""
        val folderName = intent.getStringExtra("FOLDER_NAME") ?: "Carpeta"

        findViewById<MaterialToolbar>(R.id.topBar).apply {
            title = folderName
            setNavigationOnClickListener { finish() }
        }

        val rv = findViewById<RecyclerView>(R.id.rvFolderSongs)
        rv.layoutManager = LinearLayoutManager(this)

        adapter = SongAdapter(emptyList()) { song ->
            val idx = folderSongs.indexOf(song)
            QueueManager.setQueue(folderSongs, idx)
            startActivity(Intent(this, PlayerActivity::class.java))
        }
        rv.adapter = adapter

        // Botón flotante de Aleatorio para esta carpeta
        val fabShuffle = findViewById<FloatingActionButton>(R.id.fabShuffleFolder)
        fabShuffle.setOnClickListener {
            if (folderSongs.isNotEmpty()) {
                QueueManager.setQueue(folderSongs, 0)
                QueueManager.shuffleQueue()
                QueueManager.getCurrentSong()?.let { _ ->
                    startActivity(Intent(this, PlayerActivity::class.java))
                }
                android.widget.Toast.makeText(this, "Reproducción aleatoria de la carpeta", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        loadFolderSongs(folderPath)
    }

    private fun loadFolderSongs(folderPath: String) {
        lifecycleScope.launch {
            val allSongs = MusicRepository.getAllSongs(this@FolderDetailActivity)
            folderSongs = allSongs.filter { song ->
                val cursor = contentResolver.query(
                    song.uri,
                    arrayOf(android.provider.MediaStore.Audio.Media.DATA),
                    null, null, null
                )
                var path: String? = null
                cursor?.use {
                    if (it.moveToFirst()) {
                        path = it.getString(0)
                    }
                }
                path?.startsWith(folderPath) == true
            }
            adapter.updateSongs(folderSongs)
        }
    }
}