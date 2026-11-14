package com.example.games.rulebook

import com.example.viewmodel.GameType

object RulebookData {

    fun getRulebook(type: GameType): GameRulebook {
        return when (type) {
            GameType.CHATURANGA -> chaturangaRulebook
            GameType.SHATRANJ -> shatranjRulebook
            GameType.LUDO -> ludoRulebook
        }
    }

    // 1. CHATURANGA (CHATURAJI) - 4-PLAYER DICE CHESS
    private val chaturangaRulebook = GameRulebook(
        gameType = GameType.CHATURANGA,
        title = "Chaturanga (Chaturaji)",
        subtitle = "The Ancient 4-Player Father of Chess on 8x8 Ashtapada",
        historicalOrigin = "Documented by Persian polymath Abu Rayhan al-Biruni in 1030 CE in 'Kitab fi Tahqiq ma li'l-Hind', Chaturanga ('Four Limbs of Army') is the ancestor of all chess variants, originally played with four armies and dice.",
        coreObjective = "Command one of four armies (Red, Green, Yellow, Black) to capture enemy Kings, annihilate opposing forces, control the Ashtapada board, and amass the highest tactical score.",
        setupOverview = listOf(
            "Board: Played on an uncheckered 8×8 Ashtapada grid with traditional 'X' marks on specific celestial sanctuary squares.",
            "Four Armies: Red (South), Green (West), Yellow (North), and Black (East).",
            "Army Composition: Each of the 4 players controls 8 pieces: 1 Raja (King), 1 Gaja (Elephant), 1 Ashva (Horse), 1 Nau (Boat/Chariot), and 4 Padati (Pawns).",
            "Initial Placement: Pieces occupy the rear rank of their respective quadrant; 4 pawns line up directly in front on the second rank.",
            "Turn Rotation: Play rotates clockwise: Red → Green → Yellow → Black."
        ),
        pieces = listOf(
            PieceRule(
                name = "Raja (King)",
                symbol = "♚",
                points = 3,
                movement = "Steps exactly 1 square in any direction (orthogonal or diagonal).",
                specialNotes = "Capturing an enemy Raja captures 3 points AND instantly disables the victim's remaining army."
            ),
            PieceRule(
                name = "Gaja (Elephant)",
                symbol = "♜",
                points = 5,
                movement = "Jumps diagonally over intervening squares to land exactly 2 squares away.",
                specialNotes = "Can jump over any friendly or enemy pieces in its path; visits only 8 distinct squares of its color lattice."
            ),
            PieceRule(
                name = "Ashva (Horse)",
                symbol = "♞",
                points = 3,
                movement = "Standard Knight's leap: 2 squares in one orthogonal direction plus 1 square perpendicular (L-shape).",
                specialNotes = "Leaps over intermediate pieces; highly lethal on crowded Ashtapada boards."
            ),
            PieceRule(
                name = "Nau (Boat / Chariot)",
                symbol = "♝",
                points = 5,
                movement = "Moves 2 squares orthogonally (horizontal or vertical), jumping over intermediate pieces.",
                specialNotes = "In traditional al-Biruni rules, boats can also execute diagonal captures or 2-square diagonal leaps."
            ),
            PieceRule(
                name = "Padati (Pawn)",
                symbol = "♟",
                points = 1,
                movement = "Advances 1 square forward toward the opposite board perimeter. Captures 1 square diagonally forward.",
                specialNotes = "Promotes to the piece type corresponding to the specific column file reached on the opposite rank (Raja excluded)."
            )
        ),
        turnAndDiceRules = listOf(
            "Mandatory Dice Roll: At the start of your turn, roll the 6-sided die to determine which piece category is legally permitted to move:",
            "• Roll 1 or 5: You must move a Padati (Pawn) or your Raja (King).",
            "• Roll 2: You must move your Nau (Boat).",
            "• Roll 3: You must move your Ashva (Horse).",
            "• Roll 4 or 6: You must move your Gaja (Elephant).",
            "Forced Move Rule: If you roll a number corresponding to pieces you still have legal moves for, you MUST move one. If no legal moves exist for that rolled piece, your turn is forfeited."
        ),
        drawRules = listOf(
            "• King Elimination Stalemate: If only two Kings remain on the board with zero remaining attack pieces capable of capturing each other, the match ends in a mutual draw.",
            "• 20-Round Inactivity Rule: If 20 consecutive complete round cycles (80 turns across all 4 armies) pass without a single piece capture or pawn promotion, the game is declared a draw.",
            "• Boat Deadlock (Nau Patt): When two remaining players only possess a Boat each and neither boat can legally reach the other's quadrant lattice, a draw is declared.",
            "• Simultaneous Double King Capture Agreement: If the final remaining players agree that neither can make offensive progress, a mutual draw may be called."
        ),
        loopholesAndExploits = listOf(
            "• Sinhasana (Throne Triumph) Loophole: If your Raja maneuvers onto the exact starting throne square of an enemy Raja, you capture their entire throne and command their territory, yielding a massive 5-point bonus!",
            "• Chaturaji (Four Boats Triumph): If you move your Boat into the fourth corner of a 2×2 square formed by three other boats, you capture all four boats simultaneously in a single strike!",
            "• The 'Dead Army' Meat-Shield Exploit: When an opponent's King is captured, their remaining 7 pieces freeze permanently in place as lifeless obstacles. Astute players intentionally use these dead enemy pieces as defensive shields to block enemy Horse and Elephant attack corridors!",
            "• Dice Probability Manipulation: Pawns/Kings and Elephants have a 2-in-6 (33.3%) roll probability, while Boats and Horses only have a 1-in-6 (16.7%) roll probability. Positioning your Elephants and Pawns aggressively exploits this 2x statistical advantage!",
            "• Corner Quadrant Lockout: Trapping an opponent's King against the outer Ashtapada board edge allows adjacent players to pin and capture their King without risking retaliation."
        ),
        proStrategies = listOf(
            "Prioritize capturing enemy Kings early; eliminating a player converts all their surviving pieces into free capture targets.",
            "Guard your Boat: Boats are worth 5 points and are the most agile jumpers for striking unexpected diagonals.",
            "Coordinate attacks against the leader; in a 4-player game, gang up on whichever color has the highest score."
        )
    )

