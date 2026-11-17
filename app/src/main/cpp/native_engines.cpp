#include <jni.h>
#include <cmath>
#include <cstdint>
#include <vector>
#include <algorithm>
#include <cstdlib>
#include <string>
#include <cctype>
#include <unordered_set>
#include <unordered_map>

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

} // extern "C"

namespace cosmos_math {

inline void replaceAll(std::string& str, const std::string& from, const std::string& to) {
    if (from.empty()) return;
    size_t start_pos = 0;
    while ((start_pos = str.find(from, start_pos)) != std::string::npos) {
        str.replace(start_pos, from.length(), to);
        start_pos += to.length();
    }
}

inline void cleanHeadingAndStrayTokens(std::string& text) {
    // Strip malformed hash tokens
    replaceAll(text, "###/", "");
    replaceAll(text, "##/", "");
    replaceAll(text, "#/", "");

    // Detach markdown hashes directly attached to math blocks
    replaceAll(text, "###$$", "###\n\n$$");
    replaceAll(text, "##$$", "##\n\n$$");
    replaceAll(text, "#$$", "#\n\n$$");
    replaceAll(text, "$$###", "$$\n\n###");
    replaceAll(text, "$$##", "$$\n\n##");
    replaceAll(text, "$$#", "$$\n\n#");

    // Repair corrupted control-byte escape sequences from JSON or stream splitting
    const std::string f_rac = std::string("\f") + "rac";
    replaceAll(text, f_rac, "\\frac");
    const std::string f_orall = std::string("\f") + "orall";
    replaceAll(text, f_orall, "\\forall");
    const std::string t_ext = std::string("\t") + "ext";
    replaceAll(text, t_ext, "\\text");
    const std::string t_imes = std::string("\t") + "imes";
    replaceAll(text, t_imes, "\\times");
    const std::string t_heta = std::string("\t") + "heta";
    replaceAll(text, t_heta, "\\theta");
    const std::string t_an = std::string("\t") + "an";
    replaceAll(text, t_an, "\\tan");
    const std::string t_au = std::string("\t") + "au";
    replaceAll(text, t_au, "\\tau");
    const std::string t_o = std::string("\t") + "o";
    replaceAll(text, t_o, "\\to");
    const std::string b_egin = std::string("\b") + "egin";
    replaceAll(text, b_egin, "\\begin");
    const std::string b_ar = std::string("\b") + "ar";
    replaceAll(text, b_ar, "\\bar");
    const std::string b_eta = std::string("\b") + "eta";
    replaceAll(text, b_eta, "\\beta");
    const std::string b_inom = std::string("\b") + "inom";
    replaceAll(text, b_inom, "\\binom");
    const std::string r_ight = std::string("\r") + "ight";
    replaceAll(text, r_ight, "\\right");
    const std::string r_ho = std::string("\r") + "ho";
    replaceAll(text, r_ho, "\\rho");
}

inline void applyMacroFallbacks(std::string& text) {
    replaceAll(text, "\\tfrac", "\\frac");
    replaceAll(text, "\\dfrac", "\\frac");
    replaceAll(text, "\\cfrac", "\\frac");
    replaceAll(text, "\\bm{", "\\mathbf{");
    replaceAll(text, "\\mathbbm{", "\\mathbf{");
    replaceAll(text, "\\implies", "\\Rightarrow");
    replaceAll(text, "\\iff", "\\Leftrightarrow");
    replaceAll(text, "\\impliedby", "\\Leftarrow");
    replaceAll(text, "\\celsius", "^\\circ\\mathrm{C}");
    replaceAll(text, "\\degree", "^\\circ");
    replaceAll(text, "\\ohm", "\\Omega");
    replaceAll(text, "\\angstrom", "\\text{\\AA}");
}

inline void balanceMathDelimiters(std::string& text) {
    // 1. Balance double dollar ($$) display delimiters
    size_t displayCount = 0;
    size_t pos = 0;
    while ((pos = text.find("$$", pos)) != std::string::npos) {
        displayCount++;
        pos += 2;
    }
    if (displayCount % 2 != 0) {
        text += "\n$$\n";
    }

    // 2. Balance lone single dollar ($) inline delimiters
    size_t loneDollarCount = 0;
    for (size_t i = 0; i < text.size(); ++i) {
        if (text[i] == '$') {
            if (i + 1 < text.size() && text[i + 1] == '$') {
                i++; // Skip display delimiter
            } else {
                loneDollarCount++;
            }
        }
    }
    if (loneDollarCount % 2 != 0) {
        text += "$";
    }

    // Clean stray trailing backslash if any
    if (!text.empty() && text.back() == '\\') {
        text.pop_back();
    }
}

} // namespace cosmos_math

