#ifndef COSMIUM_TTS_ENGINE_H
#define COSMIUM_TTS_ENGINE_H

#include <string>
#include <atomic>
#include <memory>
#include <vector>

class CosmiumTtsEngine {
public:
    CosmiumTtsEngine();
    ~CosmiumTtsEngine();

    bool initialize(const std::string& modelPath, const std::string& voicesPath, const std::string& tokensPath);
    void setLanguage(const std::string& langCode);
    void synthesizeAndPlay(const std::string& text);
    void stop();
    void destroy();

    float getCurrentRms() const {
        return currentRms_.load(std::memory_order_relaxed);
    }

    bool isSpeaking() const {
        return isSpeaking_.load(std::memory_order_relaxed);
    }

private:
    std::atomic<float> currentRms_{0.0f};
    std::atomic<bool> isSpeaking_{false};
    std::string currentLang_{"en"};
    bool isInitialized_{false};
};

#endif // COSMIUM_TTS_ENGINE_H
