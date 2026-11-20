package com.cronos

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader
import java.io.ByteArrayInputStream

/**
 * Muestra la app web (assets/index.html) a pantalla completa.
 * Todo funciona sin conexión: las fotos, la parrilla y los tiempos se guardan
 * en el almacenamiento del propio WebView, que persiste entre usos de la app.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var fileCallback: ValueCallback<Array<Uri>>? = null

    // Resultado del selector de fotos que abre el botón "Añadir fotos" de la página.
    private val pickFiles =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            fileCallback?.onReceiveValue(selectedUris(result.resultCode, result.data))
            fileCallback = null
        }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // La pantalla no se apaga mientras la app está abierta: es un cronómetro.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Sirve assets/ desde un origen https propio, necesario para que
        // IndexedDB y localStorage funcionen igual que en un navegador.
        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView = WebView(this)
        setContentView(webView)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            // Pitidos de la cuenta atrás sin tener que tocar la pantalla antes de cada salida.
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = false
            allowContentAccess = true
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        // Guardar resultados en las hojas de cálculo de la temporada (Descargas/Cronos).
        webView.addJavascriptInterface(SeasonFiles(this), "CronosFiles")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val url = request?.url ?: return null
                if (url.host == WebViewAssetLoader.DEFAULT_DOMAIN) {
                    return loader.shouldInterceptRequest(url)
                }
                // Cualquier otra dirección queda bloqueada: la app es 100 % local.
                return WebResourceResponse(
                    "text/plain", "utf-8", 403, "Blocked",
                    emptyMap(), ByteArrayInputStream(ByteArray(0))
                )
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean = true
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                params: WebChromeClient.FileChooserParams?
            ): Boolean {
                if (callback == null || params == null) return false
                fileCallback?.onReceiveValue(null)
                fileCallback = callback
                return try {
                    val intent = params.createIntent()
                    if (params.mode == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE) {
                        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    }
                    pickFiles.launch(intent)
                    true
                } catch (e: Exception) {
                    fileCallback = null
                    false
                }
            }
        }

        webView.loadUrl("https://${WebViewAssetLoader.DEFAULT_DOMAIN}/assets/index.html")
    }

    /**
     * Devuelve todas las fotos elegidas. Con selección múltiple Android las manda
     * en ClipData, y FileChooserParams.parseResult solo lee una: por eso antes,
     * al elegir varias y pulsar "Hecho", no llegaba ninguna a la página.
     */
    private fun selectedUris(resultCode: Int, data: Intent?): Array<Uri>? {
        if (resultCode != Activity.RESULT_OK || data == null) return null
        val uris = mutableListOf<Uri>()
        data.clipData?.let { clip ->
            for (i in 0 until clip.itemCount) clip.getItemAt(i).uri?.let { uris.add(it) }
        }
        if (uris.isEmpty()) data.data?.let { uris.add(it) }
        return if (uris.isEmpty()) null else uris.toTypedArray()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        webView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
