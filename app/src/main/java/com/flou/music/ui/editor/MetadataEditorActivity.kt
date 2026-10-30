package com.flou.music.ui.editor

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.flou.music.R
import com.flou.music.ui.webview.WebViewActivity
import com.mpatric.mp3agic.ID3v24Tag
import com.mpatric.mp3agic.Mp3File
import java.io.File
import java.net.URLEncoder

class MetadataEditorActivity : AppCompatActivity() {
    private lateinit var etTitle: EditText
    private lateinit var etArtist: EditText
    private lateinit var etAlbum: EditText
    private lateinit var etYear: EditText
    private lateinit var etGenre: EditText
    private lateinit var btnSave: Button
    private lateinit var btnChangeCover: Button
    private lateinit var btnLyrics: Button
    private lateinit var ivCover: ImageView

    private var songUri: Uri? = null
    private var realPath: String? = null
    private var newCoverBytes: ByteArray? = null
    private var songTitle: String = ""
    private var songArtist: String = ""

    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    contentResolver.openInputStream(uri)?.use { input ->
                        newCoverBytes = input.readBytes()
                        val bmp = android.graphics.BitmapFactory.decodeByteArray(newCoverBytes!!, 0, newCoverBytes!!.size)
                        ivCover.setImageBitmap(bmp)
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error al cargar imagen", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (hasManageStoragePermission()) {
            Toast.makeText(this, "Permiso concedido. Ahora puedes editar.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permiso denegado. No se podrán guardar cambios.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_metadata_editor)

        etTitle = findViewById(R.id.etTitle)
        etArtist = findViewById(R.id.etArtist)
        etAlbum = findViewById(R.id.etAlbum)
        etYear = findViewById(R.id.etYear)
        etGenre = findViewById(R.id.etGenre)
        btnSave = findViewById(R.id.btnSave)
        btnChangeCover = findViewById(R.id.btnChangeCover)
        btnLyrics = findViewById(R.id.btnLyrics)
        ivCover = findViewById(R.id.ivCover)

        songUri = intent.getStringExtra("SONG_URI")?.let { Uri.parse(it) }
        realPath = intent.getStringExtra("FILE_PATH")
        
        // Pedir permiso de escritura completa si es necesario
        if (!hasManageStoragePermission()) {
            requestManageStoragePermission()
        }
        
        loadCurrentMetadata()

        btnSave.setOnClickListener { saveMetadata() }
        btnChangeCover.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            pickImage.launch(intent)
        }
        btnLyrics.setOnClickListener {
            val query = "$songTitle de $songArtist letra"
            val searchUrl = "https://www.google.com/search?q=${URLEncoder.encode(query, "UTF-8")}"
            val intent = Intent(this, WebViewActivity::class.java)
            intent.putExtra("URL", searchUrl)
            startActivity(intent)
        }
    }

    private fun hasManageStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestManageStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
                requestPermissionLauncher.launch(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                requestPermissionLauncher.launch(intent)
            }
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                1001
            )
        }
    }

    private fun loadCurrentMetadata() {
        realPath?.let { path ->
            try {
                val file = File(path)
                if (!file.exists()) {
                    Toast.makeText(this, "Archivo no accesible", Toast.LENGTH_SHORT).show()
                    return
                }
                val mp3 = Mp3File(file)
                if (mp3.hasId3v2Tag()) {
                    val tag = mp3.id3v2Tag
                    songTitle = tag.title ?: ""
                    songArtist = tag.artist ?: ""
                    etTitle.setText(songTitle)
                    etArtist.setText(songArtist)
                    etAlbum.setText(tag.album ?: "")
                    etYear.setText(tag.year ?: "")
                    etGenre.setText(tag.genreDescription ?: "")
                    tag.albumImage?.let { bytes ->
                        val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        ivCover.setImageBitmap(bmp)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveMetadata() {
        if (!hasManageStoragePermission()) {
            Toast.makeText(this, "Se necesita permiso de almacenamiento. Concédelo e intenta de nuevo.", Toast.LENGTH_LONG).show()
            requestManageStoragePermission()
            return
        }
        
        realPath?.let { path ->
            try {
                val file = File(path)
                val mp3 = Mp3File(file)
                val tag = if (mp3.hasId3v2Tag()) mp3.id3v2Tag else ID3v24Tag().apply { mp3.id3v2Tag = this }

                tag.title = etTitle.text.toString()
                tag.artist = etArtist.text.toString()
                tag.album = etAlbum.text.toString()
                tag.year = etYear.text.toString()
                tag.genreDescription = etGenre.text.toString()

                newCoverBytes?.let { bytes ->
                    tag.setAlbumImage(bytes, "image/jpeg")
                }

                val tempPath = path + ".tmp"
                mp3.save(tempPath)
                val tempFile = File(tempPath)
                val originalFile = File(path)
                
                if (originalFile.delete() && tempFile.renameTo(originalFile)) {
                    Toast.makeText(this, "Guardado correctamente", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Error al reemplazar archivo", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}