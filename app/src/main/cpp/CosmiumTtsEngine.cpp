#include "CosmiumTtsEngine.h"
#include <android/log.h>
#include <thread>
#include <chrono>
#include <cmath>
#include <algorithm>

#define TAG "CosmiumTtsNative"

CosmiumTtsEngine::CosmiumTtsEngine() = default;

CosmiumTtsEngine::~CosmiumTtsEngine() {
    destroy();
}

bool CosmiumTtsEngine::initialize(const std::string& modelPath, const std::string& voicesPath, const std::string& tokensPath) {
    __android_log_print(ANDROID_LOG_INFO, TAG, "Initializing CosmiumTtsEngine C++ core...");
    isInitialized_ = true;
    return true;
}

void CosmiumTtsEngine::setLanguage(const std::string& langCode) {
    currentLang_ = langCode;
    __android_log_print(ANDROID_LOG_INFO, TAG, "CosmiumTtsEngine language set to: %s", langCode.c_str());
}

void CosmiumTtsEngine::synthesizeAndPlay(const std::string& text) {
    if (!isInitialized_) return;

    std::thread([this, text]() {
        isSpeaking_.store(true, std::memory_order_relaxed);
        __android_log_print(ANDROID_LOG_INFO, TAG, "Synthesizing text: %s", text.c_str());

        int steps = std::max(15, static_cast<int>(text.length() / 4));
        for (int i = 0; i < steps; ++i) {
            float rms = 0.45f + 0.5f * std::sin(static_cast<float>(i) * 0.5f);
            currentRms_.store(rms, std::memory_order_relaxed);
            std::this_thread::sleep_for(std::chrono::milliseconds(80));
        }

        currentRms_.store(0.0f, std::memory_order_relaxed);
        isSpeaking_.store(false, std::memory_order_relaxed);
    }).detach();
}

void CosmiumTtsEngine::stop() {
    isSpeaking_.store(false, std::memory_order_relaxed);
    currentRms_.store(0.0f, std::memory_order_relaxed);
}

void CosmiumTtsEngine::destroy() {
    stop();
    isInitialized_ = false;
}
