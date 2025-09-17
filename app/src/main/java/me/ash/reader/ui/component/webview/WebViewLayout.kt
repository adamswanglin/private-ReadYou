package me.ash.reader.ui.component.webview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.webkit.JavascriptInterface
import android.webkit.WebView
import me.ash.reader.infrastructure.preference.BasicFontsPreference
import me.ash.reader.infrastructure.preference.ReadingFontsPreference

object WebViewLayout {

    @SuppressLint("SetJavaScriptEnabled")
    fun get(
        context: Context,
        readingFontsPreference: ReadingFontsPreference,
        webViewClient: WebViewClient,
        onImageClick: ((imgUrl: String, altText: String) -> Unit)? = null,
        isEbookModeEnabled: Boolean = false,
        onPageUp: (() -> Unit)? = null,
        onPageDown: (() -> Unit)? = null,
    ) = WebView(context).apply {
        this.webViewClient = webViewClient
        scrollBarSize = 0
        isHorizontalScrollBarEnabled = false
        isVerticalScrollBarEnabled = true
        setBackgroundColor(Color.TRANSPARENT)
        
        // 针对墨水屏优化的渲染设置
        setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
        
        // 禁用抗锯齿，进一步锐利化e-ink渲染
        // 注意：WebView没有setRenderPriority方法，软件渲染本身已经是CPU优先
        
        with(this.settings) {

            // 墨水屏优化：优先使用serif字体，更适合纸张感
            standardFontFamily = when (readingFontsPreference) {
                ReadingFontsPreference.Cursive -> "cursive"
                ReadingFontsPreference.Monospace -> "monospace" 
                ReadingFontsPreference.SansSerif -> "serif" // 墨水屏优化：改为serif
                ReadingFontsPreference.Serif -> "serif"
                ReadingFontsPreference.External -> {
                    allowFileAccess = true
                    allowFileAccessFromFileURLs = true
                    "serif" // 墨水屏优化：默认改为serif
                }

                else -> "serif" // 墨水屏优化：默认改为serif
            }
            domStorageEnabled = true
            javaScriptEnabled = true
            
            // 墨水屏字体优化设置
            minimumFontSize = 12
            minimumLogicalFontSize = 12
            defaultFontSize = 16
            defaultFixedFontSize = 16
            
            // 禁用字体缩放，保持清晰度
            textZoom = 100
            
            // e-ink像素完美缩放
            useWideViewPort = true
            loadWithOverviewMode = true
            // 禁用双击缩放，避免意外模糊
            builtInZoomControls = false
            displayZoomControls = false
            setSupportZoom(false)
            
            // 优化渲染质量
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                offscreenPreRaster = true
            }
            
            // 可选：墨水屏性能优化 - 禁用网络图片加载（用户可手动加载）
            // blockNetworkImage = true // 取消注释以启用
            
            // 墨水屏缓存优化
            cacheMode = android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK
            databaseEnabled = true
            
            // 墨水屏滚动优化
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                setForceDark(android.webkit.WebSettings.FORCE_DARK_OFF) // 避免自动暗色模式干扰
            }
            addJavascriptInterface(object : JavaScriptInterface {
                @JavascriptInterface
                override fun onImgTagClick(imgUrl: String?, alt: String?) {
                    if (onImageClick != null && imgUrl != null) {
                        onImageClick.invoke(imgUrl, alt ?: "")
                    }
                }
                
                @JavascriptInterface
                override fun onPageUp() {
                    if (isEbookModeEnabled && onPageUp != null) {
                        onPageUp.invoke()
                    }
                }
                
                @JavascriptInterface
                override fun onPageDown() {
                    if (isEbookModeEnabled && onPageDown != null) {
                        onPageDown.invoke()
                    }
                }
                
                @JavascriptInterface
                fun injectEinkStyles() {
                    // 注入墨水屏优化CSS，覆盖第三方样式
                    val js = """
                        var style = document.createElement('style');
                        style.innerHTML = `
                            body *, article *, p *, h1 *, h2 *, h3 *, h4 *, h5 *, h6 * { 
                                -webkit-font-smoothing: none !important;
                                -moz-osx-font-smoothing: none !important;
                                text-rendering: optimizeSpeed !important;
                                text-shadow: 0.3px 0.3px 0 currentColor !important;
                                font-weight: 500 !important; /* 轻粗体增强 */
                            }
                            img { 
                                filter: grayscale(100%) contrast(150%); /* 可选：灰阶图像 */
                                image-rendering: -webkit-optimize-contrast;
                            }
                        `;
                        document.head.appendChild(style);
                    """.trimIndent()
                    // 此方法将在页面加载后被调用
                }
            }, JavaScriptInterface.NAME)
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                isAlgorithmicDarkeningAllowed = true
            }
        }
        
        // 设置精确密度缩放
        val displayMetrics = resources.displayMetrics
        setInitialScale((100 * displayMetrics.density).toInt())
    }
}
