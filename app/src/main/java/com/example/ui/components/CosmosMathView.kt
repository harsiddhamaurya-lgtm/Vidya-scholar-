package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.rag.OfflineLaTeXAssetInstaller
import com.example.ui.theme.LocalIndoPalette
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import kotlin.math.max

private const val COSMIUM_LOCAL_ORIGIN = "https://cosmium.local/"

private val RICH_MATH_OR_MARKDOWN_REGEX = Regex(
    """\$\$|\$[^$\n]+\$|\\\(|\\\)|\\\[|\\\]|\\(?:frac|sqrt|vec|int|sum|lim|rightarrow|rightleftharpoons|pm|cdot|times|text|mathbf|begin)\b|\*\*[^*]+\*\*|^\s{0,3}#{1,4}\s+|^\s*[-*•]\s+|^\s*\d+\.\s+|`[^`]+`""",
    setOf(RegexOption.MULTILINE)
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CosmosMathProseView(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    if (!RICH_MATH_OR_MARKDOWN_REGEX.containsMatchIn(text)) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 22.sp,
            color = textColor,
            modifier = modifier
        )
        return
    }

    val context = LocalContext.current
    val palette = LocalIndoPalette.current
    var hasError by remember { mutableStateOf(false) }

    val latexDir = remember {
        OfflineLaTeXAssetInstaller.ensureKaTeXAssetsInstalled(context)
    }
    val hasKaTeX = remember(latexDir) {
        File(latexDir, "katex.min.js").exists() &&
            File(latexDir, "katex.min.css").exists() &&
            File(latexDir, "contrib/auto-render.min.js").exists()
    }

    if (!hasKaTeX || hasError) {
        Text(
            text = formatReadableMathFallback(text),
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 22.sp,
            color = textColor,
            modifier = modifier
        )
        return
    }

    val hexColor = remember(textColor) {
        String.format("#%06X", 0xFFFFFF and textColor.toArgb())
    }
    val accentHex = remember(palette.accentSaffron) {
        String.format("#%06X", 0xFFFFFF and palette.accentSaffron.toArgb())
    }
    val cyanHex = remember(palette.accentCyan) {
        String.format("#%06X", 0xFFFFFF and palette.accentCyan.toArgb())
    }
    val htmlContent = remember(text, hexColor, accentHex, cyanHex, palette.isDark) {
        buildOfflineKaTeXHtml(
            rawText = text,
            hexColor = hexColor,
            accentHex = accentHex,
            cyanHex = cyanHex,
            isDark = palette.isDark
        )
    }

    // Seed with a sensible minimum height to avoid 0dp layout flashes before WebView reports exact height
    val estimatedInitialDp = remember(text) {
        val lineCount = max(2, text.lines().size)
        (lineCount * 22 + 24).coerceIn(36, 420)
    }
    var measuredHeightDp by remember(text) { mutableIntStateOf(estimatedInitialDp) }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(measuredHeightDp.dp),
        factory = { ctx ->
            createKaTeXWebView(
                context = ctx,
                latexDir = latexDir,
                htmlContent = htmlContent,
                onHeightChangedDp = { newHeightDp ->
                    if (newHeightDp > 12 && newHeightDp != measuredHeightDp) {
                        measuredHeightDp = newHeightDp
                    }
                },
                onError = { hasError = true }
            ) ?: android.view.View(ctx)
        },
        update = { v ->
            if (v is TouchFriendlyMathWebView && v.lastLoadedHtml != htmlContent) {
                v.lastLoadedHtml = htmlContent
                runCatching {
                    v.loadDataWithBaseURL(COSMIUM_LOCAL_ORIGIN, htmlContent, "text/html", "utf-8", null)
                }.onFailure { hasError = true }
            }
        }
    )
}

private class TouchFriendlyMathWebView(context: Context) : WebView(context) {
    var lastLoadedHtml: String = ""

