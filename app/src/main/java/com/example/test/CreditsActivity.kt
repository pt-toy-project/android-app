package com.example.test

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

/** Bundled citations work offline. Only a user's HTTP(S) link click opens the browser. */
class CreditsActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private var googleCreditsDialog: androidx.appcompat.app.AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_credits)
        applySystemBarInsets()
        ScreenToolbar.bind(this, R.string.redesign_credits, back = true)
        findViewById<MaterialButton>(R.id.credits_close).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.credits_google).setOnClickListener {
            googleCreditsDialog?.dismiss()
            googleCreditsDialog = androidx.appcompat.app.AlertDialog.Builder(this).setTitle(R.string.google_library_credits)
                .setMessage(R.string.google_library_notice).setPositiveButton(android.R.string.ok, null).show()
        }
        webView = findViewById(R.id.credits_webview)
        webView.settings.apply {
            textZoom = (resources.configuration.fontScale * 100).toInt()
            if (android.os.Build.VERSION.SDK_INT >= 33) isAlgorithmicDarkeningAllowed = true
            javaScriptEnabled = false
            allowFileAccess = false
            allowContentAccess = false
            domStorageEnabled = false
            blockNetworkLoads = true
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (request.url.toString() == ASSET_URL) return false
                if (request.isForMainFrame && request.hasGesture()) openSource(request.url)
                return true
            }

            @Deprecated("Used by older Android WebView implementations")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                if (url == ASSET_URL) return false
                openSource(Uri.parse(url))
                return true
            }
        }
        if (savedInstanceState == null || webView.restoreState(savedInstanceState) == null) webView.loadUrl(ASSET_URL)
    }

    private fun openSource(uri: Uri) {
        if (uri.scheme !in setOf("http", "https") || uri.host.isNullOrBlank()) return
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.credits_browser_unavailable, Toast.LENGTH_LONG).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        googleCreditsDialog?.dismiss()
        webView.stopLoading()
        (webView.parent as? android.view.ViewGroup)?.removeView(webView)
        webView.destroy()
        super.onDestroy()
    }

    private companion object {
        const val ASSET_URL = "file:///android_asset/history-credits.html"
    }
}
