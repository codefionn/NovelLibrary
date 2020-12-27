package io.github.gmathi.novellibrary.activity.settings

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.webkit.*
import io.github.gmathi.novellibrary.R

class OpenSourceLicensesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_open_source_licenses)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val url = "file:///android_asset/open_source_licenses.html"

        val webView = findViewById<WebView>(R.id.webView)
        webView.apply {
            settings.apply {
                defaultFontSize = 12
                allowContentAccess = true
                allowFileAccess = true
            }
        }

        webView.loadUrl(url)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }
}