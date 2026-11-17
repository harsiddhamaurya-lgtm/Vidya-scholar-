package com.example.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeCoreBridgeTest {

    @Test
    fun cosineSimilarity_computesNormalizedDotProductCorrectly() {
        val a = floatArrayOf(1f, 0f, 0f)
        val b = floatArrayOf(1f, 0f, 0f)
        val c = floatArrayOf(0f, 1f, 0f)

        assertEquals(1.0f, NativeCoreBridge.cosineSimilarity(a, b), 0.001f)
        assertEquals(0.0f, NativeCoreBridge.cosineSimilarity(a, c), 0.001f)
        assertEquals(0.0f, NativeCoreBridge.cosineSimilarity(FloatArray(0), b), 0.001f)
    }

    @Test
    fun encodeConceptVector_returnsUnitLength384DimVectorAndPreservesSemanticOverlap() {
        val v1 = NativeCoreBridge.encodeConceptVector("Calculate the derivative using chain rule", 384)
        val v2 = NativeCoreBridge.encodeConceptVector("Differentiation slope and calculus tangent rate", 384)
        val v3 = NativeCoreBridge.encodeConceptVector("Accounting balance sheet assets liabilities", 384)

        assertEquals(384, v1.size)
        assertEquals(384, v2.size)
        val simRelated = NativeCoreBridge.cosineSimilarity(v1, v2)
        val simUnrelated = NativeCoreBridge.cosineSimilarity(v1, v3)
        assertTrue("Semantic concept overlap ($simRelated) should exceed unrelated ($simUnrelated)", simRelated > simUnrelated)
    }

    @Test
    fun evaluateQuizAnswer_normalizesSpokenMathAndKeywords() {
        assertTrue(
            NativeCoreBridge.evaluateQuizAnswer(
                studentAnswer = "v equals u plus at",
                canonicalAnswer = "v = u + at",
                expectedKeywords = listOf("u + at", "v = u")
            )
        )
        assertTrue(
            NativeCoreBridge.evaluateQuizAnswer(
                studentAnswer = "Assets = Liabilities + Capital",
                canonicalAnswer = "Assets = Liabilities + Capital",
                expectedKeywords = listOf("liabilities + capital")
            )
        )
        assertFalse(
            NativeCoreBridge.evaluateQuizAnswer(
                studentAnswer = "photosynthesis",
                canonicalAnswer = "v = u + at",
                expectedKeywords = listOf("u + at", "v = u")
            )
        )
    }

    @Test
    fun chooseLudoBestToken_prioritizesCaptureThenHomeThenExitYard() {
        // Token 0 at pos 10 with dice 3 -> 13 (safe zone, not capture), Token 1 at pos 5 with dice 3 -> 8 (safe), Token 2 at pos 1 -> 4 (enemy at 4!)
        val idxCapture = NativeCoreBridge.chooseLudoBestToken(
            tokenPositions = intArrayOf(10, 5, 1),
            tokenStartTrackIndices = intArrayOf(0, 0, 0),
            enemyPositions = intArrayOf(4),
            dice = 3,
            safeZones = intArrayOf(0, 8, 13, 21, 26, 34, 39, 47)
        )
        assertEquals(2, idxCapture)

        // Token 1 can enter center home (57) on dice 2 from pos 55
        val idxHome = NativeCoreBridge.chooseLudoBestToken(
            tokenPositions = intArrayOf(10, 55),
            tokenStartTrackIndices = intArrayOf(0, 0),
            enemyPositions = intArrayOf(),
            dice = 2,
            safeZones = intArrayOf(0, 8, 13, 21, 26, 34, 39, 47)
        )
        assertEquals(1, idxHome)
    }

    @Test
    fun chooseChaturangaBestMove_prioritizesRajaCaptureOverRegularCapture() {
        val idx = NativeCoreBridge.chooseChaturangaBestMove(
            capturedPoints = intArrayOf(5, 3, 0),
            capturedIsRaja = booleanArrayOf(false, true, false),
            toRows = intArrayOf(0, 2, 3),
            toCols = intArrayOf(0, 2, 3)
        )
        assertEquals(1, idx)
    }
}
