package com.example.utils

import com.example.data.ChatMessage
import com.example.rag.OfflineRAGManager
import java.util.UUID

object OfflineCosmiumKnowledgeEngine {

    suspend fun generateRagOfflineAnswer(
        ragManager: OfflineRAGManager,
        query: String,
        standardId: String,
        streamId: String
    ): ChatMessage {
        val result = ragManager.answerOffline(
            query = query,
            standardId = standardId,
            streamId = streamId
        )
        val stdGrade = when {
            standardId.contains("9") -> 9
            standardId.contains("10") -> 10
            standardId.contains("12") -> 12
            else -> 11
        }
        val sanitizedText = NativeCoreBridge.sanitizeAndFormatMath(result.answerText, stdGrade, streamId)

        val sourceLabels = result.retrievedChunks.map { hit ->
            "${hit.chunk.subjectName} Ch.${hit.chunk.chapterNumber} • ${hit.chunk.topicTitle}"
        }
        return ChatMessage(
            id = UUID.randomUUID().toString(),
            text = sanitizedText,
            isUser = false,
            ragSources = sourceLabels,
            brainBadge = null
        )
    }

    fun generateOfflineAnswer(query: String): ChatMessage {
        val q = query.lowercase()
        val text = when {
            q.contains("limit") || q.contains("calculus") || q.contains("derivative") || q.contains("integral") -> {
                "### Cosmium Study Guide • Calculus\n\n" +
                    "1. **Core Concept:** Limits and derivatives measure instantaneous rates of change. The derivative \$f'(x)\$ gives the exact slope of the tangent line at \$x\$.\n\n" +
                    "2. **Power Rule:**\n\n\$\$\n\\frac{d}{dx}(x^n) = n x^{n-1}\n\$\$\n\n" +
                    "3. **Example:** If \$f(x) = x^3 - 4x\$, then applying the power rule gives \$f'(x) = 3x^2 - 4\$."
            }
            q.contains("work energy") || q.contains("work-energy") || (q.contains("work") && q.contains("kinetic")) -> {
                "The **Work-Energy Theorem** states that the **net work done by all forces acting on a particle equals the change in its kinetic energy**.\n\n" +
                    "### 1. Mathematical Formulation\n\n" +
                    "Mathematically, the theorem connects net work \$W_{\\text{net}}\$ directly to initial speed \$v_i\$ and final speed \$v_f\$:\n\n" +
                    "\$\$\nW_{\\text{net}} = \\Delta K = K_f - K_i = \\frac{1}{2}m v_f^2 - \\frac{1}{2}m v_i^2\n\$\$\n\n" +
                    "**where:**\n" +
                    "- \$W_{\\text{net}}\$ is net work done by all forces acting on the mass \$m\$.\n" +
                    "- \$K_i = \\frac{1}{2}m v_i^2\$ and \$K_f = \\frac{1}{2}m v_f^2\$ are initial and final kinetic energies.\n\n" +
                    "### 2. Why It Works (Derivation)\n\n" +
                    "Starting from Newton's Second Law \$\\vec{F}_{\\text{net}} = m \\frac{d\\vec{v}}{dt}\$ and integrating \$d\\vec{r} = \\vec{v}\\,dt\$ yields \$\\int_{v_i}^{v_f} m\\vec{v}\\cdot d\\vec{v} = \\frac{1}{2}mv_f^2 - \\frac{1}{2}mv_i^2 = \\Delta K\$.\n\n" +
                    "### 3. Crucial Nuances & Common Pitfalls\n\n" +
                    "- **Include every force doing work:** Combine work by gravity, applied pulls, springs, and kinetic friction.\n" +
                    "- **Perpendicular forces do zero work:** Normal force on a track or centripetal force contributes \$0\$ work.\n\n" +
                    "### 4. Quick Worked Example\n\n" +
                    "A \$2\\text{ kg}\$ block speeds up from rest to \$v_f = 5\\text{ m/s}\$. Its net work is \$W_{\\text{net}} = \\frac{1}{2}(2)(5^2) = 25\\text{ J}\$."
            }
            q.contains("newton") || q.contains("force") || q.contains("velocity") || q.contains("physics") || q.contains("motion") -> {
                "### Cosmium Study Guide • Physics\n\n" +
                    "1. **Newton's Second Law & Kinematics:**\n\n\$\$\nF = m \\cdot a\n\$\$\n\n\$\$\nv = u + at\n\$\$\n\n\$\$\ns = ut + \\frac{1}{2}at^2\n\$\$\n\n" +
                    "2. **Tip:** Always resolve vector forces like \$\\vec{F}\$ along normal and tangential axes before writing equations."
            }
            q.contains("account") || q.contains("debit") || q.contains("credit") || q.contains("ratio") -> {
                "### Cosmium Study Guide • Accountancy\n\n" +
                    "1. **Accounting Equation:**\n\n\$\$\n\\text{Assets} = \\text{Liabilities} + \\text{Capital}\n\$\$\n\n" +
                    "2. **Current Ratio:**\n\n\$\$\n\\text{Current Ratio} = \\frac{\\text{Current Assets}}{\\text{Current Liabilities}}\n\$\$"
            }
            q.contains("python") || q.contains("code") || q.contains("debug") || q.contains("program") -> {
                "### Cosmium Study Guide • Coding\n\n" +
                    "```python\n" +
                    "def solve_scholastic_challenge(data_list):\n" +
                    "    valid_scores = [x for x in data_list if isinstance(x, (int, float)) and x >= 0]\n" +
                    "    total = sum(valid_scores)\n" +
                    "    average = total / len(valid_scores) if valid_scores else 0\n" +
                    "    return {'total': total, 'average': average}\n" +
                    "```"
            }
            else -> {
                "### Cosmium Study Guide\n\n" +
                    "Ready to help offline with: **\"\$query\"**.\n\n" +
                    "Try asking a focused question from your Mathematics, Physics, Chemistry, Accountancy, Economics, or Biology chapters for a step-by-step walkthrough."
            }
        }

        val sanitizedText = NativeCoreBridge.sanitizeAndFormatMath(text, 11, "science_pcm")

        return ChatMessage(
            id = UUID.randomUUID().toString(),
            text = sanitizedText,
            isUser = false,
            brainBadge = null
        )
    }
}
