package com.example.games.ludo

import kotlin.random.Random

enum class LudoColor(val title: String, val startTrackIndex: Int) {
    RED("Red", 0),
    GREEN("Green", 13),
    YELLOW("Yellow", 26),
    BLUE("Blue", 39)
}

data class LudoToken(
    val id: Int, // 0..3
    val color: LudoColor,
    val position: Int = -1 // -1 = In Yard, 0..51 = Main track, 52..56 = Home column, 57 = Home (Finished)
) {
    val isHome: Boolean get() = position == 57
    val isInYard: Boolean get() = position == -1
    val isOnTrack: Boolean get() = position in 0..51
    val isInHomeStretch: Boolean get() = position in 52..56
}

data class LudoGameState(
    val tokens: List<LudoToken>,
    val currentTurn: LudoColor = LudoColor.RED,
    val lastDiceValue: Int = 1,
    val hasRolled: Boolean = false,
    val winners: List<LudoColor> = emptyList(),
    val isGameOver: Boolean = false,
    val logMessage: String = "Roll the dice to begin!"
) {
    fun serialize(): String {
        val grouped = tokens.groupBy { it.color }
        val parts = grouped.map { (color, list) ->
            val posStr = list.joinToString(",") { "${it.id}:${it.position}" }
            "${color.name}=$posStr"
        }.joinToString(";")
        return "$parts|${currentTurn.name}|$lastDiceValue|$hasRolled"
    }

    companion object {
        fun initialGame(activeColors: List<LudoColor> = LudoColor.entries): LudoGameState {
            val list = mutableListOf<LudoToken>()
            for (color in activeColors) {
                for (id in 0..3) {
                    list.add(LudoToken(id, color, -1))
                }
            }
            return LudoGameState(tokens = list, currentTurn = activeColors.firstOrNull() ?: LudoColor.RED)
        }

        fun deserialize(data: String): LudoGameState? {
            try {
                val sections = data.split("|")
                if (sections.size < 4) return null
                val colorSections = sections[0].split(";")
                val tokens = mutableListOf<LudoToken>()
                for (sec in colorSections) {
                    val kv = sec.split("=")
                    if (kv.size != 2) continue
                    val color = LudoColor.valueOf(kv[0])
                    val tokenEntries = kv[1].split(",")
                    for (entry in tokenEntries) {
                        val ip = entry.split(":")
                        if (ip.size == 2) {
                            tokens.add(LudoToken(ip[0].toInt(), color, ip[1].toInt()))
                        }
                    }
                }
                val turn = LudoColor.valueOf(sections[1])
                val dice = sections[2].toIntOrNull() ?: 1
                val rolled = sections[3].toBooleanStrictOrNull() ?: false
                return LudoGameState(tokens = tokens, currentTurn = turn, lastDiceValue = dice, hasRolled = rolled)
            } catch (e: Exception) {
                return null
            }
        }
    }
}

object LudoEngine {
    // 8 Safe spots on the main 52-step board (4 start squares + 4 star squares)
    val SAFE_ZONES = setOf(0, 8, 13, 21, 26, 34, 39, 47)

    fun rollDice(): Int = Random.nextInt(1, 7)

    /**
     * Computes exact destination board position (-1, 0..51 track, 52..56 home column, 57 Home)
     * using relative distance around each color's 56-step complete Pachisi circuit.
     */
    fun getDestinationPosition(token: LudoToken, dice: Int): Int? {
        if (token.isHome) return null
        if (token.isInYard) {
            return if (dice == 6) token.color.startTrackIndex else null
        }

        val currentRelStep = if (token.position in 52..56) {
            token.position - 1 // 51..55
        } else {
            (token.position - token.color.startTrackIndex + 52) % 52 // 0..50
        }

        val nextRelStep = currentRelStep + dice
        return when {
            nextRelStep > 56 -> null // Must roll exact number to enter center Home
            nextRelStep == 56 -> 57  // Center Home Sanctuary!
            nextRelStep in 51..55 -> nextRelStep + 1 // 52..56 Home Column
            else -> (token.color.startTrackIndex + nextRelStep) % 52
        }
    }

    fun canMoveToken(token: LudoToken, dice: Int): Boolean {
        return getDestinationPosition(token, dice) != null
    }