extern "C" {

JNIEXPORT jstring JNICALL
Java_com_example_utils_NativeCoreBridge_nativeSanitizeAndFormatMath(
    JNIEnv* env,
    jobject thiz,
    jstring rawTextStr,
    jint standardGrade,
    jstring streamIdStr
) {
    if (!rawTextStr) return env->NewStringUTF("");
    const char* rawChars = env->GetStringUTFChars(rawTextStr, nullptr);
    std::string text(rawChars ? rawChars : "");
    env->ReleaseStringUTFChars(rawTextStr, rawChars);

    cosmos_math::cleanHeadingAndStrayTokens(text);
    cosmos_math::applyMacroFallbacks(text);
    cosmos_math::balanceMathDelimiters(text);

    return env->NewStringUTF(text.c_str());
}

JNIEXPORT jstring JNICALL
Java_com_example_utils_NativeCoreBridge_nativeCalibrateCosmosPrompt(
    JNIEnv* env,
    jobject thiz,
    jstring userQueryStr,
    jint standardGrade,
    jstring streamIdStr
) {
    if (!userQueryStr) return env->NewStringUTF("");
    const char* qChars = env->GetStringUTFChars(userQueryStr, nullptr);
    std::string query(qChars ? qChars : "");
    env->ReleaseStringUTFChars(userQueryStr, qChars);

    const char* streamChars = streamIdStr ? env->GetStringUTFChars(streamIdStr, nullptr) : "general";
    std::string stream(streamChars ? streamChars : "general");
    if (streamIdStr) env->ReleaseStringUTFChars(streamIdStr, streamChars);

    std::string prompt;
    prompt += "You are Cosmium—the encouraging AI study sanctuary and doubt solver for Vidhya Scholar.\n";

    if (standardGrade <= 10) {
        prompt += "[Grade Calibration: Foundation Standard " + std::to_string(standardGrade) + "]\n";
        prompt += "- Subjects: Science (Physics, Chemistry, Biology), Mathematics, Social Science (Economics, Civics, History, Geography).\n";
        prompt += "- Typography & Math: Use clean, standard LaTeX for all formulas, balanced chemical equations (e.g. \\text{2H}_2 + \\text{O}_2 \\rightarrow \\text{2H}_2\\text{O}), algebra, and geometry.\n";
        prompt += "- Social Science: Express economic and statistical calculations (GDP, per capita income, index numbers) with clean LaTeX formulas.\n";
    } else {
        prompt += "[Grade Calibration: Senior Secondary Standard " + std::to_string(standardGrade) + " • Stream: " + stream + "]\n";
        if (stream == "science_pcm") {
            prompt += "- Subjects: Physics, Chemistry, Advanced Mathematics, Computer Science / AI.\n";
            prompt += "- Typography: Use calibrated KaTeX + extended LaTeX hybrid for deep NCERT derivations, calculus (\\int, \\frac{d}{dx}, \\lim), vectors (\\vec{v}), matrices (\\begin{bmatrix}), electromagnetism, thermodynamics, and organic/inorganic reaction mechanisms (\\rightarrow, \\rightleftharpoons).\n";
        } else if (stream == "science_pcb") {
            prompt += "- Subjects: Physics, Chemistry, Biology (Genetics, Physiology, Ecology).\n";
            prompt += "- Typography: Use calibrated KaTeX for physical chemistry kinetics/thermodynamics, genetics Hardy-Weinberg equations ($p^2 + 2pq + q^2 = 1$), and chemical mechanisms.\n";
        } else if (stream == "commerce") {
            prompt += "- Subjects: Accountancy, Micro/Macro Economics, Business Mathematics, Statistics.\n";
            prompt += "- Typography: Use calibrated KaTeX for Accountancy (accounting equations, financial ratios, structured financial statements using aligned and array environments), and Economics/Statistics formulas (\\sum, elasticity, investment multiplier $k = \\frac{1}{1 - \\text{MPC}}$).\n";
        } else if (stream == "arts") {
            prompt += "- Subjects: History, Political Science, Economics, Psychology, Geography.\n";
            prompt += "- Typography: Use calibrated KaTeX for economic data analysis, demographic models, and statistical indices.\n";
        } else {
            prompt += "- Typography: Use calibrated KaTeX + LaTeX hybrid for all formulas, derivations, and calculations.\n";
        }
    }

    prompt += "\n[Strict Delimiter & Formatting Rules]\n";
    prompt += "1. Strict Delimiters: Always wrap inline math in $ ... $ and display equations/derivations in $$ ... $$ on separate lines.\n";
    prompt += "2. No Markdown Hash Pollution: Never attach markdown heading hashes (###, ##, #) inside or directly adjacent to math blocks.\n";
    prompt += "3. Crisp Textbook Typography: Ensure all backslashes in commands like \\frac, \\vec, \\int, \\rightarrow, and \\text are properly escaped and preserved.\n";
    prompt += "\nStudent Question: " + query;

    return env->NewStringUTF(prompt.c_str());
}

} // extern "C"

