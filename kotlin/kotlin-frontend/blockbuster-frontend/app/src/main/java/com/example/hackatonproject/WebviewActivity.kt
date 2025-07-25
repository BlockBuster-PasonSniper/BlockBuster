package com.example.hackatonproject

import android.os.Bundle
import android.view.MenuItem
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity

class WebviewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        // ← 버튼 활성화
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "HackatonProject"

        val webView = findViewById<WebView>(R.id.webView)
        webView.settings.javaScriptEnabled = true
        val url = intent.getStringExtra("url")
        webView.loadUrl(url ?: "http://localhost:3000/api/receive-json")
    }

    // ← 뒤로가기 동작
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish() // 현재 Activity 종료
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
