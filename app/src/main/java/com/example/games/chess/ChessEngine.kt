package com.example.games.chess

import kotlin.math.abs
import kotlin.random.Random

enum class PieceColor { WHITE, BLACK }
enum class PieceType(val symbol: String, val value: Int) {
    PAWN("P", 100),
    KNIGHT("N", 320),
    BISHOP("B", 330),
    ROOK("R", 500),
    QUEEN("Q", 900),
    KING("K", 20000)
}

data class ChessPiece(
    val type: PieceType,
    val color: PieceColor
) {
    fun toChar(): Char {
        val c = when (type) {
            PieceType.PAWN -> 'p'
            PieceType.KNIGHT -> 'n'
            PieceType.BISHOP -> 'b'
            PieceType.ROOK -> 'r'
            PieceType.QUEEN -> 'q'
            PieceType.KING -> 'k'
        }
        return if (color == PieceColor.WHITE) c.uppercaseChar() else c
    }

    companion object {
        fun fromChar(c: Char): ChessPiece? {
            val isWhite = c.isUpperCase()
            val color = if (isWhite) PieceColor.WHITE else PieceColor.BLACK
            val type = when (c.lowercaseChar()) {
                'p' -> PieceType.PAWN
                'n' -> PieceType.KNIGHT
                'b' -> PieceType.BISHOP
                'r' -> PieceType.ROOK
                'q' -> PieceType.QUEEN
                'k' -> PieceType.KING
                else -> return null
            }
            return ChessPiece(type, color)
        }
    }
}

data class Position(val row: Int, val col: Int) {
    fun isValid(): Boolean = row in 0..7 && col in 0..7
}

data class ChessMove(
    val from: Position,
    val to: Position,
    val captured: ChessPiece? = null
)

data class ChessGameState(
    val board: Array<Array<ChessPiece?>>,
    val currentTurn: PieceColor = PieceColor.WHITE,
    val isCheck: Boolean = false,
    val isCheckmate: Boolean = false,
    val capturedWhite: List<ChessPiece> = emptyList(),
    val capturedBlack: List<ChessPiece> = emptyList(),
    val moveHistory: List<ChessMove> = emptyList()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ChessGameState
        return board.contentDeepEquals(other.board) &&
                currentTurn == other.currentTurn &&
                isCheck == other.isCheck &&
                isCheckmate == other.isCheckmate &&
                capturedWhite == other.capturedWhite &&
                capturedBlack == other.capturedBlack
    }

    override fun hashCode(): Int {
        var result = board.contentDeepHashCode()
        result = 31 * result + currentTurn.hashCode()
        result = 31 * result + isCheck.hashCode()
        result = 31 * result + isCheckmate.hashCode()
        result = 31 * result + capturedWhite.hashCode()
        result = 31 * result + capturedBlack.hashCode()
        return result
    }

    fun serialize(): String {
        val sb = StringBuilder()
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board[r][c]
                sb.append(piece?.toChar() ?: '.')
            }
        }
        sb.append("|").append(currentTurn.name)
        return sb.toString()
    }

    companion object {
        fun deserialize(data: String): ChessGameState? {
            try {
                val parts = data.split("|")
                if (parts.size < 2 || parts[0].length != 64) return null
                val board = Array(8) { Array<ChessPiece?>(8) { null } }
                for (i in 0 until 64) {
                    val r = i / 8
                    val c = i % 8
                    val ch = parts[0][i]
                    if (ch != '.') {
                        board[r][c] = ChessPiece.fromChar(ch)
                    }
                }
                val turn = if (parts[1] == PieceColor.BLACK.name) PieceColor.BLACK else PieceColor.WHITE
                return ChessGameState(board = board, currentTurn = turn)
            } catch (e: Exception) {
                return null
            }
        }

        fun initialBoard(): ChessGameState {
            val b = Array(8) { Array<ChessPiece?>(8) { null } }
            // Black back row (row 0)
            b[0][0] = ChessPiece(PieceType.ROOK, PieceColor.BLACK)
            b[0][1] = ChessPiece(PieceType.KNIGHT, PieceColor.BLACK)
            b[0][2] = ChessPiece(PieceType.BISHOP, PieceColor.BLACK)
            b[0][3] = ChessPiece(PieceType.QUEEN, PieceColor.BLACK)
            b[0][4] = ChessPiece(PieceType.KING, PieceColor.BLACK)
            b[0][5] = ChessPiece(PieceType.BISHOP, PieceColor.BLACK)
            b[0][6] = ChessPiece(PieceType.KNIGHT, PieceColor.BLACK)
            b[0][7] = ChessPiece(PieceType.ROOK, PieceColor.BLACK)
            for (c in 0..7) b[1][c] = ChessPiece(PieceType.PAWN, PieceColor.BLACK)

            // White back row (row 7)
            b[7][0] = ChessPiece(PieceType.ROOK, PieceColor.WHITE)
            b[7][1] = ChessPiece(PieceType.KNIGHT, PieceColor.WHITE)
            b[7][2] = ChessPiece(PieceType.BISHOP, PieceColor.WHITE)
            b[7][3] = ChessPiece(PieceType.QUEEN, PieceColor.WHITE)
            b[7][4] = ChessPiece(PieceType.KING, PieceColor.WHITE)
            b[7][5] = ChessPiece(PieceType.BISHOP, PieceColor.WHITE)
            b[7][6] = ChessPiece(PieceType.KNIGHT, PieceColor.WHITE)
            b[7][7] = ChessPiece(PieceType.ROOK, PieceColor.WHITE)
            for (c in 0..7) b[6][c] = ChessPiece(PieceType.PAWN, PieceColor.WHITE)

            return ChessGameState(board = b, currentTurn = PieceColor.WHITE)
        }
    }
}

