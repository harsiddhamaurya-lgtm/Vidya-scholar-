package com.example.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    fun ragTokenizerAndFtsBuilder_normalizeEquationsAndFilterStopWords() {
        val tokens = NativeCoreBridge.tokenizeRagText("Please explain Newton F=ma and Kirchhoff loop law")
        assertFalse(tokens.contains("please"))
        assertFalse(tokens.contains("explain"))
        assertTrue(tokens.contains("newton"))
        assertTrue(tokens.contains("kirchhoff"))

        val fts = NativeCoreBridge.buildSafeFtsMatchQuery("Kirchhoff's voltage loop!!")
        assertTrue(fts.contains("kirchhoff*"))
        assertTrue(fts.contains(" OR "))
    }

    @Test
    fun formatAsciiFormulaToLatex_convertsFormulaSymbolsCleanly() {
        val quad = NativeCoreBridge.formatAsciiFormulaToLatex("x = (-b +- sqrt(b^2 - 4ac)) / 2a")
        assertTrue(quad.contains("\\pm"))
        assertTrue(quad.contains("\\sqrt{b^2 - 4ac}"))
        assertTrue(quad.contains("\\frac"))

        val mirror = NativeCoreBridge.formatAsciiFormulaToLatex("1/f = 1/v + 1/u (mirror)")
        assertTrue(mirror.contains("\\text{(mirror)}"))
    }

    @Test
    fun ludoDestinationAndBot_computeExactCircuitStepsAndBestMove() {
        assertNull(NativeCoreBridge.getLudoDestinationPosition(position = -1, startTrackIndex = 0, dice = 4))
        assertEquals(0, NativeCoreBridge.getLudoDestinationPosition(position = -1, startTrackIndex = 0, dice = 6))
        assertEquals(57, NativeCoreBridge.getLudoDestinationPosition(position = 55, startTrackIndex = 0, dice = 2))
        assertNull(NativeCoreBridge.getLudoDestinationPosition(position = 55, startTrackIndex = 0, dice = 4))

        val idxCapture = NativeCoreBridge.chooseLudoBestToken(
            tokenPositions = intArrayOf(10, 5, 1),
            tokenStartTrackIndices = intArrayOf(0, 0, 0),
            enemyPositions = intArrayOf(4),
            dice = 3,
            safeZones = intArrayOf(0, 8, 13, 21, 26, 34, 39, 47)
        )
        assertEquals(2, idxCapture)

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
    fun chessEngineCore_validatesMovesCheckAndBotSearch() {
        val board = IntArray(64)
        board[7 * 8 + 4] = 6  // White King on (7, 4)
        board[6 * 8 + 4] = 1  // White Pawn on (6, 4) blocking rook check
        board[0 * 8 + 4] = -4 // Black Rook on (0, 4)
        board[0 * 8 + 0] = -6 // Black King on (0, 0)

        assertFalse(NativeCoreBridge.isChessKingAttacked(board, 1))
        val pawnMoves = NativeCoreBridge.getChessValidMoves(board, 1, 6, 4)
        assertTrue(pawnMoves.contains(5 * 8 + 4))
        assertTrue(pawnMoves.contains(4 * 8 + 4))

        // Remove blocking pawn -> White King is now in check from Black Rook
        board[6 * 8 + 4] = 0
        assertTrue(NativeCoreBridge.isChessKingAttacked(board, 1))
        assertTrue(NativeCoreBridge.hasAnyChessLegalMoves(board, 1))

        val botMove = NativeCoreBridge.computeChessBotMove(board, 1, 1)
        assertNotNull(botMove)
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

    @Test
    fun synthesizeGameMusicStep_producesNonEmptyPcmForAllModes() {
        for (ordinal in 0..2) {
            val pcm = NativeCoreBridge.synthesizeGameMusicStep(ordinal, noteIndex = 0, sampleRate = 22050, globalSampleStart = 0L)
            assertTrue("Mode $ordinal should synthesize non-empty PCM", pcm.size > 1000)
        }
    }
}