    fun getMovableTokens(state: LudoGameState): List<LudoToken> {
        if (!state.hasRolled) return emptyList()
        val playerTokens = state.tokens.filter { it.color == state.currentTurn }
        return playerTokens.filter { canMoveToken(it, state.lastDiceValue) }
    }

    fun moveToken(state: LudoGameState, token: LudoToken): LudoGameState {
        val dice = state.lastDiceValue
        val newPos = getDestinationPosition(token, dice) ?: token.position

        var capturedMessage: String? = null
        var didCapture = false
        val updatedTokens = state.tokens.map { t ->
            if (t.id == token.id && t.color == token.color) {
                t.copy(position = newPos)
            } else if (newPos in 0..51 && !SAFE_ZONES.contains(newPos) && t.color != token.color && t.position == newPos) {
                // Knockout opponent token back to Yard!
                didCapture = true
                capturedMessage = "${token.color.title} captured ${t.color.title}'s token!"
                t.copy(position = -1)
            } else {
                t
            }
        }

        // Check if current player just won all 4 tokens
        val finishedCount = updatedTokens.count { it.color == token.color && it.isHome }
        val newWinners = if (finishedCount == 4 && !state.winners.contains(token.color)) {
            state.winners + token.color
        } else {
            state.winners
        }

        val activeColors = state.tokens.map { it.color }.distinct()
        // 6 or capturing an enemy grants an extra roll (unless finished all 4 tokens)
        val extraTurn = (dice == 6 || didCapture || newPos == 57) && finishedCount < 4
        val nextTurn = if (extraTurn) state.currentTurn else nextPlayer(state.currentTurn, activeColors)

        val log = when {
            finishedCount == 4 -> "${token.color.title} finished all 4 tokens!"
            capturedMessage != null -> "$capturedMessage Bonus Roll!"
            newPos == 57 -> "${token.color.title} reached Home! Bonus Roll!"
            extraTurn -> "${token.color.title} rolled a 6! Roll again!"
            else -> "${nextTurn.title}'s turn."
        }

        return state.copy(
            tokens = updatedTokens,
            currentTurn = nextTurn,
            hasRolled = false,
            winners = newWinners,
            isGameOver = newWinners.size >= (activeColors.size - 1).coerceAtLeast(1),
            logMessage = log
        )
    }

    fun nextPlayer(current: LudoColor, activeColors: List<LudoColor> = LudoColor.entries): LudoColor {
        val pool = activeColors.ifEmpty { LudoColor.entries }
        val currentIdx = pool.indexOf(current)
        if (currentIdx == -1) return pool.first()
        return pool[(currentIdx + 1) % pool.size]
    }

    // Bot decision making
    fun chooseBotToken(movableTokens: List<LudoToken>, allTokens: List<LudoToken>, dice: Int): LudoToken? {
        if (movableTokens.isEmpty()) return null
        if (movableTokens.size == 1) return movableTokens.first()

        // 1. Can we capture an opponent?
        for (token in movableTokens) {
            val dest = getDestinationPosition(token, dice) ?: continue
            if (dest in 0..51 && !SAFE_ZONES.contains(dest)) {
                val enemy = allTokens.find { it.color != token.color && it.position == dest }
                if (enemy != null) return token
            }
        }

        // 2. Can we land a token into Home (57)?
        val homeReachers = movableTokens.filter { getDestinationPosition(it, dice) == 57 }
        if (homeReachers.isNotEmpty()) return homeReachers.first()

        // 3. Move token out of Yard if rolled 6
        if (dice == 6) {
            val yardToken = movableTokens.find { it.isInYard }
            if (yardToken != null) return yardToken
        }

        // 4. Move token into Home Stretch (52..56) or Safe Zone
        for (token in movableTokens) {
            val dest = getDestinationPosition(token, dice) ?: continue
            if (dest in 52..56 || SAFE_ZONES.contains(dest)) return token
        }

        // 5. Advance the token furthest ahead on its circuit
        return movableTokens.maxByOrNull { t ->
            if (t.position in 52..56) t.position
            else (t.position - t.color.startTrackIndex + 52) % 52
        } ?: movableTokens.first()
    }
}
