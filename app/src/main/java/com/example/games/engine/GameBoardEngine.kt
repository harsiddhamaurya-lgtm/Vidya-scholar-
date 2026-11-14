package com.example.games.engine

/**
 * Abstract GameBoardEngine defining unified move validation, turn management,
 * and board state transitions for all playable strategy games (Shatranj, Pachisi, Chaturanga).
 */
interface GameBoardEngine<State, Move, Pos> {
    fun initialBoardState(): State
    fun getValidMoves(state: State, pos: Pos): List<Pos>
    fun makeMove(state: State, move: Move): State
    fun isGameOver(state: State): Boolean
    fun computeBotMove(state: State, difficulty: String): Move?
}
