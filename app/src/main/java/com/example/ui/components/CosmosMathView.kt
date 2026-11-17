package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.rag.OfflineLaTeXAssetInstaller
import java.io.File

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CosmosMathProseView(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    if (!text.contains("$") && !text.contains("\\(") && !text.contains("\\[")) {
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
    var hasError by remember { mutableStateOf(false) }

    val latexDir = remember {
        OfflineLaTeXAssetInstaller.ensureKaTeXAssetsInstalled(context)
    }
    val hasKaTeX = remember(latexDir) {
        File(latexDir, "katex.min.js").exists() && File(latexDir, "katex.min.css").exists()
    }

    if (!hasKaTeX || hasError) {
        Text(
            text = text,
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
    val htmlContent = remember(text, hexColor) {
        buildOfflineKaTeXHtml(text, hexColor)
    }

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            createKaTeXWebView(ctx, latexDir, htmlContent) {
                hasError = true
            } ?: android.view.View(ctx)
        },
        update = { v ->
            if (v is WebView) {
                runCatching {
                    v.loadDataWithBaseURL("file://${latexDir.absolutePath}/", htmlContent, "text/html", "utf-8", null)
                }.onFailure { hasError = true }
            }
        }
    )
}

@SuppressLint("SetJavaScriptEnabled")
private fun createKaTeXWebView(
    context: Context,
    latexDir: File,
    htmlContent: String,
    onError: () -> Unit
): WebView? {
    return runCatching {
        WebView(context).apply {
            setBackgroundColor(0) // Transparent
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            settings.javaScriptEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.domStorageEnabled = false
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            webViewClient = object : WebViewClient() {
                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    onError()
                }
            }
            loadDataWithBaseURL("file://${latexDir.absolutePath}/", htmlContent, "text/html", "utf-8", null)
        }
    }.getOrNull()
}

private fun buildOfflineKaTeXHtml(rawText: String, hexColor: String): String {
    // Escape HTML special characters in prose while keeping LaTeX syntax ($$ and $) intact
    val escaped = rawText
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\n", "<br/>")

    return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
          <link rel="stylesheet" href="katex.min.css">
          <script src="katex.min.js"></script>
          <script src="contrib/auto-render.min.js"></script>
          <script src="contrib/mhchem.min.js"></script>
          <style>
            body {
              margin: 0;
              padding: 2px 0;
              background: transparent;
              color: $hexColor;
              font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
              font-size: 14px;
              line-height: 1.55;
              word-wrap: break-word;
            }
            .katex-display {
              margin: 0.5em 0;
              overflow-x: auto;
              overflow-y: hidden;
            }
            .katex {
              font-size: 1.05em;
            }
          </style>
        </head>
        <body>
          <div id="math-content">$escaped</div>
          <script>
            document.addEventListener("DOMContentLoaded", function() {
              if (typeof renderMathInElement !== 'undefined') {
                renderMathInElement(document.getElementById("math-content"), {
                  delimiters: [
                    {left: "$$", right: "$$", display: true},
                    {left: "$", right: "$", display: false},
                    {left: "\\(", right: "\\)", display: false},
                    {left: "\\[", right: "\\]", display: true}
                  ],
                  throwOnError: false
                });
              }
            });
          </script>
        </body>
        </html>
    """.trimIndent()
}
