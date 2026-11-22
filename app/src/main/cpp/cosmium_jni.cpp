#include <jni.h>
#include <algorithm>
#include <cstdint>
#include <string>
#include <vector>

#include "cosmium_audio.h"
#include "cosmium_games.h"
#include "cosmium_math.h"
#include "cosmium_rag.h"
#include "cosmium_renderer.h"
#include "pasa_dice_renderer.h"

namespace {

class JniUtfString {
public:
    JniUtfString(JNIEnv* env, jstring jStr, const char* fallback = "")
        : env_(env), jStr_(jStr) {
        if (env_ && jStr_) {
            const char* raw = env_->GetStringUTFChars(jStr_, nullptr);
            value_ = raw ? raw : fallback;
            if (raw) {
                env_->ReleaseStringUTFChars(jStr_, raw);
            }
        } else {
            value_ = fallback;
        }
    }

    const std::string& str() const { return value_; }

private:
    JNIEnv* env_;
    jstring jStr_;
    std::string value_;
};

std::vector<int> readIntArray(JNIEnv* env, jintArray array) {
    if (!env || !array) return {};
    jsize len = env->GetArrayLength(array);
    if (len <= 0) return {};
    std::vector<int> out(static_cast<size_t>(len));
    env->GetIntArrayRegion(array, 0, len, out.data());
    return out;
}

jshortArray toJShortArray(JNIEnv* env, const std::vector<int16_t>& data) {
    jsize len = static_cast<jsize>(data.size());
    jshortArray out = env->NewShortArray(len);
    if (out && len > 0) {
        env->SetShortArrayRegion(out, 0, len, data.data());
    }
    return out;
}

jintArray toJIntArray(JNIEnv* env, const std::vector<int>& data) {
    jsize len = static_cast<jsize>(data.size());
    jintArray out = env->NewIntArray(len);
    if (out && len > 0) {
        env->SetIntArrayRegion(out, 0, len, data.data());
    }
    return out;
}

} // namespace

