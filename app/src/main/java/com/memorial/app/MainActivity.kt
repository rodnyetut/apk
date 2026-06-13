package com.memorial.app

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private var retryCount = 0
    private val maxRetries = 2

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)

        setupWebView()
        loadUrl()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            
            // ВКЛЮЧАЕМ КЭШ ДЛЯ ОФЛАЙН-РЕЖИМА
            cacheMode = WebSettings.LOAD_DEFAULT
            setAppCacheEnabled(true)
            
            // БЕЗОПАСНОЕ ВКЛЮЧЕНИЕ БАЗЫ ДАННЫХ (нужно для Service Worker)
            try {
                databaseEnabled = true
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                    setDatabasePath(cacheDir.absolutePath)
                }
            } catch (e: Exception) {
                // Игнорируем ошибки на старых версиях
                e.printStackTrace()
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE
                retryCount = 0
            }

            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                progressBar.visibility = View.GONE
                
                // Если нет интернета и страница не в кэше
                if (!isNetworkAvailable()) {
                    Toast.makeText(
                        this@MainActivity,
                        "Нет интернета, загружаем сохранённую версию",
                        Toast.LENGTH_SHORT
                    ).show()
                    // Пытаемся загрузить из кэша
                    webView?.loadUrl("https://aobaushev.ru")
                    return
                }
                
                if (retryCount < maxRetries) {
                    retryCount++
                    webView?.postDelayed({
                        webView?.loadUrl("https://aobaushev.ru")
                    }, 1500)
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Ошибка загрузки: $description",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: android.webkit.SslErrorHandler?,
                error: android.net.http.SslError?
            ) {
                handler?.proceed()
            }
        }

        webView.webChromeClient = WebChromeClient()
    }

    private fun loadUrl() {
        // Сначала пробуем загрузить страницу (из сети или кэша)
        webView.loadUrl("https://aobaushev.ru")
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return when {
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> true
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
