package com.flou.music

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.flou.music.data.QueueManager
import com.flou.music.data.model.Song
import com.flou.music.data.repository.AlbumRepository
import com.flou.music.data.repository.ArtistRepository
import com.flou.music.data.repository.FolderRepository
import com.flou.music.data.repository.GenreRepository
import com.flou.music.data.repository.MusicRepository
import com.flou.music.ui.developer.DeveloperActivity
import com.flou.music.ui.folder.FolderDetailActivity
import com.flou.music.ui.player.PlayerActivity
import com.flou.music.ui.settings.SettingsActivity
import com.flou.music.ui.webview.WebViewActivity
import com.flou.music.util.M3UHandler
import com.flou.music.util.ThemeManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.search.SearchBar
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var rvMain: RecyclerView
    private lateinit var tabLayout: TabLayout
    private lateinit var miniPlayerContainer: FrameLayout
    private lateinit var topBar: MaterialToolbar
    private lateinit var searchBar: SearchBar
    private lateinit var fabShuffle: FloatingActionButton

    private lateinit var songAdapter: SongAdapter
    private lateinit var folderAdapter: FolderAdapter
    private lateinit var artistAdapter: ArtistAdapter
    private lateinit var albumAdapter: AlbumAdapter
    private lateinit var genreAdapter: GenreAdapter

    private var allSongs: List<Song> = emptyList()
    private var currentSong: Song? = null
    private var currentTab = 0

    private val openM3ULauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            lifecycleScope.launch {
                val paths = M3UHandler.importPlaylist(this@MainActivity, it)
                android.widget.Toast.makeText(
                    this@MainActivity,
                    "Importadas ${paths.size} canciones de la lista",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        setTheme(ThemeManager.getColorTheme(this))
        
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvMain = findViewById(R.id.rvMain)
        tabLayout = findViewById(R.id.tabLayout)
        miniPlayerContainer = findViewById(R.id.miniPlayerContainer)
        topBar = findViewById(R.id.topBar)
        searchBar = findViewById(R.id.searchBar)
        fabShuffle = findViewById(R.id.fabShuffle)

        rvMain.layoutManager = LinearLayoutManager(this)

        songAdapter = SongAdapter(emptyList()) { song ->
            val idx = allSongs.indexOf(song)
            QueueManager.setQueue(allSongs, idx)
            currentSong = song
            startActivity(Intent(this, PlayerActivity::class.java))
            updateMiniPlayer(song)
        }

        folderAdapter = FolderAdapter(emptyList()) { folder ->
            val intent = Intent(this, FolderDetailActivity::class.java)
            intent.putExtra("FOLDER_PATH", folder.path)
            intent.putExtra("FOLDER_NAME", folder.name)
            startActivity(intent)
        }

        artistAdapter = ArtistAdapter(emptyList()) { a ->
            android.widget.Toast.makeText(this, "Artista: ${a.name}", android.widget.Toast.LENGTH_SHORT).show()
        }

        albumAdapter = AlbumAdapter(emptyList()) { al ->
            android.widget.Toast.makeText(this, "Álbum: ${al.name}", android.widget.Toast.LENGTH_SHORT).show()
        }

        genreAdapter = GenreAdapter(emptyList()) { g ->
            android.widget.Toast.makeText(this, "Género: ${g.name}", android.widget.Toast.LENGTH_SHORT).show()
        }

        setupTabs()
        setupMenu()
        setupShuffleButton()
        setupSearch()
        loadSongs()
    }

    private fun setupSearch() {
        searchBar.setOnClickListener {
            // Mostrar diálogo de búsqueda
            val options = arrayOf("Por Canción", "Por Artista", "Por Álbum", "Por Año")
            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Buscar música")
                .setItems(options) { _, which ->
                    showSearchDialog(which)
                }
                .show()
        }
    }

    private fun showSearchDialog(type: Int) {
        val title = when (type) {
            0 -> "Buscar por Canción"
            1 -> "Buscar por Artista"
            2 -> "Buscar por Álbum"
            3 -> "Buscar por Año"
            else -> "Buscar"
        }

        val editText = android.widget.EditText(this).apply {
            hint = "Escribe aquí..."
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(editText)
            .setPositiveButton("Buscar") { _, _ ->
                val query = editText.text.toString()
                if (query.isNotEmpty()) {
                    performSearch(query, type)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun performSearch(query: String, type: Int) {
        lifecycleScope.launch {
            val results = when (type) {
                0 -> allSongs.filter { it.title.contains(query, ignoreCase = true) }
                1 -> allSongs.filter { it.artist.contains(query, ignoreCase = true) }
                2 -> allSongs.filter { 
                    // Buscar en álbum (requiere consulta adicional)
                    val cursor = contentResolver.query(
                        it.uri,
                        arrayOf(android.provider.MediaStore.Audio.Media.ALBUM),
                        null, null, null
                    )
                    var album = ""
                    cursor?.use { c ->
                        if (c.moveToFirst()) album = c.getString(0) ?: ""
                    }
                    album.contains(query, ignoreCase = true)
                }
                3 -> allSongs.filter {
                    // Buscar por año
                    val cursor = contentResolver.query(
                        it.uri,
                        arrayOf(android.provider.MediaStore.Audio.Media.YEAR),
                        null, null, null
                    )
                    var year = ""
                    cursor?.use { c ->
                        if (c.moveToFirst()) year = c.getString(0) ?: ""
                    }
                    year.contains(query)
                }
                else -> emptyList()
            }

            if (results.isNotEmpty()) {
                songAdapter.updateSongs(results)
                rvMain.adapter = songAdapter
                android.widget.Toast.makeText(
                    this@MainActivity,
                    "Encontradas ${results.size} canciones",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } else {
                android.widget.Toast.makeText(
                    this@MainActivity,
                    "No se encontraron resultados",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupShuffleButton() {
        fabShuffle.setOnClickListener {
            val listToShuffle = when (currentTab) {
                0 -> allSongs
                else -> allSongs
            }
            
            if (listToShuffle.isNotEmpty()) {
                QueueManager.setQueue(listToShuffle, 0)
                QueueManager.shuffleQueue()
                QueueManager.getCurrentSong()?.let { song ->
                    currentSong = song
                    startActivity(Intent(this, PlayerActivity::class.java))
                    updateMiniPlayer(song)
                }
                android.widget.Toast.makeText(this, "Reproducción aleatoria iniciada", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupMenu() {
        topBar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                R.id.action_open_m3u -> {
                    openM3ULauncher.launch("audio/x-mpegurl")
                    true
                }
                R.id.action_export_m3u -> {
                    if (allSongs.isNotEmpty()) {
                        lifecycleScope.launch {
                            val uri = M3UHandler.exportPlaylist(
                                this@MainActivity,
                                allSongs,
                                "FlouMusic_Playlist"
                            )
                            if (uri != null) {
                                android.widget.Toast.makeText(
                                    this@MainActivity,
                                    "Lista exportada correctamente",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                    true
                }
                R.id.action_github -> {
                    val i = Intent(this, WebViewActivity::class.java)
                    i.putExtra("URL", "https://github.com/")
                    startActivity(i)
                    true
                }
                R.id.action_developer -> {
                    startActivity(Intent(this, DeveloperActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun setupTabs() {
        listOf("Canciones", "Carpetas", "Artistas", "Álbumes", "Géneros", "Favoritos").forEach {
            tabLayout.addTab(tabLayout.newTab().setText(it))
        }
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                when (currentTab) {
                    0 -> loadSongs()
                    1 -> loadFolders()
                    2 -> loadArtists()
                    3 -> loadAlbums()
                    4 -> loadGenres()
                    5 -> loadFavorites()
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun loadSongs() {
        lifecycleScope.launch {
            allSongs = MusicRepository.getAllSongs(this@MainActivity)
            songAdapter.updateSongs(allSongs)
            rvMain.adapter = songAdapter
        }
    }

    private fun loadFolders() {
        lifecycleScope.launch {
            val folders = FolderRepository.getAllFolders(this@MainActivity)
            folderAdapter.update(folders)
            rvMain.adapter = folderAdapter
        }
    }

    private fun loadArtists() {
        lifecycleScope.launch {
            val artists = ArtistRepository.getAllArtists(this@MainActivity)
            artistAdapter.update(artists)
            rvMain.adapter = artistAdapter
        }
    }

    private fun loadAlbums() {
        lifecycleScope.launch {
            val albums = AlbumRepository.getAllAlbums(this@MainActivity)
            albumAdapter.update(albums)
            rvMain.adapter = albumAdapter
        }
    }

    private fun loadGenres() {
        lifecycleScope.launch {
            val genres = GenreRepository.getAllGenres(this@MainActivity)
            genreAdapter.update(genres)
            rvMain.adapter = genreAdapter
        }
    }

    private fun loadFavorites() {
        android.widget.Toast.makeText(this, "Favoritos (próximamente)", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun updateMiniPlayer(song: Song) {
        miniPlayerContainer.removeAllViews()
        val v = layoutInflater.inflate(R.layout.layout_mini_player, miniPlayerContainer, false)
        v.findViewById<TextView>(R.id.tvMiniTitle).text = song.title
        v.findViewById<TextView>(R.id.tvMiniArtist).text = song.artist
        song.albumArtUri?.let { v.findViewById<ImageView>(R.id.ivMiniArt).setImageURI(it) }
        v.findViewById<ImageButton>(R.id.btnMiniPlayPause).setOnClickListener {
            startActivity(Intent(this, PlayerActivity::class.java))
        }
        v.visibility = View.VISIBLE
        miniPlayerContainer.addView(v)
    }

    override fun onResume() {
        super.onResume()
        currentSong?.let { updateMiniPlayer(it) }
    }
}