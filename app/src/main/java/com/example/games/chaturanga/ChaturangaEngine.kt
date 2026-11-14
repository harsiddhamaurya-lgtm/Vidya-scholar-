package com.example.games.chaturanga

import kotlin.math.abs
import kotlin.random.Random

enum class ChaturangaPlayer(val title: String, val dirR: Int, val dirC: Int) {
    RED("Red", -1, 0),       // Starts at bottom (row 7), advances up
    GREEN("Green", 0, 1),     // Starts at left (col 0), advances right
    YELLOW("Yellow", 1, 0),   // Starts at top (row 0), advances down
    BLACK("Black", 0, -1)     // Starts at right (col 7), advances left
}

enum class ChaturangaPieceType(val title: String, val symbol: String, val points: Int) {
    RAJA("Raja (King)", "♚", 3),
    GAJA("Gaja (Elephant)", "♜", 5),
    ASHVA("Ashva (Horse)", "♞", 3),
    NAU("Nau (Boat)", "♝", 5),
    PADATI("Padati (Pawn)", "♟", 1)
}

data class ChaturangaPiece(
    val type: ChaturangaPieceType,
    val player: ChaturangaPlayer,
    val isDead: Boolean = false // If player's Raja is captured, pieces freeze as "dead"
)

data class BoardPos(val row: Int, val col: Int) {
    fun isValid(): Boolean = row in 0..7 && col in 0..7
}

data class ChaturangaMove(
    val from: BoardPos,
    val to: BoardPos,
    val captured: ChaturangaPiece? = null
)