    // 2. SHATRANJ (2-PLAYER CHESS)
    private val shatranjRulebook = GameRulebook(
        gameType = GameType.SHATRANJ,
        title = "Shatranj • Chess (2-Player)",
        subtitle = "The Timeless Duel of Tactics, Checkmate & Strategic Mastery",
        historicalOrigin = "Transmitted from India's Chaturanga into Persia during the Sasanian Empire (where it was known as Chatrang), and later refined throughout the Islamic Golden Age as Shatranj, this is the direct forefather of modern chess.",
        coreObjective = "Trap and threaten the opponent's King (Shah) such that escape or defense is impossible (Checkmate), or eliminate all defending material to achieve total victory.",
        setupOverview = listOf(
            "Board: Standard 8×8 grid with 64 alternating squares.",
            "Two Armies: White (moves first) vs Black.",
            "16 Pieces per Army: 1 Shah (King), 1 Farzin (Counselor/Queen), 2 Fil/Gaja (Elephants/Bishops), 2 Asb (Horses/Knights), 2 Rokh (Chariots/Rooks), and 8 Baidaq (Pawns).",
            "Initial Placement: Rooks in corners (a1, h1 / a8, h8), Knights inward (b1, g1 / b8, g8), Elephants/Bishops (c1, f1 / c8, f8), Queen on d1 / d8, King on e1 / e8, Pawns across rows 2 and 7."
        ),
        pieces = listOf(
            PieceRule(
                name = "Shah (King)",
                symbol = "♚",
                points = 100,
                movement = "1 square in any direction (orthogonal or diagonal).",
                specialNotes = "Must never step into check; checkmate of the Shah ends the match immediately."
            ),
            PieceRule(
                name = "Farzin / Firzan (Counselor / Queen)",
                symbol = "♛",
                points = 9,
                movement = "Modern/Grandmaster rules: Slides any number of squares horizontally, vertically, or diagonally.",
                specialNotes = "The most versatile and powerful offensive piece on the board."
            ),
            PieceRule(
                name = "Rokh (Chariot / Rook)",
                symbol = "♜",
                points = 5,
                movement = "Slides any number of vacant squares along straight ranks or files.",
                specialNotes = "Dominates open files and endgames."
            ),
            PieceRule(
                name = "Fil (Elephant / Bishop)",
                symbol = "♝",
                points = 3,
                movement = "Slides diagonally across vacant squares of its assigned color.",
                specialNotes = "Each player has one light-square bishop and one dark-square bishop."
            ),
            PieceRule(
                name = "Asb (Horse / Knight)",
                symbol = "♞",
                points = 3,
                movement = "Leaps in an L-pattern (2 squares one way, 1 square perpendicular).",
                specialNotes = "The only piece that can jump over other pieces."
            ),
            PieceRule(
                name = "Baidaq (Pawn)",
                symbol = "♟",
                points = 1,
                movement = "Advances 1 square forward (or 2 squares from starting rank). Captures 1 square diagonally forward.",
                specialNotes = "Promotes to a Queen upon touching the opponent's back rank (rank 8 for White, rank 1 for Black)."
            )
        ),
        turnAndDiceRules = listOf(
            "Deterministic Turn Play: No dice are used in Shatranj. Victory relies purely on calculation, foresight, and tactical depth.",
            "White moves first, alternating each single ply.",
            "Check & Legal Moves: When your Shah is attacked (Check), your next move MUST relieve the threat by moving the King, blocking the check, or capturing the attacker."
        ),
        drawRules = listOf(
            "• Stalemate (Patt): If the player whose turn it is to move has NO legal moves and their King is NOT in check, the game is an immediate DRAW.",
            "• Threefold Repetition: If the exact identical board position occurs 3 times with the same player to move and identical legal moves, a draw occurs.",
            "• 50-Move Rule: If 50 consecutive moves occur for both players with zero pawn moves and zero captures, the game is declared a draw.",
            "• Insufficient Mating Material: The game is a draw when neither side possesses enough pieces to force checkmate (e.g., King vs King, King + Bishop vs King, or King + Knight vs King).",
            "• Mutual Agreement: Both players can agree to a draw at any point during active play.",
            "• Bare King (Historic Shatranj Burj): In classical Shatranj, reducing an opponent to a bare King is a win unless the opponent can capture your last piece on the very next turn, which results in a draw."
        ),
        loopholesAndExploits = listOf(
            "• The Absolute Pin Exploit: Pinning an opponent's high-value piece (Queen, Rook) against their King renders that pinned piece completely frozen, as moving it would expose their King to illegal check.",
            "• Desperado Piece Sacrifice: When an active piece is doomed to capture, look to sacrifice it for maximum enemy material or check checks before it perishes.",
            "• Royal Fork Trap: Using a Knight to simultaneously attack the opponent's King and Queen guarantees winning the Queen on the subsequent ply.",
            "• Stalemate Defense Trick: If you are losing by significant material, sacrifice all remaining pieces to leave your King with zero legal moves—turning certain defeat into a saved draw!"
        ),
        proStrategies = listOf(
            "Control the 4 center squares (d4, e4, d5, e5) early to command board diagonals.",
            "Develop minor pieces (Knights and Bishops) before launching aggressive Queen attacks.",
            "Do not move the same piece multiple times in the opening phase unless threatened."
        )
    )

