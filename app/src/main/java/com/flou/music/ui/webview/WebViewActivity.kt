package com.flou.music.ui.webview

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.flou.music.R
import com.google.android.material.button.MaterialButton

class WebViewActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        webView = findViewById(R.id.webView)
        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = true

        val url = intent.getStringExtra("URL") ?: "https://google.com"
        webView.loadUrl(url)

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener {
            if (webView.canGoBack()) webView.goBack() else finish()
        }
        findViewById<MaterialButton>(R.id.btnReload).setOnClickListener { webView.reload() }
    }
}