object ChessEngine {
    fun getValidMoves(state: ChessGameState, from: Position): List<Position> {
        val piece = state.board[from.row][from.col] ?: return emptyList()
        if (piece.color != state.currentTurn) return emptyList()

        val candidateMoves = generatePseudoLegalMoves(state.board, from, piece)
        return candidateMoves.filter { to ->
            // Ensure move doesn't leave own King in check
            val nextBoard = copyBoard(state.board)
            nextBoard[to.row][to.col] = piece
            nextBoard[from.row][from.col] = null
            !isKingAttacked(nextBoard, piece.color)
        }
    }

    fun makeMove(state: ChessGameState, move: ChessMove): ChessGameState {
        val piece = state.board[move.from.row][move.from.col] ?: return state
        val target = state.board[move.to.row][move.to.col]
        val newBoard = copyBoard(state.board)

        // Pawn promotion to Queen
        val movedPiece = if (piece.type == PieceType.PAWN && (move.to.row == 0 || move.to.row == 7)) {
            ChessPiece(PieceType.QUEEN, piece.color)
        } else {
            piece
        }

        newBoard[move.to.row][move.to.col] = movedPiece
        newBoard[move.from.row][move.from.col] = null

        val nextTurn = if (state.currentTurn == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
        val capturedW = if (target?.color == PieceColor.WHITE) state.capturedWhite + target else state.capturedWhite
        val capturedB = if (target?.color == PieceColor.BLACK) state.capturedBlack + target else state.capturedBlack

        val inCheck = isKingAttacked(newBoard, nextTurn)
        val hasLegalMoves = hasAnyLegalMoves(newBoard, nextTurn)
        val isCheckmate = inCheck && !hasLegalMoves

        return ChessGameState(
            board = newBoard,
            currentTurn = nextTurn,
            isCheck = inCheck,
            isCheckmate = isCheckmate,
            capturedWhite = capturedW,
            capturedBlack = capturedB,
            moveHistory = state.moveHistory + move.copy(captured = target)
        )
    }

    private fun generatePseudoLegalMoves(board: Array<Array<ChessPiece?>>, from: Position, piece: ChessPiece): List<Position> {
        val moves = mutableListOf<Position>()
        val dir = if (piece.color == PieceColor.WHITE) -1 else 1
        val r = from.row
        val c = from.col

        when (piece.type) {
            PieceType.PAWN -> {
                // 1 forward
                val f1 = Position(r + dir, c)
                if (f1.isValid() && board[f1.row][f1.col] == null) {
                    moves.add(f1)
                    // 2 forward from starting rank
                    val startRow = if (piece.color == PieceColor.WHITE) 6 else 1
                    val f2 = Position(r + 2 * dir, c)
                    if (r == startRow && board[f2.row][f2.col] == null) {
                        moves.add(f2)
                    }
                }
                // Diagonal captures
                for (dc in listOf(-1, 1)) {
                    val cap = Position(r + dir, c + dc)
                    if (cap.isValid()) {
                        val target = board[cap.row][cap.col]
                        if (target != null && target.color != piece.color) {
                            moves.add(cap)
                        }
                    }
                }
            }
            PieceType.KNIGHT -> {
                val jumps = listOf(
                    Pair(-2, -1), Pair(-2, 1), Pair(-1, -2), Pair(-1, 2),
                    Pair(1, -2), Pair(1, 2), Pair(2, -1), Pair(2, 1)
                )
                for ((dr, dc) in jumps) {
                    val p = Position(r + dr, c + dc)
                    if (p.isValid()) {
                        val t = board[p.row][p.col]
                        if (t == null || t.color != piece.color) moves.add(p)
                    }
                }
            }
            PieceType.BISHOP -> {
                collectRays(board, piece, r, c, listOf(Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1)), moves)
            }
            PieceType.ROOK -> {
                collectRays(board, piece, r, c, listOf(Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1)), moves)
            }
            PieceType.QUEEN -> {
                collectRays(board, piece, r, c, listOf(
                    Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1),
                    Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1)
                ), moves)
            }
            PieceType.KING -> {
                for (dr in -1..1) {
                    for (dc in -1..1) {
                        if (dr == 0 && dc == 0) continue
                        val p = Position(r + dr, c + dc)
                        if (p.isValid()) {
                            val t = board[p.row][p.col]
                            if (t == null || t.color != piece.color) moves.add(p)
                        }
                    }
                }
            }
        }
        return moves
    }

    private fun collectRays(
        board: Array<Array<ChessPiece?>>,
        piece: ChessPiece,
        r: Int,
        c: Int,
        directions: List<Pair<Int, Int>>,
        out: MutableList<Position>
    ) {
        for ((dr, dc) in directions) {
            var currR = r + dr
            var currC = c + dc
            while (currR in 0..7 && currC in 0..7) {
                val target = board[currR][currC]
                if (target == null) {
                    out.add(Position(currR, currC))
                } else {
                    if (target.color != piece.color) {
                        out.add(Position(currR, currC))
                    }
                    break
                }
                currR += dr
                currC += dc
            }
        }
    }

    fun isKingAttacked(board: Array<Array<ChessPiece?>>, color: PieceColor): Boolean {
        var kingPos: Position? = null
        for (r in 0..7) {
            for (c in 0..7) {
                val p = board[r][c]
                if (p?.type == PieceType.KING && p.color == color) {
                    kingPos = Position(r, c)
                    break
                }
            }
            if (kingPos != null) break
        }
        val targetPos = kingPos ?: return false
        val enemyColor = if (color == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE

        for (r in 0..7) {
            for (c in 0..7) {
                val p = board[r][c] ?: continue
                if (p.color == enemyColor) {
                    val pseudo = generatePseudoLegalMoves(board, Position(r, c), p)
                    if (pseudo.contains(targetPos)) return true
                }
            }
        }
        return false
    }

    fun hasAnyLegalMoves(board: Array<Array<ChessPiece?>>, color: PieceColor): Boolean {
        for (r in 0..7) {
            for (c in 0..7) {
                val p = board[r][c] ?: continue
                if (p.color == color) {
                    val valid = getValidMoves(ChessGameState(board = board, currentTurn = color), Position(r, c))
                    if (valid.isNotEmpty()) return true
                }
            }
        }
        return false
    }

    fun getAllLegalMoves(state: ChessGameState): List<ChessMove> {
        val list = mutableListOf<ChessMove>()
        for (r in 0..7) {
            for (c in 0..7) {
                val p = state.board[r][c] ?: continue
                if (p.color == state.currentTurn) {
                    val from = Position(r, c)
                    val destinations = getValidMoves(state, from)
                    for (to in destinations) {
                        list.add(ChessMove(from, to, state.board[to.row][to.col]))
                    }
                }
            }
        }
        return list
    }

    fun copyBoard(source: Array<Array<ChessPiece?>>): Array<Array<ChessPiece?>> {
        return Array(8) { r ->
            Array(8) { c -> source[r][c] }
        }
    }

    // Vidyut Bot AI: Native C++ 3-ply Alpha-Beta Search (with Kotlin fallback for JVM unit tests)
    fun computeBotMove(state: ChessGameState, difficulty: String): ChessMove? {
        val flatBoard = IntArray(64)
        for (r in 0..7) {
            for (c in 0..7) {
                val p = state.board[r][c] ?: continue
                val code = when (p.type) {
                    PieceType.PAWN -> 1
                    PieceType.KNIGHT -> 2
                    PieceType.BISHOP -> 3
                    PieceType.ROOK -> 4
                    PieceType.QUEEN -> 5
                    PieceType.KING -> 6
                }
                flatBoard[r * 8 + c] = if (p.color == PieceColor.WHITE) code else -code
            }
        }
        val turnSide = if (state.currentTurn == PieceColor.WHITE) 1 else -1
        val diffCode = if (difficulty == "EASY") 0 else 1
        val encoded = com.example.utils.NativeCoreBridge.computeChessBotMove(flatBoard, turnSide, diffCode)
        if (encoded != null) {
            val fr = (encoded ushr 24) and 0xFF
            val fc = (encoded ushr 16) and 0xFF
            val tr = (encoded ushr 8) and 0xFF
            val tc = encoded and 0xFF
            if (fr in 0..7 && fc in 0..7 && tr in 0..7 && tc in 0..7) {
                return ChessMove(Position(fr, fc), Position(tr, tc), state.board[tr][tc])
            }
        }

        val legalMoves = getAllLegalMoves(state)
        if (legalMoves.isEmpty()) return null

        if (difficulty == "EASY") {
            // Prioritize captures, otherwise random
            val captureMoves = legalMoves.filter { it.captured != null }
            return if (captureMoves.isNotEmpty() && Random.nextFloat() > 0.4f) {
                captureMoves.random()
            } else {
                legalMoves.random()
            }
        }

        // MASTER: 2-ply Minimax evaluation
        var bestScore = Int.MIN_VALUE
        var bestMove: ChessMove = legalMoves.random()

        for (move in legalMoves) {
            val nextState = makeMove(state, move)
            val score = -evaluateBoard(nextState.board, state.currentTurn)
            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
        }
        return bestMove
    }

    private fun evaluateBoard(board: Array<Array<ChessPiece?>>, forColor: PieceColor): Int {
        var score = 0
        for (r in 0..7) {
            for (c in 0..7) {
                val p = board[r][c] ?: continue
                val valBase = p.type.value
                val positionalBonus = when (p.type) {
                    PieceType.PAWN -> if (p.color == PieceColor.WHITE) (7 - r) * 10 else r * 10
                    PieceType.KNIGHT, PieceType.BISHOP -> if (r in 2..5 && c in 2..5) 20 else 0
                    else -> 0
                }
                val totalVal = valBase + positionalBonus
                if (p.color == forColor) score += totalVal
                else score -= totalVal
            }
        }
        return score
    }
}