    // 3. PACHISI (LUDO) - 4-PLAYER RACE
    private val ludoRulebook = GameRulebook(
        gameType = GameType.LUDO,
        title = "Pachisi (Ludo)",
        subtitle = "The Royal Mughal Race of Strategy, Safe Havens & Knockouts",
        historicalOrigin = "Evolving from the 6th-century Indian royal game of Chaupar and depicted in the caves of Ellora, Pachisi was played by Emperor Akbar on life-sized courtyards using palace maidens as living pieces.",
        coreObjective = "Navigate all 4 of your colored tokens out of your Yard, complete a full 52-step clockwise circuit around the track, traverse the Home Stretch, and reach the central Home sanctuary first.",
        setupOverview = listOf(
            "Board: Cross-shaped board with 4 colored home yards (Red, Green, Yellow, Blue), an outer perimeter track with 52 spaces, and 4 colored home columns leading to the center triangle.",
            "Player Count: Supports 2, 3, or 4 players (or teams).",
            "Tokens: 4 tokens per player parked inside their corner Yard at match start.",
            "Starting Track Spaces: Red starts at track 0, Green at track 13, Yellow at track 26, Blue at track 39."
        ),
        pieces = listOf(
            PieceRule(
                name = "Pachisi Token (Goti)",
                symbol = "●",
                points = 10,
                movement = "Advances clockwise around the 52-tile circuit by the exact number shown on the rolled die.",
                specialNotes = "Requires a roll of 6 to be unlocked from the Yard into active play."
            )
        ),
        turnAndDiceRules = listOf(
            "Rolling the Die: Players take turns rolling a standard 6-sided die clockwise.",
            "Unlocking Tokens: You MUST roll a 6 to release a token from the Yard onto your starting colored track space.",
            "Bonus Turn on 6: Rolling a 6 grants an immediate extra roll! If you roll another 6, you get another roll.",
            "Knockout Bonus: Capturing an opponent's token by landing on their exact tile grants an immediate free bonus turn!"
        ),
        drawRules = listOf(
            "• Mutual Circuit Deadlock: In rare circumstances where remaining tokens cannot enter Home due to consecutive blocking without progress for 30 cycles, a draw is agreed.",
            "• Tie-Break by Finished Tokens: If an agreed draw occurs, the player with the most tokens safely inside Home (or furthest along the home column) is ranked first."
        ),
        loopholesAndExploits = listOf(
            "• Safe Sanctuary Haven Exploits: Star squares and starting squares are Safe Zones! Opponents landing on a Safe Zone CANNOT knock you out; both tokens co-exist peacefully.",
            "• The 6-6-6 Triple Penalty: Rolling three consecutive 6s in a row forfeits the third roll and cancels the turn to prevent runaway exploits!",
            "• Stacking Barrier Defense: Stacking 2 of your tokens on the same square creates an impenetrable blockade that opposing tokens cannot leap over until you dismantle it.",
            "• Trailing Ambush Technique: Instead of running ahead of an enemy token, park 6 spaces directly BEHIND them. A roll of 6 gives you maximum probability of knocking them all the way back to their yard!",
            "• Exact Roll Home Trap: Entering the final Home triangle requires the EXACT number. Plan ahead by placing tokens on 56 or 55 to give yourself multiple winning die rolls (1 or 2)."
        ),
        proStrategies = listOf(
            "Never leave all 4 tokens trapped in the Yard; unlock multiple tokens as soon as 6s are rolled.",
            "Prioritize knocking out leading opponent tokens over making modest advances with your own.",
            "Camp your tokens on safe star squares while waiting for high-value targets to pass."
        )
    )
}
