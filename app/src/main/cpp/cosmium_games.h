#ifndef COSMIUM_GAMES_H
#define COSMIUM_GAMES_H

#include <vector>

namespace cosmium::games {

// Chess (Shatranj) piece encoding on a 64-int row-major 8x8 board:
// 0 = empty, +1..+6 = White (P, N, B, R, Q, K), -1..-6 = Black (P, N, B, R, Q, K)
bool isChessKingAttacked(const int* board, int side);
bool hasAnyChessLegalMoves(int* board, int side);
std::vector<int> getChessValidMoves(int* board, int turnSide, int fromRow, int fromCol);
int computeChessBotMove(int* board, int turnSide, int difficulty);

// Ludo (Pachisi) position encoding:
// -1 = yard, 0..51 = main track, 52..56 = home stretch, 57 = finished. Returns -1 when invalid.
int getLudoDestinationPosition(int position, int startTrackIndex, int dice);
int chooseLudoBestToken(
    const std::vector<int>& positions,
    const std::vector<int>& startTracks,
    const std::vector<int>& enemyPositions,
    int dice,
    const std::vector<int>& safeZones
);

// Chaturanga (Chaturaji) move scoring:
int chooseChaturangaBestMove(
    const std::vector<int>& capturedPoints,
    const std::vector<bool>& capturedIsRaja,
    const std::vector<int>& toRows,
    const std::vector<int>& toCols
);

} // namespace cosmium::games

#endif // COSMIUM_GAMES_H
