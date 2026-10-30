package com.flou.music.ui.developer

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.flou.music.R
import com.flou.music.ui.webview.WebViewActivity
import com.google.android.material.button.MaterialButton

class DeveloperActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_developer)

        // URL de tu imagen (cámbiala cuando quieras)
        val imageUrl = "https://avatars.githubusercontent.com/u/213645053?v=4"
        val projectUrl = "https://github.com/poxcorporation"

        Glide.with(this).load(imageUrl).into(findViewById<ImageView>(R.id.ivDevImage))

        findViewById<MaterialButton>(R.id.btnMoreProjects).setOnClickListener {
            val intent = Intent(this, WebViewActivity::class.java)
            intent.putExtra("URL", projectUrl)
            startActivity(intent)
        }
    }
}