data class ChaturangaState(
    val board: Array<Array<ChaturangaPiece?>>,
    val currentTurn: ChaturangaPlayer = ChaturangaPlayer.RED,
    val lastDiceValue: Int = 1,
    val allowedPieceType: List<ChaturangaPieceType> = listOf(ChaturangaPieceType.PADATI, ChaturangaPieceType.RAJA),
    val hasRolled: Boolean = false,
    val playerScores: Map<ChaturangaPlayer, Int> = ChaturangaPlayer.entries.associateWith { 0 },
    val eliminatedPlayers: Set<ChaturangaPlayer> = emptySet(),
    val isGameOver: Boolean = false,
    val logMessage: String = "Roll the dice to determine which piece can move!",
    val lastMove: ChaturangaMove? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ChaturangaState
        return board.contentDeepEquals(other.board) &&
                currentTurn == other.currentTurn &&
                lastDiceValue == other.lastDiceValue &&
                hasRolled == other.hasRolled &&
                playerScores == other.playerScores &&
                eliminatedPlayers == other.eliminatedPlayers
    }

    override fun hashCode(): Int {
        var result = board.contentDeepHashCode()
        result = 31 * result + currentTurn.hashCode()
        result = 31 * result + lastDiceValue.hashCode()
        result = 31 * result + hasRolled.hashCode()
        result = 31 * result + playerScores.hashCode()
        result = 31 * result + eliminatedPlayers.hashCode()
        return result
    }

    fun serialize(): String {
        val sb = StringBuilder()
        for (r in 0..7) {
            for (c in 0..7) {
                val p = board[r][c]
                if (p == null) {
                    sb.append("..")
                } else {
                    val code = when (p.type) {
                        ChaturangaPieceType.RAJA -> 'K'
                        ChaturangaPieceType.GAJA -> 'E'
                        ChaturangaPieceType.ASHVA -> 'H'
                        ChaturangaPieceType.NAU -> 'B'
                        ChaturangaPieceType.PADATI -> 'P'
                    }
                    val colCode = when (p.player) {
                        ChaturangaPlayer.RED -> 'R'
                        ChaturangaPlayer.GREEN -> 'G'
                        ChaturangaPlayer.YELLOW -> 'Y'
                        ChaturangaPlayer.BLACK -> 'B'
                    }
                    sb.append("$code$colCode")
                }
            }
        }
        val scoresStr = playerScores.entries.joinToString(",") { "${it.key.name}:${it.value}" }
        val elimStr = eliminatedPlayers.joinToString(",") { it.name }
        return "$sb|${currentTurn.name}|$lastDiceValue|$hasRolled|$scoresStr|$elimStr|$isGameOver"
    }

    companion object {
        fun deserialize(data: String): ChaturangaState? {
            try {
                val parts = data.split("|")
                if (parts.size < 7 || parts[0].length != 128) return null
                val b = Array(8) { Array<ChaturangaPiece?>(8) { null } }
                var idx = 0
                for (r in 0..7) {
                    for (c in 0..7) {
                        val pieceCode = parts[0][idx]
                        val playerCode = parts[0][idx + 1]
                        idx += 2
                        if (pieceCode != '.') {
                            val type = when (pieceCode) {
                                'K' -> ChaturangaPieceType.RAJA
                                'E' -> ChaturangaPieceType.GAJA
                                'H' -> ChaturangaPieceType.ASHVA
                                'B' -> ChaturangaPieceType.NAU
                                'P' -> ChaturangaPieceType.PADATI
                                else -> ChaturangaPieceType.PADATI
                            }
                            val player = when (playerCode) {
                                'R' -> ChaturangaPlayer.RED
                                'G' -> ChaturangaPlayer.GREEN
                                'Y' -> ChaturangaPlayer.YELLOW
                                'B' -> ChaturangaPlayer.BLACK
                                else -> ChaturangaPlayer.RED
                            }
                            b[r][c] = ChaturangaPiece(type, player)
                        }
                    }
                }
                val turn = ChaturangaPlayer.valueOf(parts[1])
                val dice = parts[2].toInt()
                val rolled = parts[3].toBooleanStrict()
                val scores = parts[4].split(",").filter { it.isNotBlank() }.associate {
                    val s = it.split(":")
                    ChaturangaPlayer.valueOf(s[0]) to s[1].toInt()
                }
                val elim = parts[5].split(",").filter { it.isNotBlank() }.map { ChaturangaPlayer.valueOf(it) }.toSet()
                val over = parts[6].toBooleanStrict()
                return ChaturangaState(
                    board = b,
                    currentTurn = turn,
                    lastDiceValue = dice,
                    allowedPieceType = ChaturangaEngine.diceToPieceTypes(dice),
                    hasRolled = rolled,
                    playerScores = scores,
                    eliminatedPlayers = elim,
                    isGameOver = over
                )
            } catch (e: Exception) {
                return null
            }
        }

        fun initialBoard(): ChaturangaState {
            val b = Array(8) { Array<ChaturangaPiece?>(8) { null } }

            // RED (Starts at Bottom Right, row 7, cols 4..7; Pawns at row 6, cols 4..7)
            b[7][4] = ChaturangaPiece(ChaturangaPieceType.RAJA, ChaturangaPlayer.RED)
            b[7][5] = ChaturangaPiece(ChaturangaPieceType.GAJA, ChaturangaPlayer.RED)
            b[7][6] = ChaturangaPiece(ChaturangaPieceType.ASHVA, ChaturangaPlayer.RED)
            b[7][7] = ChaturangaPiece(ChaturangaPieceType.NAU, ChaturangaPlayer.RED)
            for (c in 4..7) b[6][c] = ChaturangaPiece(ChaturangaPieceType.PADATI, ChaturangaPlayer.RED)

            // GREEN (Starts at Bottom Left, rows 4..7, col 0; Pawns at col 1, rows 4..7)
            b[4][0] = ChaturangaPiece(ChaturangaPieceType.NAU, ChaturangaPlayer.GREEN)
            b[5][0] = ChaturangaPiece(ChaturangaPieceType.ASHVA, ChaturangaPlayer.GREEN)
            b[6][0] = ChaturangaPiece(ChaturangaPieceType.GAJA, ChaturangaPlayer.GREEN)
            b[7][0] = ChaturangaPiece(ChaturangaPieceType.RAJA, ChaturangaPlayer.GREEN)
            for (r in 4..7) b[r][1] = ChaturangaPiece(ChaturangaPieceType.PADATI, ChaturangaPlayer.GREEN)

            // YELLOW (Starts at Top Left, row 0, cols 0..3; Pawns at row 1, cols 0..3)
            b[0][0] = ChaturangaPiece(ChaturangaPieceType.NAU, ChaturangaPlayer.YELLOW)
            b[0][1] = ChaturangaPiece(ChaturangaPieceType.ASHVA, ChaturangaPlayer.YELLOW)
            b[0][2] = ChaturangaPiece(ChaturangaPieceType.GAJA, ChaturangaPlayer.YELLOW)
            b[0][3] = ChaturangaPiece(ChaturangaPieceType.RAJA, ChaturangaPlayer.YELLOW)
            for (c in 0..3) b[1][c] = ChaturangaPiece(ChaturangaPieceType.PADATI, ChaturangaPlayer.YELLOW)

            // BLACK (Starts at Top Right, rows 0..3, col 7; Pawns at col 6, rows 0..3)
            b[0][7] = ChaturangaPiece(ChaturangaPieceType.RAJA, ChaturangaPlayer.BLACK)
            b[1][7] = ChaturangaPiece(ChaturangaPieceType.GAJA, ChaturangaPlayer.BLACK)
            b[2][7] = ChaturangaPiece(ChaturangaPieceType.ASHVA, ChaturangaPlayer.BLACK)
            b[3][7] = ChaturangaPiece(ChaturangaPieceType.NAU, ChaturangaPlayer.BLACK)
            for (r in 0..3) b[r][6] = ChaturangaPiece(ChaturangaPieceType.PADATI, ChaturangaPlayer.BLACK)

            return ChaturangaState(board = b)
        }
    }
}

