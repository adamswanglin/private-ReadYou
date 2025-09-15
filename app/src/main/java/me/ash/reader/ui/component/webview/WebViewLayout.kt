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
        with(this.settings) {

            standardFontFamily = when (readingFontsPreference) {
                ReadingFontsPreference.Cursive -> "cursive"
                ReadingFontsPreference.Monospace -> "monospace"
                ReadingFontsPreference.SansSerif -> "sans-serif"
                ReadingFontsPreference.Serif -> "serif"
                ReadingFontsPreference.External -> {
                    allowFileAccess = true
                    allowFileAccessFromFileURLs = true
                    "sans-serif"
                }

                else -> "sans-serif"
            }
            domStorageEnabled = true
            javaScriptEnabled = true
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
            }, JavaScriptInterface.NAME)
            setSupportZoom(false)
            // 启用字体抗锯齿
            textZoom = 100
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                isAlgorithmicDarkeningAllowed = true
            }
        }
    }
}
