package com.flou.music.ui.player

import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.flou.music.R
import com.flou.music.data.QueueManager
import com.flou.music.service.MusicPlaybackService
import com.flou.music.ui.editor.MetadataEditorActivity
import com.flou.music.ui.equalizer.EqualizerActivity
import com.flou.music.ui.webview.WebViewActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.google.common.util.concurrent.MoreExecutors
import java.net.URLEncoder

class PlayerActivity : AppCompatActivity() {

    private var controller: MediaController? = null
    private lateinit var slider: Slider
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvTotalTime: TextView
    private lateinit var btnPlayPause: MaterialButton
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>
    private lateinit var prefs: SharedPreferences
    private val handler = Handler(Looper.getMainLooper())
    private var currentSong = QueueManager.getCurrentSong()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        
        prefs = getSharedPreferences("FlouMusic", MODE_PRIVATE)

        slider = findViewById(R.id.sliderProgress)
        tvCurrentTime = findViewById(R.id.tvCurrentTime)
        tvTotalTime = findViewById(R.id.tvTotalTime)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        val btnNext = findViewById<MaterialButton>(R.id.btnNext)
        val btnPrev = findViewById<MaterialButton>(R.id.btnPrev)
        val btnEdit = findViewById<MaterialButton>(R.id.btnEdit)
        val btnOptions = findViewById<MaterialButton>(R.id.btnOptions)
        val btnEqualizer = findViewById<MaterialButton>(R.id.btnEqualizer)
        val tvLyrics = findViewById<TextView>(R.id.tvLyrics)
        val btnSearchLyrics = findViewById<MaterialButton>(R.id.btnSearchLyrics)

        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottomSheetLyrics))
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        val sessionToken = SessionToken(this, ComponentName(this, MusicPlaybackService::class.java))
        val future = MediaController.Builder(this, sessionToken).buildAsync()

        future.addListener({
            controller = future.get()
            currentSong?.let { song ->
                if (controller?.mediaItemCount == 0) {
                    controller?.setMediaItem(MediaItem.fromUri(song.uri))
                    controller?.prepare()
                    controller?.play()
                }
                updateUI(song)
            }

            slider.addOnChangeListener { _, value, fromUser ->
                if (fromUser && (controller?.duration ?: 0) > 0) {
                    controller?.seekTo((value / 100f * (controller?.duration ?: 0)).toLong())
                }
            }

            controller?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    btnPlayPause.setIconResource(
                        if (isPlaying) android.R.drawable.ic_media_pause
                        else android.R.drawable.ic_media_play
                    )
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        QueueManager.getNextSong()?.let { next ->
                            currentSong = next
                            controller?.setMediaItem(MediaItem.fromUri(next.uri))
                            controller?.prepare()
                            controller?.play()
                            updateUI(next)
                        }
                    }
                }
            })

            handler.post(object : Runnable {
                override fun run() {
                    controller?.let { c ->
                        if (c.duration > 0) {
                            val progress = (c.currentPosition.toFloat() / c.duration * 100f)
                                .coerceIn(0f, 100f)
                            slider.value = progress
                            tvCurrentTime.text = formatTime(c.currentPosition)
                            tvTotalTime.text = formatTime(c.duration)
                        }
                    }
                    handler.postDelayed(this, 500)
                }
            })

            btnPlayPause.setOnClickListener {
                controller?.let { if (it.isPlaying) it.pause() else it.play() }
            }

            btnNext.setOnClickListener {
                QueueManager.getNextSong()?.let { next ->
                    currentSong = next
                    controller?.setMediaItem(MediaItem.fromUri(next.uri))
                    controller?.prepare()
                    controller?.play()
                    updateUI(next)
                }
            }

            btnPrev.setOnClickListener {
                if ((controller?.currentPosition ?: 0) > 3000) {
                    controller?.seekTo(0)
                } else {
                    QueueManager.getPreviousSong()?.let { prev ->
                        currentSong = prev
                        controller?.setMediaItem(MediaItem.fromUri(prev.uri))
                        controller?.prepare()
                        controller?.play()
                        updateUI(prev)
                    }
                }
            }

            btnEdit.setOnClickListener {
                val options = arrayOf(
                    "Editar metadatos",
                    "Cambiar carátula",
                    "Ver Letra"
                )
                MaterialAlertDialogBuilder(this)
                    .setTitle("Editar")
                    .setItems(options) { _, which ->
                        when (which) {
                            0, 1 -> {
                                val intent = Intent(this, MetadataEditorActivity::class.java)
                                intent.putExtra("FILE_PATH", getRealPathFromUri(currentSong?.uri))
                                intent.putExtra("SONG_URI", currentSong?.uri?.toString())
                                startActivity(intent)
                            }
                            2 -> {
                                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                                tvLyrics.text = "No hay Letra"
                            }
                        }
                    }
                    .show()
            }

            btnOptions.setOnClickListener {
                showOptionsMenu(it)
            }

            btnEqualizer.setOnClickListener {
                val intent = Intent(this, EqualizerActivity::class.java)
                intent.putExtra("AUDIO_SESSION_ID", 0)
                startActivity(intent)
            }

            btnSearchLyrics.setOnClickListener {
                currentSong?.let { song ->
                    val query = "${song.title} de ${song.artist} letra"
                    val searchUrl = "https://www.google.com/search?q=${URLEncoder.encode(query, "UTF-8")}"
                    val intent = Intent(this, WebViewActivity::class.java)
                    intent.putExtra("URL", searchUrl)
                    startActivity(intent)
                }
            }

        }, MoreExecutors.directExecutor())
    }

    private fun showOptionsMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, "Velocidad de reproducción")
        popup.menu.add(0, 2, 1, "Compartir canción")
        popup.menu.add(0, 3, 2, "Agregar a Favoritos")
        popup.menu.add(0, 4, 3, "Agregar a Playlist")
        popup.menu.add(0, 5, 4, "Abrir canción en...")
        popup.menu.add(0, 6, 5, "Modo Aleatorio: ${if (QueueManager.isShuffled) "ON" else "OFF"}")
        popup.menu.add(0, 7, 6, "Repetir: ${getRepeatModeText()}")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> showSpeedDialog()
                2 -> shareSong()
                3 -> toggleFavorite()
                4 -> android.widget.Toast.makeText(this, "Agregar a Playlist (próximamente)", android.widget.Toast.LENGTH_SHORT).show()
                5 -> openWith()
                6 -> toggleShuffle()
                7 -> cycleRepeatMode()
            }
            true
        }
        popup.show()
    }

    private fun toggleFavorite() {
        currentSong?.let { song ->
            val favorites = prefs.getStringSet("favorites", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            val songId = song.id.toString()
            
            if (favorites.contains(songId)) {
                favorites.remove(songId)
                android.widget.Toast.makeText(this, "Eliminado de Favoritos", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                favorites.add(songId)
                android.widget.Toast.makeText(this, "Agregado a Favoritos", android.widget.Toast.LENGTH_SHORT).show()
            }
            
            prefs.edit().putStringSet("favorites", favorites).apply()
        }
    }

    private fun showSpeedDialog() {
        val speeds = arrayOf("0.5x", "0.75x", "1x (Normal)", "1.25x", "1.5x", "2x")
        val values = floatArrayOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

        MaterialAlertDialogBuilder(this)
            .setTitle("Velocidad de reproducción")
            .setItems(speeds) { _, which ->
                controller?.setPlaybackParameters(PlaybackParameters(values[which]))
                android.widget.Toast.makeText(this, "Velocidad: ${speeds[which]}", android.widget.Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun shareSong() {
        currentSong?.let { song ->
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_TEXT, "Escucha ${song.title} de ${song.artist}")
                putExtra(Intent.EXTRA_STREAM, song.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Compartir canción"))
        }
    }

    private fun openWith() {
        currentSong?.let { song ->
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(song.uri, "audio/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            try {
                startActivity(Intent.createChooser(intent, "Abrir con..."))
            } catch (e: Exception) {
                android.widget.Toast.makeText(this, "No hay apps para abrir", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleShuffle() {
        if (QueueManager.isShuffled) {
            QueueManager.unshuffleQueue()
            android.widget.Toast.makeText(this, "Aleatorio: OFF", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            QueueManager.shuffleQueue()
            android.widget.Toast.makeText(this, "Aleatorio: ON", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun cycleRepeatMode() {
        QueueManager.repeatMode = (QueueManager.repeatMode + 1) % 3
        val msg = when (QueueManager.repeatMode) {
            0 -> "Repetir: OFF"
            1 -> "Repetir: Todas"
            2 -> "Repetir: Una"
            else -> ""
        }
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun getRepeatModeText(): String {
        return when (QueueManager.repeatMode) {
            0 -> "OFF"
            1 -> "Todas"
            2 -> "Una"
            else -> "OFF"
        }
    }

    private fun updateUI(song: com.flou.music.data.model.Song) {
        findViewById<TextView>(R.id.tvPlayerTitle).text = song.title
        findViewById<TextView>(R.id.tvPlayerArtist).text = song.artist
        
        // LOGO PREDETERMINADO si no hay carátula
        val ivArt = findViewById<ImageView>(R.id.ivPlayerArt)
        if (song.albumArtUri != null) {
            ivArt.setImageURI(song.albumArtUri)
        } else {
            ivArt.setImageResource(android.R.drawable.ic_media_play)
        }
    }

    private fun getRealPathFromUri(uri: android.net.Uri?): String? {
        uri ?: return null
        val proj = arrayOf(android.provider.MediaStore.Audio.Media.DATA)
        contentResolver.query(uri, proj, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DATA)
            if (cursor.moveToFirst()) return cursor.getString(idx)
        }
        return null
    }

    private fun formatTime(ms: Long): String {
        val s = ms / 1000
        return String.format("%d:%02d", s / 60, s % 60)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        MediaController.releaseFuture(
            MediaController.Builder(
                this,
                SessionToken(this, ComponentName(this, MusicPlaybackService::class.java))
            ).buildAsync()
        )
    }
}