    @Volatile
    var horizontalMathScrollActive: Boolean = false

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                if (horizontalMathScrollActive) {
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                horizontalMathScrollActive = false
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}

private class MathWebViewBridge(
    private val webView: TouchFriendlyMathWebView,
    private val onHeightChangedDp: (Int) -> Unit
) {
    @JavascriptInterface
    fun onContentHeight(heightCssPx: Int) {
        if (heightCssPx > 0) {
            webView.post {
                onHeightChangedDp(heightCssPx + 6)
            }
        }
    }

    @JavascriptInterface
    fun setHorizontalScrollActive(active: Boolean) {
        webView.horizontalMathScrollActive = active
    }
}

@SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
private fun createKaTeXWebView(
    context: Context,
    latexDir: File,
    htmlContent: String,
    onHeightChangedDp: (Int) -> Unit,
    onError: () -> Unit
): TouchFriendlyMathWebView? {
    return runCatching {
        TouchFriendlyMathWebView(context).apply {
            setBackgroundColor(0) // Transparent
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            overScrollMode = WebView.OVER_SCROLL_NEVER
            settings.javaScriptEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.domStorageEnabled = false
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            settings.useWideViewPort = false
            settings.loadWithOverviewMode = false

            addJavascriptInterface(MathWebViewBridge(this, onHeightChangedDp), "MathBridge")

            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    val uri = request?.url ?: return null
                    val urlStr = uri.toString()
                    if (!urlStr.startsWith(COSMIUM_LOCAL_ORIGIN)) {
                        // Block any accidental external CDN request for 100% offline guarantee
                        return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                    }
                    val relPath = urlStr.removePrefix(COSMIUM_LOCAL_ORIGIN).substringBefore('?').substringBefore('#')
                    val targetFile = File(latexDir, relPath).canonicalFile
                    if (!targetFile.path.startsWith(latexDir.canonicalPath) || !targetFile.exists() || !targetFile.isFile) {
                        return null
                    }
                    val mime = when {
                        relPath.endsWith(".css") -> "text/css"
                        relPath.endsWith(".js") -> "application/javascript"
                        relPath.endsWith(".woff2") -> "font/woff2"
                        relPath.endsWith(".woff") -> "font/woff"
                        relPath.endsWith(".ttf") -> "font/ttf"
                        relPath.endsWith(".json") -> "application/json"
                        else -> "application/octet-stream"
                    }
                    return runCatching {
                        WebResourceResponse(mime, "UTF-8", FileInputStream(targetFile))
                    }.getOrNull()
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    view?.evaluateJavascript("if (window.__renderCosmiumMath) { window.__renderCosmiumMath(); }", null)
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    if (request?.isForMainFrame == true) {
                        onError()
                    }
                }
            }
            lastLoadedHtml = htmlContent
            loadDataWithBaseURL(COSMIUM_LOCAL_ORIGIN, htmlContent, "text/html", "utf-8", null)
        }
    }.getOrNull()
}

internal fun buildMarkdownWithProtectedMathHtml(rawText: String): String =
    com.example.utils.NativeCoreBridge.buildMarkdownWithProtectedMathHtml(rawText)

internal fun formatReadableMathFallback(rawText: String): String =
    com.example.utils.NativeCoreBridge.formatReadableMathFallback(rawText)

