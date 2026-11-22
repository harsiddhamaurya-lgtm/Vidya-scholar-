#include "cosmium_games.h"

#include <algorithm>
#include <cstdlib>

namespace cosmium::games {
namespace {

constexpr int kPawn = 1;
constexpr int kKnight = 2;
constexpr int kBishop = 3;
constexpr int kRook = 4;
constexpr int kQueen = 5;
constexpr int kKing = 6;

constexpr int kPieceValues[7] = {0, 100, 320, 330, 500, 900, 20000};

constexpr int kPawnPst[64] = {
     0,  0,  0,  0,  0,  0,  0,  0,
    50, 50, 50, 50, 50, 50, 50, 50,
    10, 10, 20, 30, 30, 20, 10, 10,
     5,  5, 10, 25, 25, 10,  5,  5,
     0,  0,  0, 20, 20,  0,  0,  0,
     5, -5,-10,  0,  0,-10, -5,  5,
     5, 10, 10,-20,-20, 10, 10,  5,
     0,  0,  0,  0,  0,  0,  0,  0
};

constexpr int kCenterPst[64] = {
   -20,-10,-10,-10,-10,-10,-10,-20,
   -10,  0,  0,  0,  0,  0,  0,-10,
   -10,  0, 10, 15, 15, 10,  0,-10,
   -10,  5, 15, 20, 20, 15,  5,-10,
   -10,  0, 15, 20, 20, 15,  0,-10,
   -10,  5, 10, 15, 15, 10,  5,-10,
   -10,  0,  5,  0,  0,  5,  0,-10,
   -20,-10,-10,-10,-10,-10,-10,-20
};

struct RawMove {
    int fr;
    int fc;
    int tr;
    int tc;
    int scoreGuess;
};

inline bool inBounds(int r, int c) {
    return r >= 0 && r < 8 && c >= 0 && c < 8;
}

inline int pieceSide(int p) {
    return (p > 0) ? 1 : ((p < 0) ? -1 : 0);
}

bool isSquareAttacked(const int* board, int targetR, int targetC, int attackerSide) {
    int pawnRow = targetR + attackerSide;
    if (pawnRow >= 0 && pawnRow < 8) {
        if (targetC - 1 >= 0 && board[pawnRow * 8 + (targetC - 1)] == attackerSide * kPawn) return true;
        if (targetC + 1 < 8 && board[pawnRow * 8 + (targetC + 1)] == attackerSide * kPawn) return true;
    }

    static constexpr int kKnightDr[8] = {-2, -2, -1, -1, 1, 1, 2, 2};
    static constexpr int kKnightDc[8] = {-1, 1, -2, 2, -2, 2, -1, 1};
    for (int i = 0; i < 8; ++i) {
        int nr = targetR + kKnightDr[i];
        int nc = targetC + kKnightDc[i];
        if (inBounds(nr, nc) && board[nr * 8 + nc] == attackerSide * kKnight) return true;
    }

    for (int dr = -1; dr <= 1; ++dr) {
        for (int dc = -1; dc <= 1; ++dc) {
            if (dr == 0 && dc == 0) continue;
            int nr = targetR + dr;
            int nc = targetC + dc;
            if (inBounds(nr, nc) && board[nr * 8 + nc] == attackerSide * kKing) return true;
        }
    }

    static constexpr int kOrthoDr[4] = {-1, 1, 0, 0};
    static constexpr int kOrthoDc[4] = {0, 0, -1, 1};
    for (int d = 0; d < 4; ++d) {
        int r = targetR + kOrthoDr[d];
        int c = targetC + kOrthoDc[d];
        while (inBounds(r, c)) {
            int p = board[r * 8 + c];
            if (p != 0) {
                if (pieceSide(p) == attackerSide && (std::abs(p) == kRook || std::abs(p) == kQueen)) {
                    return true;
                }
                break;
            }
            r += kOrthoDr[d];
            c += kOrthoDc[d];
        }
    }

    static constexpr int kDiagDr[4] = {-1, -1, 1, 1};
    static constexpr int kDiagDc[4] = {-1, 1, -1, 1};
    for (int d = 0; d < 4; ++d) {
        int r = targetR + kDiagDr[d];
        int c = targetC + kDiagDc[d];
        while (inBounds(r, c)) {
            int p = board[r * 8 + c];
            if (p != 0) {
                if (pieceSide(p) == attackerSide && (std::abs(p) == kBishop || std::abs(p) == kQueen)) {
                    return true;
                }
                break;
            }
            r += kDiagDr[d];
            c += kDiagDc[d];
        }
    }

    return false;
}

void generateLegalMovesForSquare(
    int* board,
    int r,
    int c,
    int side,
    std::vector<RawMove>& outMoves
) {
    int p = board[r * 8 + c];
    if (pieceSide(p) != side) return;
    int type = std::abs(p);

    auto tryPush = [&](int tr, int tc) {
        int origFrom = board[r * 8 + c];
        int origTo = board[tr * 8 + tc];
        int moved = (type == kPawn && (tr == 0 || tr == 7)) ? (side * kQueen) : origFrom;
        board[tr * 8 + tc] = moved;
        board[r * 8 + c] = 0;
        bool safe = !isChessKingAttacked(board, side);
        board[r * 8 + c] = origFrom;
        board[tr * 8 + tc] = origTo;
        if (safe) {
            int guess = (origTo != 0)
                ? (10 * kPieceValues[std::abs(origTo)] - kPieceValues[type])
                : 0;
            outMoves.push_back({r, c, tr, tc, guess});
        }
    };

    if (type == kPawn) {
        int dir = (side == 1) ? -1 : 1;
        int startRow = (side == 1) ? 6 : 1;
        if (inBounds(r + dir, c) && board[(r + dir) * 8 + c] == 0) {
            tryPush(r + dir, c);
            if (r == startRow && inBounds(r + 2 * dir, c) && board[(r + 2 * dir) * 8 + c] == 0) {
                tryPush(r + 2 * dir, c);
            }
        }
        for (int dc : {-1, 1}) {
            int nr = r + dir;
            int nc = c + dc;
            if (inBounds(nr, nc) && pieceSide(board[nr * 8 + nc]) == -side) {
                tryPush(nr, nc);
            }
        }
    } else if (type == kKnight) {
        static constexpr int kDr[8] = {-2, -2, -1, -1, 1, 1, 2, 2};
        static constexpr int kDc[8] = {-1, 1, -2, 2, -2, 2, -1, 1};
        for (int i = 0; i < 8; ++i) {
            int nr = r + kDr[i];
            int nc = c + kDc[i];
            if (inBounds(nr, nc) && pieceSide(board[nr * 8 + nc]) != side) {
                tryPush(nr, nc);
            }
        }
    } else if (type == kKing) {
        for (int dr = -1; dr <= 1; ++dr) {
            for (int dc = -1; dc <= 1; ++dc) {
                if (dr == 0 && dc == 0) continue;
                int nr = r + dr;
                int nc = c + dc;
                if (inBounds(nr, nc) && pieceSide(board[nr * 8 + nc]) != side) {
                    tryPush(nr, nc);
                }
            }
        }
    } else {
        static constexpr int kAllDr[8] = {-1, 1, 0, 0, -1, -1, 1, 1};
        static constexpr int kAllDc[8] = {0, 0, -1, 1, -1, 1, -1, 1};
        int startD = (type == kBishop) ? 4 : 0;
        int endD = (type == kRook) ? 4 : 8;
        for (int d = startD; d < endD; ++d) {
            int nr = r + kAllDr[d];
            int nc = c + kAllDc[d];
            while (inBounds(nr, nc)) {
                int target = board[nr * 8 + nc];
                if (target == 0) {
                    tryPush(nr, nc);
                } else {
                    if (pieceSide(target) == -side) {
                        tryPush(nr, nc);
                    }
                    break;
                }
                nr += kAllDr[d];
                nc += kAllDc[d];
            }
        }
    }
}

void generateAllLegalMoves(int* board, int side, std::vector<RawMove>& outMoves) {
    outMoves.clear();
    for (int r = 0; r < 8; ++r) {
        for (int c = 0; c < 8; ++c) {
            if (pieceSide(board[r * 8 + c]) == side) {
                generateLegalMovesForSquare(board, r, c, side, outMoves);
            }
        }
    }
}

int evaluateBoard(const int* board, int side) {
    int score = 0;
    for (int i = 0; i < 64; ++i) {
        int p = board[i];
        if (p == 0) continue;
        int type = std::abs(p);
        int pSide = pieceSide(p);
        int r = i / 8;
        int idx = (pSide == 1) ? i : ((7 - r) * 8 + (i % 8));
        int pst = 0;
        if (type == kPawn) pst = kPawnPst[idx];
        else if (type == kKnight || type == kBishop) pst = kCenterPst[idx];
        int val = kPieceValues[type] + pst;
        score += (pSide == side) ? val : -val;
    }
    return score;
}

int alphaBeta(int* board, int depth, int alpha, int beta, int side) {
    if (depth == 0) {
        return evaluateBoard(board, side);
    }
    std::vector<RawMove> moves;
    moves.reserve(40);
    generateAllLegalMoves(board, side, moves);
    if (moves.empty()) {
        return isChessKingAttacked(board, side) ? (-100000 - depth) : 0;
    }
    std::sort(moves.begin(), moves.end(), [](const RawMove& a, const RawMove& b) {
        return a.scoreGuess > b.scoreGuess;
    });

    int best = -200000;
    for (const auto& m : moves) {
        int origFrom = board[m.fr * 8 + m.fc];
        int origTo = board[m.tr * 8 + m.tc];
        int moved = (std::abs(origFrom) == kPawn && (m.tr == 0 || m.tr == 7))
            ? (side * kQueen)
            : origFrom;
        board[m.tr * 8 + m.tc] = moved;
        board[m.fr * 8 + m.fc] = 0;

        int val = -alphaBeta(board, depth - 1, -beta, -alpha, -side);

        board[m.fr * 8 + m.fc] = origFrom;
        board[m.tr * 8 + m.tc] = origTo;

        if (val > best) best = val;
        if (val > alpha) alpha = val;
        if (alpha >= beta) break;
    }
    return best;
}

} // namespace

bool isChessKingAttacked(const int* board, int side) {
    if (!board) return false;
    for (int i = 0; i < 64; ++i) {
        if (board[i] == side * kKing) {
            return isSquareAttacked(board, i / 8, i % 8, -side);
        }
    }
    return false;
}

bool hasAnyChessLegalMoves(int* board, int side) {
    if (!board) return false;
    std::vector<RawMove> moves;
    moves.reserve(28);
    for (int r = 0; r < 8; ++r) {
        for (int c = 0; c < 8; ++c) {
            if (pieceSide(board[r * 8 + c]) == side) {
                moves.clear();
                generateLegalMovesForSquare(board, r, c, side, moves);
                if (!moves.empty()) return true;
            }
        }
    }
    return false;
}

std::vector<int> getChessValidMoves(int* board, int turnSide, int fromRow, int fromCol) {
    std::vector<int> packedSquares;
    if (!board || !inBounds(fromRow, fromCol)) return packedSquares;
    std::vector<RawMove> moves;
    moves.reserve(28);
    generateLegalMovesForSquare(board, fromRow, fromCol, turnSide, moves);
    packedSquares.reserve(moves.size());
    for (const auto& m : moves) {
        packedSquares.push_back(m.tr * 8 + m.tc);
    }
    return packedSquares;
}

int computeChessBotMove(int* board, int turnSide, int difficulty) {
    if (!board) return -1;
    std::vector<RawMove> moves;
    moves.reserve(48);
    generateAllLegalMoves(board, turnSide, moves);
    if (moves.empty()) return -1;

    if (difficulty == 0) {
        std::vector<RawMove> captures;
        for (const auto& m : moves) {
            if (board[m.tr * 8 + m.tc] != 0) captures.push_back(m);
        }
        const auto& pool = (!captures.empty() && (std::rand() % 10) >= 4) ? captures : moves;
        const RawMove& pick = pool[std::rand() % pool.size()];
        return ((pick.fr & 0xFF) << 24) | ((pick.fc & 0xFF) << 16) | ((pick.tr & 0xFF) << 8) | (pick.tc & 0xFF);
    }

    std::sort(moves.begin(), moves.end(), [](const RawMove& a, const RawMove& b) {
        return a.scoreGuess > b.scoreGuess;
    });

    int bestScore = -200000;
    RawMove bestMove = moves[0];
    int alpha = -200000;
    constexpr int kBeta = 200000;

    for (const auto& m : moves) {
        int origFrom = board[m.fr * 8 + m.fc];
        int origTo = board[m.tr * 8 + m.tc];
        int moved = (std::abs(origFrom) == kPawn && (m.tr == 0 || m.tr == 7))
            ? (turnSide * kQueen)
            : origFrom;
        board[m.tr * 8 + m.tc] = moved;
        board[m.fr * 8 + m.fc] = 0;

        int score = -alphaBeta(board, 2, -kBeta, -alpha, -turnSide);

        board[m.fr * 8 + m.fc] = origFrom;
        board[m.tr * 8 + m.tc] = origTo;

        if (score > bestScore) {
            bestScore = score;
            bestMove = m;
        }
        if (score > alpha) alpha = score;
    }

    return ((bestMove.fr & 0xFF) << 24) | ((bestMove.fc & 0xFF) << 16) | ((bestMove.tr & 0xFF) << 8) | (bestMove.tc & 0xFF);
}

int getLudoDestinationPosition(int position, int startTrackIndex, int dice) {
    if (position == 57) return -1;
    if (position == -1) return (dice == 6) ? startTrackIndex : -1;

    int currentRelStep = (position >= 52 && position <= 56)
        ? (position - 1)
        : ((position - startTrackIndex + 52) % 52);

    int nextRelStep = currentRelStep + dice;
    if (nextRelStep > 56) return -1;
    if (nextRelStep == 56) return 57;
    if (nextRelStep >= 51 && nextRelStep <= 55) return nextRelStep + 1;
    return (startTrackIndex + nextRelStep) % 52;
}

int chooseLudoBestToken(
    const std::vector<int>& positions,
    const std::vector<int>& startTracks,
    const std::vector<int>& enemyPositions,
    int dice,
    const std::vector<int>& safeZones
) {
    size_t count = std::min(positions.size(), startTracks.size());
    if (count == 0) return -1;
    if (count == 1) return 0;

    auto isSafe = [&](int p) {
        return std::find(safeZones.begin(), safeZones.end(), p) != safeZones.end();
    };

    int bestIdx = 0;
    int bestScore = -100000;

    for (size_t i = 0; i < count; ++i) {
        int dest = getLudoDestinationPosition(positions[i], startTracks[i], dice);
        if (dest == -1) continue;

        int score = 0;
        if (dest >= 0 && dest <= 51 && !isSafe(dest)) {
            if (std::find(enemyPositions.begin(), enemyPositions.end(), dest) != enemyPositions.end()) {
                score += 10000;
            }
        }
        if (dest == 57) score += 5000;
        if (dice == 6 && positions[i] == -1) score += 2500;
        if ((dest >= 52 && dest <= 56) || isSafe(dest)) score += 1200;

        int progress = (positions[i] >= 52 && positions[i] <= 56)
            ? positions[i]
            : ((positions[i] == -1) ? 0 : ((positions[i] - startTracks[i] + 52) % 52));
        score += progress;

        if (score > bestScore) {
            bestScore = score;
            bestIdx = static_cast<int>(i);
        }
    }

    return bestIdx;
}

int chooseChaturangaBestMove(
    const std::vector<int>& capturedPoints,
    const std::vector<bool>& capturedIsRaja,
    const std::vector<int>& toRows,
    const std::vector<int>& toCols
) {
    size_t count = std::min(
        std::min(capturedPoints.size(), capturedIsRaja.size()),
        std::min(toRows.size(), toCols.size())
    );
    if (count == 0) return -1;
    if (count == 1) return 0;

    int bestScore = -100000;
    std::vector<int> bestIndices;

    for (size_t i = 0; i < count; ++i) {
        int score = 0;
        if (capturedIsRaja[i]) {
            score += 50000;
        } else if (capturedPoints[i] > 0) {
            score += capturedPoints[i] * 100;
        }
        int distCenter = std::abs(toRows[i] * 2 - 7) + std::abs(toCols[i] * 2 - 7);
        score += (14 - distCenter);

        if (score > bestScore) {
            bestScore = score;
            bestIndices.clear();
            bestIndices.push_back(static_cast<int>(i));
        } else if (score == bestScore) {
            bestIndices.push_back(static_cast<int>(i));
        }
    }

    if (bestIndices.empty()) return 0;
    return bestIndices[std::rand() % bestIndices.size()];
}

} // namespace cosmium::games