namespace cosmium_rag {

inline std::string toLowerAscii(const std::string& input) {
    std::string out;
    out.reserve(input.size());
    for (unsigned char ch : input) {
        out.push_back(static_cast<char>(std::tolower(ch)));
    }
    return out;
}

inline void replaceAllAscii(std::string& str, const std::string& from, const std::string& to) {
    if (from.empty()) return;
    size_t pos = 0;
    while ((pos = str.find(from, pos)) != std::string::npos) {
        str.replace(pos, from.length(), to);
        pos += to.length();
    }
}

inline int32_t javaStringHashCode(const std::string& s) {
    uint32_t h = 0;
    for (unsigned char ch : s) {
        h = h * 31u + static_cast<uint32_t>(ch);
    }
    return static_cast<int32_t>(h);
}

inline bool isTokenChar(char ch) {
    unsigned char c = static_cast<unsigned char>(ch);
    return std::isalnum(c) != 0 || c == '_';
}

inline bool isBridgeChar(char ch) {
    return ch == '+' || ch == '^' || ch == '=' || ch == '-';
}

inline const std::unordered_set<std::string>& stopWords() {
    static const std::unordered_set<std::string> kStop = {
        "the", "is", "are", "was", "were", "a", "an", "and", "or", "of", "to", "in",
        "for", "on", "with", "by", "from", "at", "as", "that", "this", "what", "how",
        "why", "when", "which", "who", "please", "explain", "solve", "tell", "me", "my",
        "can", "you", "it", "be", "into", "using", "about"
    };
    return kStop;
}

std::vector<std::string> tokenizeText(const std::string& raw) {
    std::string normalized = toLowerAscii(raw);
    replaceAllAscii(normalized, "f=ma", " f_eq_ma force mass acceleration newton ");
    replaceAllAscii(normalized, "v=ir", " v_eq_ir ohm voltage current resistance ");
    replaceAllAscii(normalized, "e=mc^2", " e_eq_mc2 energy relativity ");
    replaceAllAscii(normalized, "d/dx", " d_dx derivative differentiation calculus ");
    replaceAllAscii(normalized, "pv=nrt", " pv_eq_nrt ideal gas constant ");
    replaceAllAscii(normalized, "a=l+c", " accounting equation assets liabilities capital ");

    std::vector<std::string> tokens;
    const auto& stops = stopWords();
    size_t n = normalized.size();
    size_t i = 0;
    while (i < n) {
        if (!isTokenChar(normalized[i])) {
            ++i;
            continue;
        }
        size_t start = i;
        while (i < n && isTokenChar(normalized[i])) {
            ++i;
        }
        if (i + 1 < n && isBridgeChar(normalized[i]) && isTokenChar(normalized[i + 1])) {
            ++i;
            while (i < n && isTokenChar(normalized[i])) {
                ++i;
            }
        }
        std::string tok = normalized.substr(start, i - start);
        if (tok.size() >= 2 && stops.find(tok) == stops.end()) {
            tokens.push_back(std::move(tok));
        }
    }
    return tokens;
}

struct ConceptGroup {
    const char* root;
    std::vector<const char*> synonyms;
};

const std::vector<ConceptGroup>& conceptGroups() {
    static const std::vector<ConceptGroup> kGroups = {
        {"derivative", {"differentiation", "slope", "tangent", "calculus", "rate", "dx"}},
        {"integral", {"integration", "antiderivative", "area", "substitution", "calculus"}},
        {"limit", {"continuity", "approaches", "calculus", "indeterminate"}},
        {"newton", {"force", "mass", "acceleration", "inertia", "momentum", "friction"}},
        {"kirchhoff", {"circuit", "current", "voltage", "loop", "junction", "wheatstone", "resistor"}},
        {"ohm", {"voltage", "current", "resistance", "circuit", "joule", "power"}},
        {"mirror", {"lens", "optics", "refraction", "snell", "focal", "reflection"}},
        {"thermo", {"thermodynamics", "heat", "entropy", "internal", "isothermal", "adiabatic"}},
        {"redox", {"oxidation", "reduction", "electron", "nernst", "electrochemistry"}},
        {"quantum", {"orbital", "bohr", "electron", "pauli", "spin", "atomic"}},
        {"mitosis", {"meiosis", "cell", "chromosome", "diploid", "haploid", "genetics"}},
        {"dna", {"rna", "replication", "transcription", "genetics", "mendel", "operon"}},
        {"debit", {"credit", "ledger", "journal", "accounting", "assets", "liabilities"}},
        {"elasticity", {"demand", "price", "marginal", "utility", "microeconomics"}},
        {"gdp", {"macroeconomics", "multiplier", "mpc", "national", "income"}},
        {"python", {"function", "recursion", "list", "algorithm", "complexity", "code"}}
    };
    return kGroups;
}

std::string normalizeSpeechAndMath(const std::string& raw) {
    std::string s = toLowerAscii(raw);
    replaceAllAscii(s, "equal to", " = ");
    replaceAllAscii(s, "equals", " = ");
    replaceAllAscii(s, "plus", " + ");
    replaceAllAscii(s, "minus", " - ");
    replaceAllAscii(s, "multiplied by", " * ");
    replaceAllAscii(s, "times", " * ");
    replaceAllAscii(s, "divided by", " / ");
    replaceAllAscii(s, "squared", "^2");
    replaceAllAscii(s, "cubed", "^3");
    // Collapse multiple spaces
    std::string collapsed;
    collapsed.reserve(s.size());
    bool prevSpace = true;
    for (char c : s) {
        if (std::isspace(static_cast<unsigned char>(c))) {
            if (!prevSpace) {
                collapsed.push_back(' ');
                prevSpace = true;
            }
        } else {
            collapsed.push_back(c);
            prevSpace = false;
        }
    }
    if (!collapsed.empty() && collapsed.back() == ' ') collapsed.pop_back();
    return collapsed;
}

std::string stripSpacesAndParens(const std::string& s) {
    std::string out;
    out.reserve(s.size());
    for (char c : s) {
        if (!std::isspace(static_cast<unsigned char>(c)) && c != '(' && c != ')') {
            out.push_back(c);
        }
    }
    return out;
}

} // namespace cosmium_rag