private fun buildOfflineKaTeXHtml(
    rawText: String,
    hexColor: String,
    accentHex: String,
    cyanHex: String,
    isDark: Boolean
): String {
    val bodyHtml = buildMarkdownWithProtectedMathHtml(rawText)
    val mathBlockBg = if (isDark) "rgba(255, 255, 255, 0.06)" else "rgba(232, 141, 20, 0.07)"
    val mathBlockBorder = if (isDark) "rgba(212, 175, 55, 0.30)" else "rgba(212, 175, 55, 0.38)"
    val codeBg = if (isDark) "rgba(124, 232, 255, 0.12)" else "rgba(0, 136, 169, 0.09)"

    return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
          <link rel="stylesheet" href="katex.min.css">
          <script src="katex.min.js"></script>
          <script src="contrib/auto-render.min.js"></script>
          <script src="contrib/mhchem.min.js"></script>
          <style>
            * {
              box-sizing: border-box;
            }
            html, body {
              margin: 0;
              padding: 0;
              width: 100%;
              max-width: 100%;
              overflow-x: hidden;
              background: transparent;
              color: $hexColor;
              font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
              font-size: 14px;
              line-height: 1.58;
            }
            #math-content {
              width: 100%;
              max-width: 100%;
              overflow-x: hidden;
              word-wrap: break-word;
              overflow-wrap: anywhere;
            }
            #math-content > :first-child {
              margin-top: 0;
            }
            #math-content > :last-child {
              margin-bottom: 0;
            }
            h1, h2, h3, h4 {
              margin: 10px 0 6px 0;
              font-weight: 700;
              line-height: 1.32;
              color: $hexColor;
            }
            h1 { font-size: 1.16em; }
            h2 { font-size: 1.10em; }
            h3 { font-size: 1.04em; color: $accentHex; }
            h4 { font-size: 0.98em; color: $cyanHex; }
            p {
              margin: 0 0 8px 0;
            }
            ul, ol {
              margin: 4px 0 10px 0;
              padding-left: 20px;
            }
            li {
              margin: 4px 0;
            }
            blockquote {
              margin: 8px 0;
              padding: 6px 12px;
              border-left: 3px solid $accentHex;
              background: $mathBlockBg;
              border-radius: 0 8px 8px 0;
            }
            code {
              font-family: monospace;
              font-size: 0.90em;
              padding: 1px 5px;
              border-radius: 5px;
              background: $codeBg;
              color: $cyanHex;
            }
            /* Separate centered/padded block for display equations $$...$$ with horizontal touch scrolling */
            .math-display-block {
              display: block;
              width: 100%;
              max-width: 100%;
              margin: 10px 0;
              padding: 12px 14px;
              border-radius: 12px;
              background: $mathBlockBg;
              border: 1px solid $mathBlockBorder;
              overflow-x: auto;
              overflow-y: hidden;
              -webkit-overflow-scrolling: touch;
              text-align: center;
              touch-action: pan-x pan-y;
            }
            .math-display-block .katex-display {
              margin: 0 auto !important;
              padding: 2px 4px;
              display: inline-block;
              min-width: 100%;
              width: max-content;
              text-align: center;
              overflow: visible;
            }
            .katex {
              font-size: 1.05em;
              vertical-align: baseline;
            }
            .katex-display {
              margin: 0.6em 0;
              overflow-x: auto;
              overflow-y: hidden;
              -webkit-overflow-scrolling: touch;
              max-width: 100%;
            }
          </style>
        </head>
        <body>
          <div id="math-content">$bodyHtml</div>
          <script>
            (function() {
              function reportHeight() {
                if (window.MathBridge && window.MathBridge.onContentHeight) {
                  var el = document.getElementById("math-content");
                  var h = Math.ceil((el ? el.scrollHeight : document.body.scrollHeight) || 24);
                  window.MathBridge.onContentHeight(h);
                }
              }

              function bindHorizontalScrollGuards() {
                var blocks = document.querySelectorAll(".math-display-block, .katex-display");
                blocks.forEach(function(block) {
                  if (block.__cosmiumTouchBound) return;
                  block.__cosmiumTouchBound = true;
                  block.addEventListener("touchstart", function() {
                    var overflows = block.scrollWidth > block.clientWidth + 2;
                    if (window.MathBridge && window.MathBridge.setHorizontalScrollActive) {
                      window.MathBridge.setHorizontalScrollActive(overflows);
                    }
                  }, { passive: true });
                  block.addEventListener("touchend", function() {
                    if (window.MathBridge && window.MathBridge.setHorizontalScrollActive) {
                      window.MathBridge.setHorizontalScrollActive(false);
                    }
                  }, { passive: true });
                  block.addEventListener("touchcancel", function() {
                    if (window.MathBridge && window.MathBridge.setHorizontalScrollActive) {
                      window.MathBridge.setHorizontalScrollActive(false);
                    }
                  }, { passive: true });
                });
              }

              window.__renderCosmiumMath = function() {
                var root = document.getElementById("math-content");
                if (!root) return;
                if (!root.__katexRendered && typeof renderMathInElement !== "undefined") {
                  renderMathInElement(root, {
                    delimiters: [
                      {left: "$$", right: "$$", display: true},
                      {left: "$", right: "$", display: false},
                      {left: "\\(", right: "\\)", display: false},
                      {left: "\\[", right: "\\]", display: true}
                    ],
                    throwOnError: false,
                    strict: "ignore"
                  });
                  root.__katexRendered = true;
                }
                bindHorizontalScrollGuards();
                reportHeight();
                if (document.fonts && document.fonts.ready) {
                  document.fonts.ready.then(function() {
                    reportHeight();
                  });
                }
                setTimeout(reportHeight, 60);
                setTimeout(reportHeight, 220);
              };

              document.addEventListener("DOMContentLoaded", window.__renderCosmiumMath);
              window.addEventListener("load", window.__renderCosmiumMath);
            })();
          </script>
        </body>
        </html>
    """.trimIndent()
}
