#ifndef COSMIUM_RENDERER_H
#define COSMIUM_RENDERER_H

#include <GLES3/gl3.h>
#include <array>
#include <string>
#include <vector>

struct EmberParticle {
    float baseX;
    float baseY;
    float baseZ;
    float radius;
    float speed;
    float angleOffset;
    float size;
};

/**
 * Rolling 60-frame automatic LOD controller.
 * Switches to low-density shaders and low-density geometry whenever
 * the rolling average frame time exceeds 33.0 ms (< 30 FPS).
 */
class AutoLodController {
public:
    void reset(bool startLowLod);
    // Returns true if LOD level changed
    bool recordFrameTime(float frameTimeMs);
    bool isLowLod() const { return lowLod_; }

private:
    static constexpr size_t kWindowSize = 60;
    static constexpr float kHighToLowThresholdMs = 33.0f;
    static constexpr float kLowToHighThresholdMs = 22.0f;

    std::array<float, kWindowSize> samples_{};
    size_t writeIndex_ = 0;
    size_t sampleCount_ = 0;
    float rollingSum_ = 0.0f;
    int cooldownFrames_ = 0;
    bool lowLod_ = false;
};

struct FluidProgramUniforms {
    GLuint program = 0;
    GLint uViewProj = -1;
    GLint uModel = -1;
    GLint uTime = -1;
    GLint uInteraction = -1;
    GLint uMood = -1;
    GLint uIsThinking = -1;
    GLint uIsOuter = -1;
    GLint uBaseColor = -1;
    GLint uEmissive = -1;
    GLint uOpacity = -1;
};

class CosmiumRenderer {
public:
    CosmiumRenderer();
    ~CosmiumRenderer();

    void init(int width, int height);
    void resize(int width, int height);
    void render(float dt, const std::string& state, float interactionIntensity, float moodTurbulence, int colorHex);
    void destroy();

private:
    void setupShaders();
    void cleanupShaders();
    void setupMeshes(bool lowLod);
    void cleanupMeshes();

    FluidProgramUniforms highLodProgram_;
    FluidProgramUniforms lowLodProgram_;
    GLuint unlitShaderProgram_ = 0;
    GLuint emberInstancedProgram_ = 0;

    // Cached uniform locations (Unlit Core & Sound Pulse Waves)
    GLint uUnlitMvpLoc_ = -1;
    GLint uUnlitColorLoc_ = -1;
    GLint uUnlitAlphaLoc_ = -1;

    // Cached uniform locations (Instanced Embers)
    GLint uEmberColorLoc_ = -1;
    GLint uEmberTimeLoc_ = -1;

    // Mesh VAOs/VBOs/EBOs
    GLuint mainSphereVao_ = 0;
    GLuint mainSphereVbo_ = 0;
    GLuint mainSphereEbo_ = 0;
    int mainSphereIndexCount_ = 0;

    GLuint outerLayerVao_ = 0;
    GLuint outerLayerVbo_ = 0;
    GLuint outerLayerEbo_ = 0;
    int outerLayerIndexCount_ = 0;

    GLuint coreVao_ = 0;
    GLuint coreVbo_ = 0;
    GLuint coreEbo_ = 0;
    int coreIndexCount_ = 0;

    GLuint emberVao_ = 0;
    GLuint emberVbo_ = 0;
    GLuint emberEbo_ = 0;
    GLuint emberInstanceVbo_ = 0;
    int emberIndexCount_ = 0;

    GLuint ringVao_ = 0;
    GLuint ringVbo_ = 0;
    GLuint ringEbo_ = 0;
    int ringIndexCount_ = 0;

    std::vector<EmberParticle> embers_;
    std::vector<float> emberInstanceMatrices_;
    AutoLodController lodController_;
    bool initialized_ = false;
    float aspect_ = 1.0f;
    float totalTime_ = 0.0f;

    // Accumulated rotational state per spec
    float mainRotX_ = 0.0f;
    float mainRotY_ = 0.0f;
    float mainRotZ_ = 0.0f;
    float outerRotX_ = 0.0f;
    float outerRotY_ = 0.0f;
    float emberGroupRotY_ = 0.0f;
};

#endif // COSMIUM_RENDERER_H
