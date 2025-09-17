package me.ash.reader.ui.component.webview

import android.content.Context
import android.net.http.SslError
import android.util.Log
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import me.ash.reader.ui.ext.isUrl
import java.io.DataInputStream
import java.net.HttpURLConnection
import java.net.URI

const val INJECTION_TOKEN = "/android_asset_font/"

class WebViewClient(
    private val context: Context,
    private val refererDomain: String?,
    private val onOpenLink: (url: String) -> Unit,
) : WebViewClient() {

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?,
    ): WebResourceResponse? {
        val url = request?.url?.toString()
        if (url != null && url.contains(INJECTION_TOKEN)) {
            try {
                val assetPath = url.substring(
                    url.indexOf(INJECTION_TOKEN) + INJECTION_TOKEN.length,
                    url.length
                )
                return WebResourceResponse(
                    "text/HTML",
                    "UTF-8",
                    context.assets.open(assetPath)
                )
            } catch (e: Exception) {
                Log.e("RLog", "WebView shouldInterceptRequest: $e")
            }
        } else if (url != null && url.isUrl()) {
            try {
                var connection = URI.create(url).toURL().openConnection() as HttpURLConnection
                if (connection.responseCode == 403) {
                    connection.disconnect()
                    connection = URI.create(url).toURL().openConnection() as HttpURLConnection
                    connection.setRequestProperty("Referer", refererDomain)
                    val inputStream = DataInputStream(connection.inputStream)
                    return WebResourceResponse(connection.contentType, "UTF-8", inputStream)
                }
            } catch (e: Exception) {
                Log.e("RLog", "shouldInterceptRequest url: $e")
            }
        }
        return super.shouldInterceptRequest(view, request)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        view!!.evaluateJavascript(OnImgClickScript, null)
        // 注入墨水屏优化CSS
        view.evaluateJavascript(EinkOptimizationScript, null)
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        if (null == request?.url) return false
        val url = request.url.toString()
        if (url.isNotEmpty()) onOpenLink(url)
        return true
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?,
    ) {
        super.onReceivedError(view, request, error)
        Log.e("RLog", "RYWebView onReceivedError: $error")
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        handler?.cancel()
    }

    companion object {
        private const val OnImgClickScript = """
            javascript:(function() {
                var imgs = document.getElementsByTagName("img");
                for(var i = 0; i < imgs.length; i++){
                    imgs[i].pos = i;
                    imgs[i].onclick = function(event) {
                        event.preventDefault();
                        window.${JavaScriptInterface.NAME}.onImgTagClick(this.src, this.alt);
                    }
                }
            })()
            """
        
        private const val EinkOptimizationScript = """
            javascript:(function() {
                var style = document.createElement('style');
                style.id = 'eink-optimization';
                style.innerHTML = `
                    body *, article *, p *, h1 *, h2 *, h3 *, h4 *, h5 *, h6 * { 
                        -webkit-font-smoothing: none !important;
                        -moz-osx-font-smoothing: none !important;
                        text-rendering: optimizeSpeed !important;
                        text-shadow: 0.3px 0.3px 0 currentColor !important;
                        font-weight: 500 !important;
                        /* 墨水屏软件渲染：禁用硬件加速相关属性 */
                        backface-visibility: initial !important;
                        transform: none !important;
                        will-change: auto !important;
                    }
                    img { 
                        filter: grayscale(100%) contrast(150%);
                        image-rendering: -webkit-optimize-contrast;
                        /* 确保图片也不触发硬件加速 */
                        backface-visibility: initial !important;
                        transform: none !important;
                    }
                `;
                // 检查是否已经添加过样式，避免重复
                var existingStyle = document.getElementById('eink-optimization');
                if (!existingStyle) {
                    document.head.appendChild(style);
                }
            })()
            """
    }
}
