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
                "[Cosmium Study Guide • Calculus]\n\n" +
                    "1. Core Concept: Limits and derivatives measure instantaneous rates of change. The derivative \$f'(x)\$ is the exact slope of the tangent line at \$x\$.\n" +
                    "2. Power Rule:\n\$\$ \\frac{d}{dx}(x^n) = n x^{n-1} \$\$\n" +
                    "3. Example: If \$f(x) = x^3 - 4x\$, then \$f'(x) = 3x^2 - 4\$."
            }
            q.contains("newton") || q.contains("force") || q.contains("velocity") || q.contains("physics") || q.contains("motion") -> {
                "[Cosmium Study Guide • Physics]\n\n" +
                    "1. Newton's Second Law:\n\$\$ F = m \\cdot a \$\$\n\$\$ v = u + at \$\$\n\$\$ s = ut + \\frac{1}{2}at^2 \$\$\n" +
                    "2. Tip: Always resolve forces along normal and tangential axes before writing equations."
            }
            q.contains("account") || q.contains("debit") || q.contains("credit") || q.contains("ratio") -> {
                "[Cosmium Study Guide • Accountancy]\n\n" +
                    "1. Accounting Equation:\n\$\$ \\text{Assets} = \\text{Liabilities} + \\text{Capital} \$\$\n" +
                    "2. Current Ratio:\n\$\$ \\text{Current Ratio} = \\frac{\\text{Current Assets}}{\\text{Current Liabilities}} \$\$"
            }
            q.contains("python") || q.contains("code") || q.contains("debug") || q.contains("program") -> {
                "[Cosmium Study Guide • Coding]\n\n" +
                    "```python\n" +
                    "def solve_scholastic_challenge(data_list):\n" +
                    "    valid_scores = [x for x in data_list if isinstance(x, (int, float)) and x >= 0]\n" +
                    "    total = sum(valid_scores)\n" +
                    "    average = total / len(valid_scores) if valid_scores else 0\n" +
                    "    return {'total': total, 'average': average}\n" +
                    "```"
            }
            else -> {
                "[Cosmium Study Guide]\n\n" +
                    "Ready to help offline with: \"\$query\".\n\n" +
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
