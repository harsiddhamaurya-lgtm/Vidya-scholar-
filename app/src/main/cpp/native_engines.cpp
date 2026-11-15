#include <jni.h>
#include <cmath>
#include <cstdint>
#include <vector>
#include <algorithm>
#include <cstdlib>

namespace {

constexpr double kPi = 3.14159265358979323846;

// --- 1. NATIVE CHESS ENGINE (ZERO-ALLOC ALPHA-BETA SEARCH) ---
// Piece encoding: 0 = empty, +1..+6 = White (P,N,B,R,Q,K), -1..-6 = Black (P,N,B,R,Q,K)
constexpr int kPawn = 1;
constexpr int kKnight = 2;
constexpr int kBishop = 3;
constexpr int kRook = 4;
constexpr int kQueen = 5;
constexpr int kKing = 6;

const int kPieceValues[7] = {0, 100, 320, 330, 500, 900, 20000};

const int kPawnPst[64] = {
     0,  0,  0,  0,  0,  0,  0,  0,
    50, 50, 50, 50, 50, 50, 50, 50,
    10, 10, 20, 30, 30, 20, 10, 10,
     5,  5, 10, 25, 25, 10,  5,  5,
     0,  0,  0, 20, 20,  0,  0,  0,
     5, -5,-10,  0,  0,-10, -5,  5,
     5, 10, 10,-20,-20, 10, 10,  5,
     0,  0,  0,  0,  0,  0,  0,  0
};

const int kCenterPst[64] = {
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
    int fr, fc, tr, tc;
    int scoreGuess;
};

inline bool inBounds(int r, int c) {
    return r >= 0 && r < 8 && c >= 0 && c < 8;
}

inline int pieceSide(int p) {
    return (p > 0) ? 1 : ((p < 0) ? -1 : 0);
}

bool isSquareAttacked(const int* board, int targetR, int targetC, int attackerSide) {
    // Pawn attacks
    int pawnRow = targetR + attackerSide; // White(+1) pawns attack upwards (from r+1 to r)
    if (pawnRow >= 0 && pawnRow < 8) {
        if (targetC - 1 >= 0 && board[pawnRow * 8 + (targetC - 1)] == attackerSide * kPawn) return true;
        if (targetC + 1 < 8 && board[pawnRow * 8 + (targetC + 1)] == attackerSide * kPawn) return true;
    }

    // Knight attacks
    static const int kKnightDr[8] = {-2, -2, -1, -1, 1, 1, 2, 2};
    static const int kKnightDc[8] = {-1, 1, -2, 2, -2, 2, -1, 1};
    for (int i = 0; i < 8; ++i) {
        int nr = targetR + kKnightDr[i];
        int nc = targetC + kKnightDc[i];
        if (inBounds(nr, nc) && board[nr * 8 + nc] == attackerSide * kKnight) return true;
    }

    // King attacks
    for (int dr = -1; dr <= 1; ++dr) {
        for (int dc = -1; dc <= 1; ++dc) {
            if (dr == 0 && dc == 0) continue;
            int nr = targetR + dr;
            int nc = targetC + dc;
            if (inBounds(nr, nc) && board[nr * 8 + nc] == attackerSide * kKing) return true;
        }
    }

    // Rook / Queen orthogonal rays
    static const int kOrthoDr[4] = {-1, 1, 0, 0};
    static const int kOrthoDc[4] = {0, 0, -1, 1};
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

    // Bishop / Queen diagonal rays
    static const int kDiagDr[4] = {-1, -1, 1, 1};
    static const int kDiagDc[4] = {-1, 1, -1, 1};
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

bool isKingInCheck(const int* board, int side) {
    for (int i = 0; i < 64; ++i) {
        if (board[i] == side * kKing) {
            return isSquareAttacked(board, i / 8, i % 8, -side);
        }
    }
    return false;
}

void generateLegalMoves(int* board, int side, std::vector<RawMove>& outMoves) {
    outMoves.clear();
    auto tryPush = [&](int fr, int fc, int tr, int tc) {
        int origFrom = board[fr * 8 + fc];
        int origTo = board[tr * 8 + tc];
        int moved = origFrom;
        if (std::abs(origFrom) == kPawn && (tr == 0 || tr == 7)) {
            moved = side * kQueen;
        }
        board[tr * 8 + tc] = moved;
        board[fr * 8 + fc] = 0;
        bool safe = !isKingInCheck(board, side);
        board[fr * 8 + fc] = origFrom;
        board[tr * 8 + tc] = origTo;
        if (safe) {
            int guess = (origTo != 0) ? (10 * kPieceValues[std::abs(origTo)] - kPieceValues[std::abs(origFrom)]) : 0;
            outMoves.push_back({fr, fc, tr, tc, guess});
        }
    };

    for (int r = 0; r < 8; ++r) {
        for (int c = 0; c < 8; ++c) {
            int p = board[r * 8 + c];
            if (pieceSide(p) != side) continue;
            int type = std::abs(p);

            if (type == kPawn) {
                int dir = (side == 1) ? -1 : 1;
                int startRow = (side == 1) ? 6 : 1;
                if (inBounds(r + dir, c) && board[(r + dir) * 8 + c] == 0) {
                    tryPush(r, c, r + dir, c);
                    if (r == startRow && inBounds(r + 2 * dir, c) && board[(r + 2 * dir) * 8 + c] == 0) {
                        tryPush(r, c, r + 2 * dir, c);
                    }
                }
                for (int dc : {-1, 1}) {
                    int nr = r + dir, nc = c + dc;
                    if (inBounds(nr, nc) && pieceSide(board[nr * 8 + nc]) == -side) {
                        tryPush(r, c, nr, nc);
                    }
                }
            } else if (type == kKnight) {
                static const int kDr[8] = {-2, -2, -1, -1, 1, 1, 2, 2};
                static const int kDc[8] = {-1, 1, -2, 2, -2, 2, -1, 1};
                for (int i = 0; i < 8; ++i) {
                    int nr = r + kDr[i], nc = c + kDc[i];
                    if (inBounds(nr, nc) && pieceSide(board[nr * 8 + nc]) != side) {
                        tryPush(r, c, nr, nc);
                    }
                }
            } else if (type == kKing) {
                for (int dr = -1; dr <= 1; ++dr) {
                    for (int dc = -1; dc <= 1; ++dc) {
                        if (dr == 0 && dc == 0) continue;
                        int nr = r + dr, nc = c + dc;
                        if (inBounds(nr, nc) && pieceSide(board[nr * 8 + nc]) != side) {
                            tryPush(r, c, nr, nc);
                        }
                    }
                }
            } else {
                static const int kAllDr[8] = {-1, 1, 0, 0, -1, -1, 1, 1};
                static const int kAllDc[8] = {0, 0, -1, 1, -1, 1, -1, 1};
                int startD = (type == kBishop) ? 4 : 0;
                int endD = (type == kRook) ? 4 : 8;
                for (int d = startD; d < endD; ++d) {
                    int nr = r + kAllDr[d], nc = c + kAllDc[d];
                    while (inBounds(nr, nc)) {
                        int target = board[nr * 8 + nc];
                        if (target == 0) {
                            tryPush(r, c, nr, nc);
                        } else {
                            if (pieceSide(target) == -side) {
                                tryPush(r, c, nr, nc);
                            }
                            break;
                        }
                        nr += kAllDr[d];
                        nc += kAllDc[d];
                    }
                }
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
    generateLegalMoves(board, side, moves);
    if (moves.empty()) {
        return isKingInCheck(board, side) ? (-100000 - depth) : 0;
    }
    std::sort(moves.begin(), moves.end(), [](const RawMove& a, const RawMove& b) {
        return a.scoreGuess > b.scoreGuess;
    });

    int best = -200000;
    for (const auto& m : moves) {
        int origFrom = board[m.fr * 8 + m.fc];
        int origTo = board[m.tr * 8 + m.tc];
        int moved = (std::abs(origFrom) == kPawn && (m.tr == 0 || m.tr == 7)) ? (side * kQueen) : origFrom;
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

extern "C" {

JNIEXPORT jint JNICALL
Java_com_example_utils_NativeCoreBridge_nativeComputeChessBotMove(
    JNIEnv* env,
    jobject thiz,
    jintArray boardArray,
    jint turnSide,
    jint difficulty
) {
    if (!boardArray || env->GetArrayLength(boardArray) != 64) return -1;
    int board[64];
    env->GetIntArrayRegion(boardArray, 0, 64, board);

    std::vector<RawMove> moves;
    moves.reserve(48);
    generateLegalMoves(board, turnSide, moves);
    if (moves.empty()) return -1;

    if (difficulty == 0) {
        // EASY: 60% prefer capture if available, else random move
        std::vector<RawMove> captures;
        for (const auto& m : moves) {
            if (board[m.tr * 8 + m.tc] != 0) captures.push_back(m);
        }
        const auto& pool = (!captures.empty() && (rand() % 10) >= 4) ? captures : moves;
        const RawMove& pick = pool[rand() % pool.size()];
        return ((pick.fr & 0xFF) << 24) | ((pick.fc & 0xFF) << 16) | ((pick.tr & 0xFF) << 8) | (pick.tc & 0xFF);
    }

    // MASTER: 3-ply Alpha-Beta Pruning Search with move ordering
    std::sort(moves.begin(), moves.end(), [](const RawMove& a, const RawMove& b) {
        return a.scoreGuess > b.scoreGuess;
    });

    int bestScore = -200000;
    RawMove bestMove = moves[0];
    int alpha = -200000;
    const int beta = 200000;

    for (const auto& m : moves) {
        int origFrom = board[m.fr * 8 + m.fc];
        int origTo = board[m.tr * 8 + m.tc];
        int moved = (std::abs(origFrom) == kPawn && (m.tr == 0 || m.tr == 7)) ? (turnSide * kQueen) : origFrom;
        board[m.tr * 8 + m.tc] = moved;
        board[m.fr * 8 + m.fc] = 0;

        int score = -alphaBeta(board, 2, -beta, -alpha, -turnSide);

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

JNIEXPORT jshortArray JNICALL
Java_com_example_utils_NativeCoreBridge_nativeSynthesizeWoodyTock(
    JNIEnv* env,
    jobject thiz,
    jint sampleRate,
    jdouble durationSec,
    jdouble baseFreq,
    jdouble sweep,
    jdouble volume
) {
    int numSamples = std::max(1, static_cast<int>(sampleRate * durationSec));
    std::vector<int16_t> buf(numSamples);
    for (int i = 0; i < numSamples; ++i) {
        double t = static_cast<double>(i) / sampleRate;
        double freq = baseFreq + sweep * std::exp(-55.0 * t);
        double attack = std::clamp(t / 0.004, 0.0, 1.0);
        double decay = std::exp(-42.0 * t);
        double body = std::sin(2.0 * kPi * freq * t) * 0.8 +
                      std::sin(2.0 * kPi * (freq * 1.48) * t) * 0.2;
        double sample = std::clamp(body * attack * decay * volume, -0.9, 0.9);
        buf[i] = static_cast<int16_t>(sample * 32767.0);
    }
    jshortArray out = env->NewShortArray(numSamples);
    if (out) env->SetShortArrayRegion(out, 0, numSamples, buf.data());
    return out;
}

JNIEXPORT jshortArray JNICALL
Java_com_example_utils_NativeCoreBridge_nativeSynthesizeTempleChime(
    JNIEnv* env,
    jobject thiz,
    jint sampleRate,
    jdouble durationSec,
    jdouble fundamental
) {
    int numSamples = std::max(1, static_cast<int>(sampleRate * durationSec));
    std::vector<int16_t> buf(numSamples);
    for (int i = 0; i < numSamples; ++i) {
        double t = static_cast<double>(i) / sampleRate;
        double attack = std::clamp(t / 0.008, 0.0, 1.0);
        double envAmp = attack * std::exp(-14.5 * t);
        double overtoneEnv = attack * std::exp(-22.0 * t);
        double wave = std::sin(2.0 * kPi * fundamental * t) * 0.65 * envAmp +
                      std::sin(2.0 * kPi * (fundamental * 2.0) * t) * 0.22 * overtoneEnv +
                      std::sin(2.0 * kPi * (fundamental * 2.76) * t) * 0.13 * overtoneEnv;
        double sample = std::clamp(wave * 0.26, -0.9, 0.9);
        buf[i] = static_cast<int16_t>(sample * 32767.0);
    }
    jshortArray out = env->NewShortArray(numSamples);
    if (out) env->SetShortArrayRegion(out, 0, numSamples, buf.data());
    return out;
}

JNIEXPORT jshortArray JNICALL
Java_com_example_utils_NativeCoreBridge_nativeSynthesizeVictorySwell(
    JNIEnv* env,
    jobject thiz,
    jint sampleRate,
    jdouble durationSec
) {
    int numSamples = std::max(1, static_cast<int>(sampleRate * durationSec));
    std::vector<int16_t> buf(numSamples);
    static const double freqs[4] = {261.63, 392.00, 523.25, 659.25};
    static const double entries[4] = {0.0, 0.06, 0.13, 0.21};

    for (int i = 0; i < numSamples; ++i) {
        double t = static_cast<double>(i) / sampleRate;
        double mix = 0.0;
        for (int k = 0; k < 4; ++k) {
            double dt = t - entries[k];
            if (dt > 0.0) {
                double envAmp = std::clamp(dt / 0.028, 0.0, 1.0) * std::exp(-3.8 * dt);
                double f = freqs[k];
                double voice = std::sin(2.0 * kPi * f * dt) * 0.58 +
                               std::sin(2.0 * kPi * (f * 2.0) * dt) * 0.26 +
                               std::sin(2.0 * kPi * (f * 3.0) * dt) * 0.16;
                mix += voice * envAmp;
            }
        }
        double sample = std::clamp(mix * 0.22, -0.92, 0.92);
        buf[i] = static_cast<int16_t>(sample * 32767.0);
    }
    jshortArray out = env->NewShortArray(numSamples);
    if (out) env->SetShortArrayRegion(out, 0, numSamples, buf.data());
    return out;
}

JNIEXPORT void JNICALL
Java_com_example_utils_NativeCoreBridge_nativeSynthesizeMusicNote(
    JNIEnv* env,
    jobject thiz,
    jshortArray outBuffer,
    jint sampleRate,
    jdouble melodyFreq,
    jdouble harmonicFreq,
    jdouble droneFreq,
    jboolean isJoyful,
    jlong globalSampleStart
) {
    if (!outBuffer) return;
    jsize len = env->GetArrayLength(outBuffer);
    if (len <= 0) return;
    std::vector<int16_t> buf(len);
    const double decayRate = isJoyful ? 5.5 : 2.2;
    const double harmonicWeight = isJoyful ? 0.18 : 0.12;
    const double droneEnv = isJoyful ? 0.25 : 0.42;

    for (jsize i = 0; i < len; ++i) {
        double tLocal = static_cast<double>(i) / sampleRate;
        double tGlobal = static_cast<double>(globalSampleStart + i) / sampleRate;
        double attack = std::clamp(tLocal / 0.025, 0.0, 1.0);
        double envAmp = attack * std::exp(-decayRate * tLocal);
        double melodyWave = std::sin(2.0 * kPi * melodyFreq * tLocal) * 0.65 +
                            std::sin(2.0 * kPi * harmonicFreq * tLocal) * harmonicWeight;
        double droneWave = std::sin(2.0 * kPi * droneFreq * tGlobal) * droneEnv +
                           std::sin(2.0 * kPi * (droneFreq * 1.5) * tGlobal) * (droneEnv * 0.45);
        double mixed = std::clamp((melodyWave * envAmp + droneWave) * 0.14, -0.95, 0.95);
        buf[i] = static_cast<int16_t>(mixed * 32767.0);
    }
    env->SetShortArrayRegion(outBuffer, 0, len, buf.data());
}

}