object ChaturangaEngine {
    // Dice mapping in classical al-Biruni Chaturaji:
    // 1 or 5 -> Padati (Pawn) or Raja (King)
    // 2 -> Nau (Boat / Ship)
    // 3 -> Ashva (Horse / Knight)
    // 4 or 6 -> Gaja (Elephant)
    fun diceToPieceTypes(dice: Int): List<ChaturangaPieceType> {
        return when (dice) {
            1, 5 -> listOf(ChaturangaPieceType.PADATI, ChaturangaPieceType.RAJA)
            2 -> listOf(ChaturangaPieceType.NAU)
            3 -> listOf(ChaturangaPieceType.ASHVA)
            4, 6 -> listOf(ChaturangaPieceType.GAJA)
            else -> listOf(ChaturangaPieceType.PADATI, ChaturangaPieceType.RAJA)
        }
    }

    fun rollDice(): Int = Random.nextInt(1, 7)

    fun getMovablePieces(state: ChaturangaState): List<BoardPos> {
        if (!state.hasRolled || state.isGameOver) return emptyList()
        val player = state.currentTurn
        val allowedTypes = state.allowedPieceType

        val positions = mutableListOf<BoardPos>()
        for (r in 0..7) {
            for (c in 0..7) {
                val p = state.board[r][c] ?: continue
                if (p.player == player && !p.isDead && allowedTypes.contains(p.type)) {
                    val moves = getValidMoves(state, BoardPos(r, c))
                    if (moves.isNotEmpty()) positions.add(BoardPos(r, c))
                }
            }
        }
        return positions
    }