extern "C" {

// =========================================================================
// 1. OpenGL ES 3.0 Visual Engines (Cosmium Orb & 3D Pasa Dice)
// =========================================================================

JNIEXPORT jlong JNICALL
Java_com_example_ui_components_CosmiumNativeBridge_nativeCreate(JNIEnv*, jobject) {
    return reinterpret_cast<jlong>(new CosmiumRenderer());
}

JNIEXPORT void JNICALL
Java_com_example_ui_components_CosmiumNativeBridge_nativeInit(
    JNIEnv*, jobject, jlong handle, jint width, jint height
) {
    if (auto* renderer = reinterpret_cast<CosmiumRenderer*>(handle)) {
        renderer->init(width, height);
    }
}

JNIEXPORT void JNICALL
Java_com_example_ui_components_CosmiumNativeBridge_nativeResize(
    JNIEnv*, jobject, jlong handle, jint width, jint height
) {
    if (auto* renderer = reinterpret_cast<CosmiumRenderer*>(handle)) {
        renderer->resize(width, height);
    }
}

JNIEXPORT void JNICALL
Java_com_example_ui_components_CosmiumNativeBridge_nativeRender(
    JNIEnv* env,
    jobject,
    jlong handle,
    jfloat dt,
    jstring stateStr,
    jfloat interactionIntensity,
    jfloat moodTurbulence,
    jint colorHex
) {
    if (auto* renderer = reinterpret_cast<CosmiumRenderer*>(handle)) {
        JniUtfString state(env, stateStr, "idle");
        renderer->render(dt, state.str(), interactionIntensity, moodTurbulence, colorHex);
    }
}

JNIEXPORT void JNICALL
Java_com_example_ui_components_CosmiumNativeBridge_nativeDestroy(JNIEnv*, jobject, jlong handle) {
    if (auto* renderer = reinterpret_cast<CosmiumRenderer*>(handle)) {
        renderer->destroy();
        delete renderer;
    }
}

JNIEXPORT jlong JNICALL
Java_com_example_ui_components_PasaDiceNativeBridge_nativeCreate(JNIEnv*, jobject) {
    return reinterpret_cast<jlong>(new PasaDiceRenderer());
}

JNIEXPORT void JNICALL
Java_com_example_ui_components_PasaDiceNativeBridge_nativeInit(
    JNIEnv*, jobject, jlong handle, jint width, jint height
) {
    if (auto* renderer = reinterpret_cast<PasaDiceRenderer*>(handle)) {
        renderer->init(width, height);
    }
}

JNIEXPORT void JNICALL
Java_com_example_ui_components_PasaDiceNativeBridge_nativeResize(
    JNIEnv*, jobject, jlong handle, jint width, jint height
) {
    if (auto* renderer = reinterpret_cast<PasaDiceRenderer*>(handle)) {
        renderer->resize(width, height);
    }
}

JNIEXPORT void JNICALL
Java_com_example_ui_components_PasaDiceNativeBridge_nativeRender(
    JNIEnv*,
    jobject,
    jlong handle,
    jfloat dt,
    jint diceValue,
    jboolean isRolling,
    jint accentColorHex,
    jboolean enabled
) {
    if (auto* renderer = reinterpret_cast<PasaDiceRenderer*>(handle)) {
        renderer->render(dt, diceValue, isRolling == JNI_TRUE, accentColorHex, enabled == JNI_TRUE);
    }
}

JNIEXPORT void JNICALL
Java_com_example_ui_components_PasaDiceNativeBridge_nativeDestroy(JNIEnv*, jobject, jlong handle) {
    if (auto* renderer = reinterpret_cast<PasaDiceRenderer*>(handle)) {
        renderer->destroy();
        delete renderer;
    }
}

// =========================================================================
// 2. Cosmium Math, KaTeX HTML Compilation & Prompt Calibration
// =========================================================================

JNIEXPORT jstring JNICALL
Java_com_example_utils_NativeCoreBridge_nativeCosmosSystemInstruction(JNIEnv* env, jobject) {
    return env->NewStringUTF(cosmium::math::cosmosSystemInstruction().c_str());
}

JNIEXPORT jstring JNICALL
Java_com_example_utils_NativeCoreBridge_nativeSanitizeAndFormatMath(
    JNIEnv* env,
    jobject,
    jstring rawTextStr,
    jint standardGrade,
    jstring streamIdStr
) {
    JniUtfString raw(env, rawTextStr);
    JniUtfString stream(env, streamIdStr, "general");
    std::string formatted = cosmium::math::sanitizeAndFormatMath(raw.str(), standardGrade, stream.str());
    return env->NewStringUTF(formatted.c_str());
}

JNIEXPORT jstring JNICALL
Java_com_example_utils_NativeCoreBridge_nativeCalibrateCosmosPrompt(
    JNIEnv* env,
    jobject,
    jstring userQueryStr,
    jint standardGrade,
    jstring streamIdStr
) {
    JniUtfString query(env, userQueryStr);
    JniUtfString stream(env, streamIdStr, "general");
    std::string prompt = cosmium::math::calibrateCosmosPrompt(query.str(), standardGrade, stream.str());
    return env->NewStringUTF(prompt.c_str());
}

JNIEXPORT jstring JNICALL
Java_com_example_utils_NativeCoreBridge_nativeBuildMarkdownWithProtectedMathHtml(
    JNIEnv* env,
    jobject,
    jstring rawTextStr
) {
    JniUtfString raw(env, rawTextStr);
    std::string html = cosmium::math::buildMarkdownWithProtectedMathHtml(raw.str());
    return env->NewStringUTF(html.c_str());
}

JNIEXPORT jstring JNICALL
Java_com_example_utils_NativeCoreBridge_nativeFormatReadableMathFallback(
    JNIEnv* env,
    jobject,
    jstring rawTextStr
) {
    JniUtfString raw(env, rawTextStr);
    std::string readable = cosmium::math::formatReadableMathFallback(raw.str());
    return env->NewStringUTF(readable.c_str());
}

JNIEXPORT jstring JNICALL
Java_com_example_utils_NativeCoreBridge_nativeFormatAsciiFormulaToLatex(
    JNIEnv* env,
    jobject,
    jstring rawFormulaStr
) {
    JniUtfString raw(env, rawFormulaStr);
    std::string latex = cosmium::math::formatAsciiFormulaToLatex(raw.str());
    return env->NewStringUTF(latex.c_str());
}

// =========================================================================
// 3. Offline RAG Tokenization, FTS4 Queries, Dense Vectors & Quiz Evaluation
// =========================================================================

JNIEXPORT jobjectArray JNICALL
Java_com_example_utils_NativeCoreBridge_nativeTokenizeRagText(
    JNIEnv* env,
    jobject,
    jstring rawTextStr
) {
    JniUtfString raw(env, rawTextStr);
    std::vector<std::string> tokens = cosmium::rag::tokenizeText(raw.str());
    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray result = env->NewObjectArray(static_cast<jsize>(tokens.size()), stringClass, nullptr);
    if (!result) return nullptr;
    for (size_t i = 0; i < tokens.size(); ++i) {
        jstring s = env->NewStringUTF(tokens[i].c_str());
        env->SetObjectArrayElement(result, static_cast<jsize>(i), s);
        env->DeleteLocalRef(s);
    }
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_example_utils_NativeCoreBridge_nativeBuildSafeFtsMatchQuery(
    JNIEnv* env,
    jobject,
    jstring rawQueryStr
) {
    JniUtfString query(env, rawQueryStr);
    std::string fts = cosmium::rag::buildSafeFtsMatchQuery(query.str());
    return env->NewStringUTF(fts.c_str());
}

JNIEXPORT jfloatArray JNICALL
Java_com_example_utils_NativeCoreBridge_nativeEncodeConceptVector(
    JNIEnv* env,
    jobject,
    jstring textStr,
    jint dim
) {
    JniUtfString input(env, textStr);
    std::vector<float> vec = cosmium::rag::encodeConceptVector(input.str(), dim);
    jsize len = static_cast<jsize>(vec.size());
    jfloatArray out = env->NewFloatArray(len);
    if (out && len > 0) {
        env->SetFloatArrayRegion(out, 0, len, vec.data());
    }
    return out;
}

JNIEXPORT jfloatArray JNICALL
Java_com_example_utils_NativeCoreBridge_nativeEncodeWeightedDocumentVector(
    JNIEnv* env,
    jobject,
    jstring topicTitleStr,
    jstring subjectNameStr,
    jstring formulaTagsStr,
    jstring contentTextStr,
    jint dim
) {
    JniUtfString title(env, topicTitleStr);
    JniUtfString subject(env, subjectNameStr);
    JniUtfString formulas(env, formulaTagsStr);
    JniUtfString content(env, contentTextStr);
    std::vector<float> vec = cosmium::rag::encodeWeightedDocumentVector(
        title.str(), subject.str(), formulas.str(), content.str(), dim
    );
    jsize len = static_cast<jsize>(vec.size());
    jfloatArray out = env->NewFloatArray(len);
    if (out && len > 0) {
        env->SetFloatArrayRegion(out, 0, len, vec.data());
    }
    return out;
}

JNIEXPORT jfloat JNICALL
Java_com_example_utils_NativeCoreBridge_nativeComputeTopicSpecificityScore(
    JNIEnv* env,
    jobject,
    jstring queryStr,
    jstring topicTitleStr,
    jstring subjectNameStr,
    jstring formulaTagsStr,
    jstring contentTextStr,
    jfloat denseSim,
    jfloat ftsScore,
    jfloat bm25Score
) {
    JniUtfString query(env, queryStr);
    JniUtfString title(env, topicTitleStr);
    JniUtfString subject(env, subjectNameStr);
    JniUtfString formulas(env, formulaTagsStr);
    JniUtfString content(env, contentTextStr);
    return cosmium::rag::computeTopicSpecificityScore(
        query.str(), title.str(), subject.str(), formulas.str(), content.str(),
        denseSim, ftsScore, bm25Score
    );
}

JNIEXPORT jstring JNICALL
Java_com_example_utils_NativeCoreBridge_nativeComposeFallbackTextbookResponse(
    JNIEnv* env,
    jobject,
    jstring topicTitleStr,
    jstring subjectNameStr,
    jint chapterNumber,
    jstring contentTextStr,
    jstring formulaTagsStr,
    jstring workedExampleStr,
    jboolean lowRamNotice
) {
    JniUtfString title(env, topicTitleStr);
    JniUtfString subject(env, subjectNameStr);
    JniUtfString content(env, contentTextStr);
    JniUtfString formulas(env, formulaTagsStr);
    JniUtfString example(env, workedExampleStr);
    std::string composed = cosmium::rag::composeFallbackTextbookResponse(
        title.str(), subject.str(), chapterNumber, content.str(), formulas.str(),
        example.str(), lowRamNotice == JNI_TRUE
    );
    return env->NewStringUTF(composed.c_str());
}

JNIEXPORT jfloat JNICALL
Java_com_example_utils_NativeCoreBridge_nativeCosineSimilarity(
    JNIEnv* env,
    jobject,
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
    float sim = 0.0f;
    if (aPtr && bPtr) {
        sim = cosmium::rag::cosineSimilarity(aPtr, bPtr, n);
    }
    if (aPtr) env->ReleaseFloatArrayElements(aArray, aPtr, JNI_ABORT);
    if (bPtr) env->ReleaseFloatArrayElements(bArray, bPtr, JNI_ABORT);
    return sim;
}

JNIEXPORT jboolean JNICALL
Java_com_example_utils_NativeCoreBridge_nativeEvaluateQuizAnswer(
    JNIEnv* env,
    jobject,
    jstring studentAnswerStr,
    jstring canonicalAnswerStr,
    jstring keywordsPipeStr
) {
    JniUtfString student(env, studentAnswerStr);
    JniUtfString canonical(env, canonicalAnswerStr);
    JniUtfString keywordsPipe(env, keywordsPipeStr);
    bool match = cosmium::rag::evaluateQuizAnswer(student.str(), canonical.str(), keywordsPipe.str());
    return match ? JNI_TRUE : JNI_FALSE;
}

// =========================================================================
// 4. Strategy Game Rules & AI Engines (Chess, Ludo, Chaturanga)
// =========================================================================

JNIEXPORT jboolean JNICALL
Java_com_example_utils_NativeCoreBridge_nativeIsChessKingAttacked(
    JNIEnv* env,
    jobject,
    jintArray boardArray,
    jint side
) {
    if (!boardArray || env->GetArrayLength(boardArray) != 64) return JNI_FALSE;
    int board[64];
    env->GetIntArrayRegion(boardArray, 0, 64, board);
    return cosmium::games::isChessKingAttacked(board, side) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_example_utils_NativeCoreBridge_nativeHasAnyChessLegalMoves(
    JNIEnv* env,
    jobject,
    jintArray boardArray,
    jint side
) {
    if (!boardArray || env->GetArrayLength(boardArray) != 64) return JNI_FALSE;
    int board[64];
    env->GetIntArrayRegion(boardArray, 0, 64, board);
    return cosmium::games::hasAnyChessLegalMoves(board, side) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jintArray JNICALL
Java_com_example_utils_NativeCoreBridge_nativeGetChessValidMoves(
    JNIEnv* env,
    jobject,
    jintArray boardArray,
    jint turnSide,
    jint fromRow,
    jint fromCol
) {
    if (!boardArray || env->GetArrayLength(boardArray) != 64) return env->NewIntArray(0);
    int board[64];
    env->GetIntArrayRegion(boardArray, 0, 64, board);
    std::vector<int> squares = cosmium::games::getChessValidMoves(board, turnSide, fromRow, fromCol);
    return toJIntArray(env, squares);
}

JNIEXPORT jint JNICALL
Java_com_example_utils_NativeCoreBridge_nativeComputeChessBotMove(
    JNIEnv* env,
    jobject,
    jintArray boardArray,
    jint turnSide,
    jint difficulty
) {
    if (!boardArray || env->GetArrayLength(boardArray) != 64) return -1;
    int board[64];
    env->GetIntArrayRegion(boardArray, 0, 64, board);
    return cosmium::games::computeChessBotMove(board, turnSide, difficulty);
}

JNIEXPORT jint JNICALL
Java_com_example_utils_NativeCoreBridge_nativeGetLudoDestinationPosition(
    JNIEnv*,
    jobject,
    jint position,
    jint startTrackIndex,
    jint dice
) {
    return cosmium::games::getLudoDestinationPosition(position, startTrackIndex, dice);
}

JNIEXPORT jint JNICALL
Java_com_example_utils_NativeCoreBridge_nativeChooseLudoBestToken(
    JNIEnv* env,
    jobject,
    jintArray tokenPositionsArr,
    jintArray tokenStartTrackArr,
    jintArray enemyPositionsArr,
    jint dice,
    jintArray safeZonesArr
) {
    std::vector<int> positions = readIntArray(env, tokenPositionsArr);
    std::vector<int> startTracks = readIntArray(env, tokenStartTrackArr);
    std::vector<int> enemies = readIntArray(env, enemyPositionsArr);
    std::vector<int> safeZones = readIntArray(env, safeZonesArr);
    return cosmium::games::chooseLudoBestToken(positions, startTracks, enemies, dice, safeZones);
}

JNIEXPORT jint JNICALL
Java_com_example_utils_NativeCoreBridge_nativeChooseChaturangaBestMove(
    JNIEnv* env,
    jobject,
    jintArray capturedPointsArr,
    jbooleanArray capturedIsRajaArr,
    jintArray toRowsArr,
    jintArray toColsArr
) {
    std::vector<int> pts = readIntArray(env, capturedPointsArr);
    std::vector<int> rows = readIntArray(env, toRowsArr);
    std::vector<int> cols = readIntArray(env, toColsArr);
    std::vector<bool> isRaja;
    if (capturedIsRajaArr) {
        jsize count = env->GetArrayLength(capturedIsRajaArr);
        if (count > 0) {
            std::vector<jboolean> raw(static_cast<size_t>(count));
            env->GetBooleanArrayRegion(capturedIsRajaArr, 0, count, raw.data());
            isRaja.reserve(raw.size());
            for (jboolean b : raw) {
                isRaja.push_back(b == JNI_TRUE);
            }
        }
    }
    return cosmium::games::chooseChaturangaBestMove(pts, isRaja, rows, cols);
}

// =========================================================================
// 5. Procedural Acoustic Audio & Music DSP
// =========================================================================

JNIEXPORT jshortArray JNICALL
Java_com_example_utils_NativeCoreBridge_nativeSynthesizeWoodyTock(
    JNIEnv* env,
    jobject,
    jint sampleRate,
    jdouble durationSec,
    jdouble baseFreq,
    jdouble sweep,
    jdouble volume
) {
    return toJShortArray(
        env,
        cosmium::audio::synthesizeWoodyTock(sampleRate, durationSec, baseFreq, sweep, volume)
    );
}

JNIEXPORT jshortArray JNICALL
Java_com_example_utils_NativeCoreBridge_nativeSynthesizeTempleChime(
    JNIEnv* env,
    jobject,
    jint sampleRate,
    jdouble durationSec,
    jdouble fundamental
) {
    return toJShortArray(
        env,
        cosmium::audio::synthesizeTempleChime(sampleRate, durationSec, fundamental)
    );
}

JNIEXPORT jshortArray JNICALL
Java_com_example_utils_NativeCoreBridge_nativeSynthesizeVictorySwell(
    JNIEnv* env,
    jobject,
    jint sampleRate,
    jdouble durationSec
) {
    return toJShortArray(env, cosmium::audio::synthesizeVictorySwell(sampleRate, durationSec));
}

JNIEXPORT void JNICALL
Java_com_example_utils_NativeCoreBridge_nativeSynthesizeMusicNote(
    JNIEnv* env,
    jobject,
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
    std::vector<int16_t> buf(static_cast<size_t>(len));
    cosmium::audio::synthesizeMusicNote(
        buf.data(),
        static_cast<int>(len),
        sampleRate,
        melodyFreq,
        harmonicFreq,
        droneFreq,
        isJoyful == JNI_TRUE,
        static_cast<int64_t>(globalSampleStart)
    );
    env->SetShortArrayRegion(outBuffer, 0, len, buf.data());
}

JNIEXPORT jshortArray JNICALL
Java_com_example_utils_NativeCoreBridge_nativeSynthesizeGameMusicStep(
    JNIEnv* env,
    jobject,
    jint gameTypeOrdinal,
    jint noteIndex,
    jint sampleRate,
    jlong globalSampleStart
) {
    return toJShortArray(
        env,
        cosmium::audio::synthesizeGameMusicStep(
            gameTypeOrdinal,
            noteIndex,
            sampleRate,
            static_cast<int64_t>(globalSampleStart)
        )
    );
}

} // extern "C"
