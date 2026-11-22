package com.example.rag

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

object OfflineLaTeXAssetInstaller {
    private const val TAG = "OfflineLaTeXInstaller"

    fun ensureKaTeXAssetsInstalled(context: Context): File {
        val latexDir = File(context.filesDir, "cosmium_latex")
        val marker = File(latexDir, ".installed_ok")
        val hasCoreAssets = marker.exists() &&
            File(latexDir, "katex.min.js").exists() &&
            File(latexDir, "katex.min.css").exists() &&
            File(latexDir, "contrib/auto-render.min.js").exists() &&
            File(latexDir, "fonts/KaTeX_Main-Regular.woff2").exists()
        if (hasCoreAssets) {
            return latexDir
        }
        latexDir.mkdirs()
        runCatching {
            context.assets.open("katex_bundle.zip").use { zis ->
                ZipInputStream(zis).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val outFile = File(latexDir, entry.name)
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            FileOutputStream(outFile).use { fos ->
                                val buf = ByteArray(8192)
                                var len = zip.read(buf)
                                while (len > 0) {
                                    fos.write(buf, 0, len)
                                    len = zip.read(buf)
                                }
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            }
            marker.writeText("ok")
        }.onFailure { e ->
            Log.e(TAG, "Failed to extract offline KaTeX bundle: ${e.message}", e)
        }
        return latexDir
    }
}