extern "C" {

JNIEXPORT jfloat JNICALL
Java_com_example_utils_NativeCoreBridge_nativeCosineSimilarity(
    JNIEnv* env,
    jobject thiz,
    jfloatArray aArray,
    jfloatArray bArray
) {
    if (!aArray || !bArray) return 0.0f;
    jsize lenA = env->GetArrayLength(aArray);
    jsize lenB = env->GetArrayLength(bArray);
    jsize n = std::min(lenA, lenB);
    if (n <= 0) return 0.0f;

    jfloat* aPtr = env->GetFloatArrayElements(aArray, nullptr);
    jfloat* bPtr = env->GetFloatArrayElements(bArray, nullptr);
    if (!aPtr || !bPtr) {
        if (aPtr) env->ReleaseFloatArrayElements(aArray, aPtr, JNI_ABORT);
        if (bPtr) env->ReleaseFloatArrayElements(bArray, bPtr, JNI_ABORT);
        return 0.0f;
    }

    double dot = 0.0;
    double normA = 0.0;
    double normB = 0.0;
    for (jsize i = 0; i < n; ++i) {
        double va = static_cast<double>(aPtr[i]);
        double vb = static_cast<double>(bPtr[i]);
        dot += va * vb;
        normA += va * va;
        normB += vb * vb;
    }

    env->ReleaseFloatArrayElements(aArray, aPtr, JNI_ABORT);
    env->ReleaseFloatArrayElements(bArray, bPtr, JNI_ABORT);

    double denom = std::sqrt(normA) * std::sqrt(normB);
    if (denom <= 1e-6) return 0.0f;
    double sim = dot / denom;
    return static_cast<jfloat>(std::clamp(sim, 0.0, 1.0));
}

JNIEXPORT jfloatArray JNICALL
Java_com_example_utils_NativeCoreBridge_nativeEncodeConceptVector(
    JNIEnv* env,
    jobject thiz,
    jstring textStr,
    jint dim
) {
    int vecDim = std::max(1, static_cast<int>(dim));
    std::vector<float> vec(vecDim, 0.0f);

    if (textStr) {
        const char* chars = env->GetStringUTFChars(textStr, nullptr);
        std::string input(chars ? chars : "");
        env->ReleaseStringUTFChars(textStr, chars);

        auto tokens = cosmium_rag::tokenizeText(input);
        auto addToken = [&](const std::string& token, float weight) {
            int32_t h1 = cosmium_rag::javaStringHashCode(token);
            int idx1 = static_cast<int>((static_cast<uint32_t>(h1) & 0x7FFFFFFFu) % static_cast<uint32_t>(vecDim));
            float sign1 = ((static_cast<uint32_t>(h1) >> 31) == 0u) ? 1.0f : -1.0f;
            vec[idx1] += sign1 * weight;

            int32_t h2 = cosmium_rag::javaStringHashCode(token + "_cosmium_b");
            int idx2 = static_cast<int>((static_cast<uint32_t>(h2) & 0x7FFFFFFFu) % static_cast<uint32_t>(vecDim));
            float sign2 = ((static_cast<uint32_t>(h2) >> 31) == 0u) ? 1.0f : -1.0f;
            vec[idx2] += sign2 * weight * 0.7f;
        };

        const auto& groups = cosmium_rag::conceptGroups();
        for (const auto& tok : tokens) {
            addToken(tok, 1.0f);
            for (const auto& group : groups) {
                bool match = (tok.find(group.root) != std::string::npos);
                if (!match) {
                    for (const char* syn : group.synonyms) {
                        if (tok.find(syn) != std::string::npos) {
                            match = true;
                            break;
                        }
                    }
                }
                if (match) {
                    addToken(std::string("syn_") + group.root, 0.65f);
                }
            }
        }

        if (tokens.size() >= 2) {
            for (size_t i = 0; i + 1 < tokens.size(); ++i) {
                addToken(tokens[i] + "_" + tokens[i + 1], 0.55f);
            }
        }

        double sumSq = 0.0;
        for (float v : vec) {
            sumSq += static_cast<double>(v) * static_cast<double>(v);
        }
        float norm = static_cast<float>(std::sqrt(sumSq));
        if (norm > 1e-6f) {
            for (float& v : vec) {
                v /= norm;
            }
        }
    }

    jfloatArray out = env->NewFloatArray(vecDim);
    if (out) env->SetFloatArrayRegion(out, 0, vecDim, vec.data());
    return out;
}

JNIEXPORT jboolean JNICALL
Java_com_example_utils_NativeCoreBridge_nativeEvaluateQuizAnswer(
    JNIEnv* env,
    jobject thiz,
    jstring studentAnswerStr,
    jstring canonicalAnswerStr,
    jstring keywordsPipeStr
) {
    if (!studentAnswerStr) return JNI_FALSE;
    const char* stuChars = env->GetStringUTFChars(studentAnswerStr, nullptr);
    std::string studentRaw(stuChars ? stuChars : "");
    env->ReleaseStringUTFChars(studentAnswerStr, stuChars);

    std::string studentNorm = cosmium_rag::normalizeSpeechAndMath(studentRaw);
    if (studentNorm.empty()) return JNI_FALSE;
    std::string studentCompact = cosmium_rag::stripSpacesAndParens(studentNorm);

    std::string keywordsPipe;
    if (keywordsPipeStr) {
        const char* kwChars = env->GetStringUTFChars(keywordsPipeStr, nullptr);
        keywordsPipe = kwChars ? kwChars : "";
        env->ReleaseStringUTFChars(keywordsPipeStr, kwChars);
    }

    std::vector<std::string> keywords;
    size_t start = 0;
    while (start <= keywordsPipe.size()) {
        size_t pos = keywordsPipe.find('|', start);
        std::string item = (pos == std::string::npos)
            ? keywordsPipe.substr(start)
            : keywordsPipe.substr(start, pos - start);
        std::string kwNorm = cosmium_rag::normalizeSpeechAndMath(item);
        if (!kwNorm.empty()) keywords.push_back(std::move(kwNorm));
        if (pos == std::string::npos) break;
        start = pos + 1;
    }

    for (const auto& kw : keywords) {
        if (studentNorm.find(kw) != std::string::npos) return JNI_TRUE;
        std::string kwCompact = cosmium_rag::stripSpacesAndParens(kw);
        if (kwCompact.size() >= 2 && studentCompact.find(kwCompact) != std::string::npos) {
            return JNI_TRUE;
        }
    }

    if (canonicalAnswerStr) {
        const char* canChars = env->GetStringUTFChars(canonicalAnswerStr, nullptr);
        std::string canonicalNorm = cosmium_rag::normalizeSpeechAndMath(canChars ? canChars : "");
        env->ReleaseStringUTFChars(canonicalAnswerStr, canChars);

        std::string canonicalCompact = cosmium_rag::stripSpacesAndParens(canonicalNorm);
        if (!canonicalCompact.empty() && studentCompact.find(canonicalCompact) != std::string::npos) {
            return JNI_TRUE;
        }
    }

    if (keywords.empty() && studentNorm.size() >= 4) {
        return JNI_TRUE;
    }
    return JNI_FALSE;
}

JNIEXPORT jint JNICALL
Java_com_example_utils_NativeCoreBridge_nativeChooseLudoBestToken(
    JNIEnv* env,
    jobject thiz,
    jintArray tokenPositionsArr,
    jintArray tokenStartTrackArr,
    jintArray enemyPositionsArr,
    jint dice,
    jintArray safeZonesArr
) {
    if (!tokenPositionsArr || !tokenStartTrackArr) return 0;
    jsize count = env->GetArrayLength(tokenPositionsArr);
    if (count <= 0) return -1;
    if (count == 1) return 0;

    std::vector<int> pos(count);
    std::vector<int> startTrack(count);
    env->GetIntArrayRegion(tokenPositionsArr, 0, count, pos.data());
    env->GetIntArrayRegion(tokenStartTrackArr, 0, count, startTrack.data());

    jsize enemyCount = enemyPositionsArr ? env->GetArrayLength(enemyPositionsArr) : 0;
    std::vector<int> enemies(enemyCount);
    if (enemyCount > 0) env->GetIntArrayRegion(enemyPositionsArr, 0, enemyCount, enemies.data());

    jsize safeCount = safeZonesArr ? env->GetArrayLength(safeZonesArr) : 0;
    std::vector<int> safeZones(safeCount);
    if (safeCount > 0) env->GetIntArrayRegion(safeZonesArr, 0, safeCount, safeZones.data());

    auto isSafe = [&](int p) {
        return std::find(safeZones.begin(), safeZones.end(), p) != safeZones.end();
    };

    auto computeDest = [&](int p, int startIdx) -> int {
        if (p == 57) return -2;
        if (p == -1) return (dice == 6) ? startIdx : -2;
        int rel = (p >= 52 && p <= 56) ? (p - 1) : ((p - startIdx + 52) % 52);
        int nextRel = rel + dice;
        if (nextRel > 56) return -2;
        if (nextRel == 56) return 57;
        if (nextRel >= 51 && nextRel <= 55) return nextRel + 1;
        return (startIdx + nextRel) % 52;
    };

    int bestIdx = 0;
    int bestScore = -100000;

    for (int i = 0; i < count; ++i) {
        int dest = computeDest(pos[i], startTrack[i]);
        if (dest == -2) continue;

        int score = 0;
        if (dest >= 0 && dest <= 51 && !isSafe(dest)) {
            if (std::find(enemies.begin(), enemies.end(), dest) != enemies.end()) {
                score += 10000; // Tier 1: Capture opponent
            }
        }
        if (dest == 57) score += 5000;                          // Tier 2: Enter center home
        if (dice == 6 && pos[i] == -1) score += 2500;           // Tier 3: Exit yard
        if ((dest >= 52 && dest <= 56) || isSafe(dest)) score += 1200; // Tier 4: Safe/home stretch

        int progress = (pos[i] >= 52 && pos[i] <= 56)
            ? pos[i]
            : ((pos[i] == -1) ? 0 : ((pos[i] - startTrack[i] + 52) % 52));
        score += progress;

        if (score > bestScore) {
            bestScore = score;
            bestIdx = i;
        }
    }

    return bestIdx;
}

JNIEXPORT jint JNICALL
Java_com_example_utils_NativeCoreBridge_nativeChooseChaturangaBestMove(
    JNIEnv* env,
    jobject thiz,
    jintArray capturedPointsArr,
    jbooleanArray capturedIsRajaArr,
    jintArray toRowsArr,
    jintArray toColsArr
) {
    if (!capturedPointsArr || !capturedIsRajaArr || !toRowsArr || !toColsArr) return 0;
    jsize count = env->GetArrayLength(capturedPointsArr);
    if (count <= 0) return -1;
    if (count == 1) return 0;

    std::vector<int> pts(count);
    std::vector<jboolean> isRaja(count);
    std::vector<int> rows(count);
    std::vector<int> cols(count);

    env->GetIntArrayRegion(capturedPointsArr, 0, count, pts.data());
    env->GetBooleanArrayRegion(capturedIsRajaArr, 0, count, isRaja.data());
    env->GetIntArrayRegion(toRowsArr, 0, count, rows.data());
    env->GetIntArrayRegion(toColsArr, 0, count, cols.data());

    int bestScore = -100000;
    std::vector<int> bestIndices;

    for (int i = 0; i < count; ++i) {
        int score = 0;
        if (isRaja[i] == JNI_TRUE) {
            score += 50000;
        } else if (pts[i] > 0) {
            score += pts[i] * 100;
        }
        // Subtle center control bonus (rows/cols 2..5)
        int distCenter = std::abs(rows[i] * 2 - 7) + std::abs(cols[i] * 2 - 7);
        score += (14 - distCenter);

        if (score > bestScore) {
            bestScore = score;
            bestIndices.clear();
            bestIndices.push_back(i);
        } else if (score == bestScore) {
            bestIndices.push_back(i);
        }
    }

    if (bestIndices.empty()) return 0;
    return bestIndices[rand() % bestIndices.size()];
}

} // extern "C"