    fun getValidMoves(state: ChaturangaState, from: BoardPos): List<BoardPos> {
        val piece = state.board[from.row][from.col] ?: return emptyList()
        if (piece.isDead || piece.player != state.currentTurn) return emptyList()
        val moves = mutableListOf<BoardPos>()
        val r = from.row
        val c = from.col
        val board = state.board

        when (piece.type) {
            ChaturangaPieceType.RAJA -> {
                // Moves 1 square any direction
                for (dr in -1..1) {
                    for (dc in -1..1) {
                        if (dr == 0 && dc == 0) continue
                        val p = BoardPos(r + dr, c + dc)
                        if (p.isValid()) {
                            val target = board[p.row][p.col]
                            if (target == null || target.player != piece.player) moves.add(p)
                        }
                    }
                }
            }
            ChaturangaPieceType.GAJA -> {
                // Elephant: Orthogonal sliding moves (like Rook in Chaturaji)
                val directions = listOf(Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1))
                for ((dr, dc) in directions) {
                    var currR = r + dr
                    var currC = c + dc
                    while (currR in 0..7 && currC in 0..7) {
                        val t = board[currR][currC]
                        if (t == null) {
                            moves.add(BoardPos(currR, currC))
                        } else {
                            if (t.player != piece.player) moves.add(BoardPos(currR, currC))
                            break
                        }
                        currR += dr
                        currC += dc
                    }
                }
            }
            ChaturangaPieceType.ASHVA -> {
                // Horse: Standard L-shaped knight leap
                val jumps = listOf(
                    Pair(-2, -1), Pair(-2, 1), Pair(-1, -2), Pair(-1, 2),
                    Pair(1, -2), Pair(1, 2), Pair(2, -1), Pair(2, 1)
                )
                for ((dr, dc) in jumps) {
                    val p = BoardPos(r + dr, c + dc)
                    if (p.isValid()) {
                        val t = board[p.row][p.col]
                        if (t == null || t.player != piece.player) moves.add(p)
                    }
                }
            }
            ChaturangaPieceType.NAU -> {
                // Boat: Jumps exactly 2 squares diagonally, leaping over pieces
                val boatJumps = listOf(Pair(-2, -2), Pair(-2, 2), Pair(2, -2), Pair(2, 2))
                for ((dr, dc) in boatJumps) {
                    val p = BoardPos(r + dr, c + dc)
                    if (p.isValid()) {
                        val t = board[p.row][p.col]
                        if (t == null || t.player != piece.player) moves.add(p)
                    }
                }
            }
            ChaturangaPieceType.PADATI -> {
                // Pawn moves 1 square forward in player's advancing direction
                val fwdR = r + piece.player.dirR
                val fwdC = c + piece.player.dirC
                val fwd = BoardPos(fwdR, fwdC)
                if (fwd.isValid() && board[fwd.row][fwd.col] == null) {
                    moves.add(fwd)
                }

                // Diagonal captures forward
                val diagPairs = if (piece.player.dirR != 0) {
                    listOf(Pair(piece.player.dirR, -1), Pair(piece.player.dirR, 1))
                } else {
                    listOf(Pair(-1, piece.player.dirC), Pair(1, piece.player.dirC))
                }
                for ((dr, dc) in diagPairs) {
                    val cap = BoardPos(r + dr, c + dc)
                    if (cap.isValid()) {
                        val t = board[cap.row][cap.col]
                        if (t != null && t.player != piece.player) moves.add(cap)
                    }
                }
            }
        }
        return moves
    }

    fun makeMove(state: ChaturangaState, move: ChaturangaMove): ChaturangaState {
        val piece = state.board[move.from.row][move.from.col] ?: return state
        val target = state.board[move.to.row][move.to.col]
        val newBoard = Array(8) { r -> Array(8) { c -> state.board[r][c] } }

        var pointsEarned = 0
        var newEliminated = state.eliminatedPlayers
        var log = "${piece.player.title} moved ${piece.type.title} to (${move.to.row}, ${move.to.col})."

        if (target != null) {
            pointsEarned += target.type.points
            log = "${piece.player.title} captured ${target.player.title}'s ${target.type.title} (+${target.type.points} pts)!"

            // If Raja captured, eliminate that player and freeze their remaining pieces!
            if (target.type == ChaturangaPieceType.RAJA) {
                newEliminated = newEliminated + target.player
                log = "${piece.player.title} captured ${target.player.title}'s RAJA! ${target.player.title} eliminated!"
                for (r in 0..7) {
                    for (c in 0..7) {
                        val p = newBoard[r][c]
                        if (p != null && p.player == target.player) {
                            newBoard[r][c] = p.copy(isDead = true)
                        }
                    }
                }
            }
        }

        // Move piece
        newBoard[move.to.row][move.to.col] = piece
        newBoard[move.from.row][move.from.col] = null

        val updatedScores = state.playerScores.toMutableMap()
        updatedScores[piece.player] = (updatedScores[piece.player] ?: 0) + pointsEarned

        val nextPlayer = getNextActivePlayer(piece.player, newEliminated)
        val gameOver = newEliminated.size >= 3

        return state.copy(
            board = newBoard,
            currentTurn = nextPlayer,
            hasRolled = false,
            playerScores = updatedScores,
            eliminatedPlayers = newEliminated,
            isGameOver = gameOver,
            logMessage = log,
            lastMove = move.copy(captured = target)
        )
    }

    fun getNextActivePlayer(current: ChaturangaPlayer, eliminated: Set<ChaturangaPlayer>): ChaturangaPlayer {
        val players = ChaturangaPlayer.entries
        var next = (players.indexOf(current) + 1) % players.size
        for (i in 0 until 4) {
            val candidate = players[next]
            if (!eliminated.contains(candidate)) return candidate
            next = (next + 1) % players.size
        }
        return current
    }

    // Bot AI move selection
    fun chooseBotMove(state: ChaturangaState): ChaturangaMove? {
        val movablePositions = getMovablePieces(state)
        if (movablePositions.isEmpty()) return null

        val candidateMoves = mutableListOf<ChaturangaMove>()
        for (from in movablePositions) {
            val valid = getValidMoves(state, from)
            for (to in valid) {
                candidateMoves.add(ChaturangaMove(from, to, state.board[to.row][to.col]))
            }
        }
        if (candidateMoves.isEmpty()) return null

        // 1. Can we capture a King?
        val kingCaptures = candidateMoves.filter { it.captured?.type == ChaturangaPieceType.RAJA }
        if (kingCaptures.isNotEmpty()) return kingCaptures.first()

        // 2. Can we capture any valuable piece?
        val valuableCaptures = candidateMoves.filter { it.captured != null }.sortedByDescending { it.captured?.type?.points ?: 0 }
        if (valuableCaptures.isNotEmpty()) return valuableCaptures.first()

        // 3. Otherwise pick a random valid move
        return candidateMoves.random()
    }
}
