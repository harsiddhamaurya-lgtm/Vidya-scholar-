package com.example.games.config

enum class PlayMode(val title: String, val subtitle: String) {
    BOT("Solo vs Bot", "Practice offline against Vidyut AI"),
    PASS_AND_PLAY("Pass & Play", "Play with friends on this device"),
    ONLINE_RANKED("Online Ranked", "Climb global ELO tiers with points at stake"),
    TOURNAMENT("Championship", "Bracket knockout: Quarters, Semis, & Finals")
}

data class PlayerSetup(
    val id: Int,
    val roleTitle: String,
    var name: String,
    var isBot: Boolean,
    val colorHex: Long,
    var rating: Int = 1200
)

data class GameSessionConfig(
    val gameTypeName: String,
    val mode: PlayMode = PlayMode.BOT,
    val playerCount: Int = 2,
    val players: List<PlayerSetup> = emptyList(),
    val difficulty: String = "TACTICIAN", // NOVICE, TACTICIAN, GRANDMASTER
    val timeControlMinutes: Int = 10, // 0 = unlimited, 5 = blitz, 10 = rapid
    val tournamentRound: String = "Quarter-Finals",
    val onlineMatchId: String = "",
    val localParticipantUid: String = "",
    val localPlayerIndex: Int = 0
)
