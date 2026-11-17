#include <jni.h>
#include <string>
#include "cosmium_renderer.h"
#include "pasa_dice_renderer.h"
#include "CosmiumTtsEngine.h"

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_example_ui_components_CosmiumNativeBridge_nativeCreate(
    JNIEnv* env,
    jobject thiz
) {
    return reinterpret_cast<jlong>(new CosmiumRenderer());
}

JNIEXPORT void JNICALL
Java_com_example_ui_components_CosmiumNativeBridge_nativeInit(
    JNIEnv* env,
    jobject thiz,
    jlong handle,
    jint width,
    jint height
) {
    auto* renderer = reinterpret_cast<CosmiumRenderer*>(handle);
    if (renderer) {
        renderer->init(width, height);
    }
}

JNIEXPORT void JNICALL
Java_com_example_ui_components_CosmiumNativeBridge_nativeResize(
    JNIEnv* env,
    jobject thiz,
    jlong handle,
    jint width,
    jint height
) {
    auto* renderer = reinterpret_cast<CosmiumRenderer*>(handle);
    if (renderer) {
        renderer->resize(width, height);
    }
}

JNIEXPORT void JNICALL
Java_com_example_ui_components_CosmiumNativeBridge_nativeRender(
    JNIEnv* env,
    jobject thiz,
    jlong handle,
    jfloat dt,
    jstring stateStr,
    jfloat interactionIntensity,
    jfloat moodTurbulence,
    jint colorHex
) {
    auto* renderer = reinterpret_cast<CosmiumRenderer*>(handle);
    if (renderer) {
        const char* stateChars = stateStr ? env->GetStringUTFChars(stateStr, nullptr) : nullptr;
        std::string state(stateChars ? stateChars : "idle");
        if (stateChars) {
            env->ReleaseStringUTFChars(stateStr, stateChars);
        }
        renderer->render(dt, state, interactionIntensity, moodTurbulence, colorHex);
    }
}

JNIEXPORT void JNICALL
Java_com_example_ui_components_CosmiumNativeBridge_nativeDestroy(
    JNIEnv* env,
    jobject thiz,
    jlong handle
) {
    auto* renderer = reinterpret_cast<CosmiumRenderer*>(handle);
    if (renderer) {
        renderer->destroy();
        delete renderer;
    }
}

// --- 3D Pasa Dice JNI Entry Points ---

JNIEXPORT jlong JNICALL
Java_com_example_ui_components_PasaDiceNativeBridge_nativeCreate(
    JNIEnv* env,
    jobject thiz
) {
    return reinterpret_cast<jlong>(new PasaDiceRenderer());
}

JNIEXPORT void JNICALL
Java_com_example_ui_components_PasaDiceNativeBridge_nativeInit(
    JNIEnv* env,
    jobject thiz,
    jlong handle,
    jint width,
    jint height
) {
    auto* renderer = reinterpret_cast<PasaDiceRenderer*>(handle);
    if (renderer) {
        renderer->init(width, height);
    }
}

JNIEXPORT void JNICALL
Java_com_example_ui_components_PasaDiceNativeBridge_nativeResize(
    JNIEnv* env,
    jobject thiz,
    jlong handle,
    jint width,
    jint height
) {
    auto* renderer = reinterpret_cast<PasaDiceRenderer*>(handle);
    if (renderer) {
        renderer->resize(width, height);
    }
}

JNIEXPORT void JNICALL
Java_com_example_ui_components_PasaDiceNativeBridge_nativeRender(
    JNIEnv* env,
    jobject thiz,
    jlong handle,
    jfloat dt,
    jint diceValue,
    jboolean isRolling,
    jint accentColorHex,
    jboolean enabled
) {
    auto* renderer = reinterpret_cast<PasaDiceRenderer*>(handle);
    if (renderer) {
        renderer->render(dt, diceValue, isRolling == JNI_TRUE, accentColorHex, enabled == JNI_TRUE);
    }
}

JNIEXPORT void JNICALL
Java_com_example_ui_components_PasaDiceNativeBridge_nativeDestroy(
    JNIEnv* env,
    jobject thiz,
    jlong handle
) {
    auto* renderer = reinterpret_cast<PasaDiceRenderer*>(handle);
    if (renderer) {
        renderer->destroy();
        delete renderer;
    }
}

// --- Cosmium TTS & RMS Telemetry JNI Entry Points ---

JNIEXPORT jlong JNICALL
Java_com_example_ui_components_CosmiumTtsBridge_nativeCreate(JNIEnv* env, jobject thiz) {
    return reinterpret_cast<jlong>(new CosmiumTtsEngine());
}

JNIEXPORT jboolean JNICALL
Java_com_example_ui_components_CosmiumTtsBridge_nativeInit(JNIEnv* env, jobject thiz, jlong handle, jstring modelPath, jstring voicesPath, jstring tokensPath) {
    auto* engine = reinterpret_cast<CosmiumTtsEngine*>(handle);
    if (!engine) return JNI_FALSE;
    const char* m = modelPath ? env->GetStringUTFChars(modelPath, nullptr) : "";
    const char* v = voicesPath ? env->GetStringUTFChars(voicesPath, nullptr) : "";
    const char* t = tokensPath ? env->GetStringUTFChars(tokensPath, nullptr) : "";
    bool ok = engine->initialize(m, v, t);
    if (modelPath) env->ReleaseStringUTFChars(modelPath, m);
    if (voicesPath) env->ReleaseStringUTFChars(voicesPath, v);
    if (tokensPath) env->ReleaseStringUTFChars(tokensPath, t);
    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_example_ui_components_CosmiumTtsBridge_nativeSynthesize(JNIEnv* env, jobject thiz, jlong handle, jstring textStr) {
    auto* engine = reinterpret_cast<CosmiumTtsEngine*>(handle);
    if (engine) {
        const char* chars = textStr ? env->GetStringUTFChars(textStr, nullptr) : "";
        engine->synthesizeAndPlay(chars ? chars : "");
        if (textStr) env->ReleaseStringUTFChars(textStr, chars);
    }
}

JNIEXPORT jfloat JNICALL
Java_com_example_ui_components_CosmiumTtsBridge_nativeGetRms(JNIEnv* env, jobject thiz, jlong handle) {
    auto* engine = reinterpret_cast<CosmiumTtsEngine*>(handle);
    return engine ? engine->getCurrentRms() : 0.0f;
}

JNIEXPORT jboolean JNICALL
Java_com_example_ui_components_CosmiumTtsBridge_nativeIsSpeaking(JNIEnv* env, jobject thiz, jlong handle) {
    auto* engine = reinterpret_cast<CosmiumTtsEngine*>(handle);
    return engine && engine->isSpeaking() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_example_ui_components_CosmiumTtsBridge_nativeDestroy(JNIEnv* env, jobject thiz, jlong handle) {
    auto* engine = reinterpret_cast<CosmiumTtsEngine*>(handle);
    if (engine) {
        engine->destroy();
        delete engine;
    }
}

}
