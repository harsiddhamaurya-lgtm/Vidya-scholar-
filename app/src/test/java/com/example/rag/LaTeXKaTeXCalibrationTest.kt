package com.example.rag

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.utils.NativeCoreBridge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LaTeXKaTeXCalibrationTest {

    @Test
    fun offlineLaTeXAssetInstaller_extractsAllKaTeXBundlesToInternalStorage() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val latexDir = OfflineLaTeXAssetInstaller.ensureKaTeXAssetsInstalled(app)

        assertTrue("LaTeX directory must exist", latexDir.exists())
        assertTrue("katex.min.js must exist", File(latexDir, "katex.min.js").exists())
        assertTrue("katex.min.css must exist", File(latexDir, "katex.min.css").exists())
        assertTrue("auto-render.min.js must exist", File(latexDir, "contrib/auto-render.min.js").exists())
        assertTrue("mhchem.min.js must exist", File(latexDir, "contrib/mhchem.min.js").exists())
        assertTrue("katex_parser_tables.json must exist", File(latexDir, "katex_parser_tables.json").exists())

        val fontsDir = File(latexDir, "fonts")
        assertTrue("fonts directory must exist", fontsDir.exists() && fontsDir.isDirectory)
        val woff2Files = fontsDir.listFiles { _, name -> name.endsWith(".woff2") } ?: emptyArray()
        assertTrue("Must contain offline WOFF2 math fonts", woff2Files.isNotEmpty())

        // Verify zero external CDN links in the offline bundle
        val jsContent = File(latexDir, "katex.min.js").readText()
        assertFalse("Must not contain unpkg CDN", jsContent.contains("unpkg.com"))
        assertFalse("Must not contain cdnjs CDN", jsContent.contains("cdnjs.cloudflare.com"))
        assertFalse("Must not contain jsdelivr CDN", jsContent.contains("cdn.jsdelivr.net"))
    }

    @Test
    fun nativeSanitizeAndFormatMath_cleansBrokenTokensAndBalancesDelimiters() {
        if (!NativeCoreBridge.isAvailable()) return

        // 1. Broken heading hashes attached to math
        val dirty = "###/ Solve for x: ###\$\$ x^2 + 5x + 6 = 0 \$\$ and find \$x"
        val cleaned = NativeCoreBridge.nativeSanitizeAndFormatMath(dirty, 10, "general")
        assertFalse(cleaned.contains("###/"))
        assertFalse(cleaned.contains("###$$"))
        assertTrue("Must balance trailing lone dollar", cleaned.endsWith("$"))

        // 2. Macro fallbacks
        val withMacros = "Current Ratio: \$\$\\tfrac{\\text{Current Assets}}{\\text{Current Liabilities}}\$\$ at 25\\degree"
        val cleanedMacros = NativeCoreBridge.nativeSanitizeAndFormatMath(withMacros, 12, "commerce")
        assertFalse(cleanedMacros.contains("\\tfrac"))
        assertTrue(cleanedMacros.contains("\\frac"))
        assertTrue(cleanedMacros.contains("^\\circ"))

        // 3. Display equation delimiter balancing
        val unclosedDisplay = "\$\$ F = m \\cdot a"
        val closedDisplay = NativeCoreBridge.nativeSanitizeAndFormatMath(unclosedDisplay, 9, "general")
        assertTrue("Unclosed $$ must be balanced", closedDisplay.contains("$$"))
        val dollarCount = closedDisplay.split("$$").size - 1
        assertEquals("$$ count must be even", 0, dollarCount % 2)
    }

    @Test
    fun nativeCalibrateCosmosPrompt_calibratesAcrossStandards9To12AndAllStreams() {
        if (!NativeCoreBridge.isAvailable()) return

        // Standard 9/10 Foundation
        val promptGrade9 = NativeCoreBridge.nativeCalibrateCosmosPrompt("Explain Newton's Laws and SS GDP", 9, "general")
        assertTrue(promptGrade9.contains("Foundation Standard 9"))
        assertTrue(promptGrade9.contains("clean, standard LaTeX"))
        assertTrue(promptGrade9.contains("balanced chemical equations"))

        // Standard 11 Science PCM
        val promptPcm = NativeCoreBridge.nativeCalibrateCosmosPrompt("Derive integration by parts", 11, "science_pcm")
        assertTrue(promptPcm.contains("Senior Secondary Standard 11"))
        assertTrue(promptPcm.contains("calculus"))
        assertTrue(promptPcm.contains("calibrated KaTeX"))

        // Standard 12 Commerce
        val promptCom = NativeCoreBridge.nativeCalibrateCosmosPrompt("Explain accounting equation and investment multiplier", 12, "commerce")
        assertTrue(promptCom.contains("Senior Secondary Standard 12"))
        assertTrue(promptCom.contains("Accountancy"))
        assertTrue(promptCom.contains("Economics/Statistics"))
    }
}
