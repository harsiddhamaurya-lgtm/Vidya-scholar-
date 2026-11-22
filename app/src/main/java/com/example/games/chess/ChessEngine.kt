package com.example.games.chess

import com.example.utils.NativeCoreBridge

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
    private fun encodeBoard(board: Array<Array<ChessPiece?>>): IntArray {
        val flat = IntArray(64)
        for (r in 0..7) {
            for (c in 0..7) {
                val p = board[r][c] ?: continue
                val code = when (p.type) {
                    PieceType.PAWN -> 1
                    PieceType.KNIGHT -> 2
                    PieceType.BISHOP -> 3
                    PieceType.ROOK -> 4
                    PieceType.QUEEN -> 5
                    PieceType.KING -> 6
                }
                flat[r * 8 + c] = if (p.color == PieceColor.WHITE) code else -code
            }
        }
        return flat
    }

    private fun sideCode(color: PieceColor): Int = if (color == PieceColor.WHITE) 1 else -1

    fun getValidMoves(state: ChessGameState, from: Position): List<Position> {
        if (!from.isValid()) return emptyList()
        val piece = state.board[from.row][from.col] ?: return emptyList()
        if (piece.color != state.currentTurn) return emptyList()
        val packed = NativeCoreBridge.getChessValidMoves(
            boardArray = encodeBoard(state.board),
            turnSide = sideCode(state.currentTurn),
            fromRow = from.row,
            fromCol = from.col
        )
        return packed.map { Position(it / 8, it % 8) }
    }

    fun makeMove(state: ChessGameState, move: ChessMove): ChessGameState {
        val piece = state.board[move.from.row][move.from.col] ?: return state
        val target = state.board[move.to.row][move.to.col]
        val newBoard = copyBoard(state.board)

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

        val flatNext = encodeBoard(newBoard)
        val nextSide = sideCode(nextTurn)
        val inCheck = NativeCoreBridge.isChessKingAttacked(flatNext, nextSide)
        val hasLegalMoves = NativeCoreBridge.hasAnyChessLegalMoves(flatNext, nextSide)
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

    fun isKingAttacked(board: Array<Array<ChessPiece?>>, color: PieceColor): Boolean =
        NativeCoreBridge.isChessKingAttacked(encodeBoard(board), sideCode(color))

    fun hasAnyLegalMoves(board: Array<Array<ChessPiece?>>, color: PieceColor): Boolean =
        NativeCoreBridge.hasAnyChessLegalMoves(encodeBoard(board), sideCode(color))

    fun getAllLegalMoves(state: ChessGameState): List<ChessMove> {
        val list = mutableListOf<ChessMove>()
        for (r in 0..7) {
            for (c in 0..7) {
                val p = state.board[r][c] ?: continue
                if (p.color == state.currentTurn) {
                    val from = Position(r, c)
                    for (to in getValidMoves(state, from)) {
                        list.add(ChessMove(from, to, state.board[to.row][to.col]))
                    }
                }
            }
        }
        return list
    }

    fun copyBoard(source: Array<Array<ChessPiece?>>): Array<Array<ChessPiece?>> =
        Array(8) { r -> Array(8) { c -> source[r][c] } }

    fun computeBotMove(state: ChessGameState, difficulty: String): ChessMove? {
        val diffCode = if (difficulty == "EASY") 0 else 1
        val encoded = NativeCoreBridge.computeChessBotMove(
            boardArray = encodeBoard(state.board),
            turnSide = sideCode(state.currentTurn),
            difficulty = diffCode
        ) ?: return null

        val fr = (encoded ushr 24) and 0xFF
        val fc = (encoded ushr 16) and 0xFF
        val tr = (encoded ushr 8) and 0xFF
        val tc = encoded and 0xFF
        if (fr !in 0..7 || fc !in 0..7 || tr !in 0..7 || tc !in 0..7) return null
        return ChessMove(Position(fr, fc), Position(tr, tc), state.board[tr][tc])
    }
}
