package com.example.ui

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.rag.OfflineRAGManager
import com.example.rag.RagRoomDatabase
import com.example.ui.components.buildMarkdownWithProtectedMathHtml
import com.example.ui.components.formatReadableMathFallback
import com.example.utils.NativeCoreBridge
import com.example.utils.OfflineCosmiumKnowledgeEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CosmosMathViewTest {

    @Test
    fun buildMarkdownWithProtectedMathHtml_isolatesDisplayBlocksAndPreservesInlineMath() {
        val sample = """
            ### Newton's Second Law
            
            The net force ${'$'}\vec{F}${'$'} on a particle of mass ${'$'}m${'$'} produces acceleration ${'$'}a${'$'}:
            
            ${'$'}${'$'}
            \vec{F} = m \cdot \vec{a}
            = \frac{d\vec{p}}{dt}
            ${'$'}${'$'}
            
            - **Key Result:** Always keep ${'$'}\vec{F}${'$'} and ${'$'}\vec{a}${'$'} collinear.
            1. Compute ${'$'}\sqrt{F_x^2 + F_y^2}${'$'}.
        """.trimIndent()

        val html = buildMarkdownWithProtectedMathHtml(sample)

        // Markdown rendered properly
        assertTrue(html.contains("<h3>Newton's Second Law</h3>"))
        assertTrue(html.contains("<strong>Key Result:</strong>"))
        assertTrue(html.contains("<ul>") && html.contains("<li>"))
        assertTrue(html.contains("<ol>"))

        // Inline math stays inside <p> and <li>
        assertTrue(html.contains("<p>The net force \$\\vec{F}\$ on a particle of mass \$m\$ produces acceleration \$a\$:</p>"))
        assertTrue(html.contains("<li>Compute \$\\sqrt{F_x^2 + F_y^2}\$.</li>"))

        // Display math sits in dedicated scrollable .math-display-block outside <p> and keeps raw newlines (no <br/> inside math)
        assertTrue(html.contains("<div class=\"math-display-block\">\$\$\n\\vec{F} = m \\cdot \\vec{a}\n= \\frac{d\\vec{p}}{dt}\n\$\$</div>"))
        assertFalse(html.contains("\\vec{F} = m \\cdot \\vec{a}<br/>"))
    }

    @Test
    fun buildMarkdownWithProtectedMathHtml_normalizesBracketAndFencedLatexAndBareCommands() {
        val sample = """
            Inline \(x = \frac{-b \pm \sqrt{b^2 - 4ac}}{2a}\) and bare \sqrt{a^2 + b^2} and \vec{v}.
            
            ```latex
            \int_0^1 x^2 \, dx = \frac{1}{3}
            ```
            
            \[
            E = mc^2
            \]
        """.trimIndent()

        val html = buildMarkdownWithProtectedMathHtml(sample)

        assertFalse("Fenced latex block should not leave backticks", html.contains("```"))
        assertTrue(html.contains("\$x = \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}\$"))
        assertTrue("Bare \\sqrt must be wrapped in $...$", html.contains("\$\\sqrt{a^2 + b^2}\$"))
        assertTrue("Bare \\vec must be wrapped in $...$", html.contains("\$\\vec{v}\$"))
        assertTrue(html.contains("<div class=\"math-display-block\">\$\$\n\\int_0^1 x^2 \\, dx = \\frac{1}{3}\n\$\$</div>"))
        assertTrue(html.contains("<div class=\"math-display-block\">\$\$\nE = mc^2\n\$\$</div>"))
    }

    @Test
    fun formatReadableMathFallback_convertsRawLatexToCleanUnicode() {
        val raw = "### Step 1\nSolve **\$x = \\frac{-b \\pm \\sqrt{b^2-4ac}}{2a}\$** with \$\\vec{F}\$"
        val readable = formatReadableMathFallback(raw)

        assertFalse(readable.contains("$"))
        assertFalse(readable.contains("###"))
        assertFalse(readable.contains("**"))
        assertFalse(readable.contains("\\frac"))
        assertFalse(readable.contains("\\sqrt"))
        assertTrue(readable.contains("±"))
        assertTrue(readable.contains("√("))
        assertTrue(readable.contains("F⃗"))
    }

    @Test
    fun offlineEngines_emitTextbookLatexAndMarkdownCompletelyOffline() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        RagRoomDatabase.resetInstanceForTesting()
        app.deleteDatabase(RagRoomDatabase.ENCRYPTED_DB_NAME)

        val quickAnswer = OfflineCosmiumKnowledgeEngine.generateOfflineAnswer("Explain Newton's second law of motion")
        assertTrue(quickAnswer.text.contains("### "))
        assertTrue(quickAnswer.text.contains("\$\$\nF = m \\cdot a\n\$\$"))
        assertTrue(quickAnswer.text.contains("\$\\vec{F}\$"))

        val manager = OfflineRAGManager(app)
        manager.installOrReindexPack("class_10", "general")
        val ragAnswer = OfflineCosmiumKnowledgeEngine.generateRagOfflineAnswer(
            ragManager = manager,
            query = "Explain quadratic formula roots discriminant",
            standardId = "class_10",
            streamId = "general"
        )
        assertTrue("Offline RAG must format formula in display block", ragAnswer.text.contains("\$\$"))
        assertTrue("Offline formula must convert ASCII +- / sqrt to LaTeX", ragAnswer.text.contains("\\pm") || ragAnswer.text.contains("\\sqrt"))
        assertFalse("Offline formula must not leak raw ASCII sqrt(", ragAnswer.text.contains("sqrt("))

        val promptHint = NativeCoreBridge.calibrateCosmosPrompt("Derive quadratic formula", 10, "general")
        assertTrue(promptHint.contains("\$...\$"))
        assertTrue(promptHint.contains("\$\$...\$\$"))

        RagRoomDatabase.resetInstanceForTesting()
    }
}
