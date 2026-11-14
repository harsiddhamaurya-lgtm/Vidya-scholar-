package com.example.games.rulebook

import com.example.viewmodel.GameType

data class PieceRule(
    val name: String,
    val symbol: String,
    val points: Int,
    val movement: String,
    val specialNotes: String
)

data class RuleSection(
    val title: String,
    val iconName: String,
    val pointers: List<String>
)

data class GameRulebook(
    val gameType: GameType,
    val title: String,
    val subtitle: String,
    val historicalOrigin: String,
    val coreObjective: String,
    val setupOverview: List<String>,
    val pieces: List<PieceRule>,
    val turnAndDiceRules: List<String>,
    val drawRules: List<String>,
    val loopholesAndExploits: List<String>,
    val proStrategies: List<String>
)
