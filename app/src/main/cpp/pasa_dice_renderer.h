#ifndef PASA_DICE_RENDERER_H
#define PASA_DICE_RENDERER_H

#include <GLES3/gl3.h>
#include "cosmium_renderer.h"

struct DiceProgramUniforms {
    GLuint program = 0;
    GLint uViewProj = -1;
    GLint uModel = -1;
    GLint uAccentColor = -1;
    GLint uIsRolling = -1;
    GLint uEnabled = -1;
    GLint uTime = -1;
};

class PasaDiceRenderer {
public:
    PasaDiceRenderer();
    ~PasaDiceRenderer();

    void init(int width, int height);
    void resize(int width, int height);
    void render(float dt, int diceValue, bool isRolling, int accentColorHex, bool enabled);
    void destroy();

private:
    void setupShaders();
    void cleanupShaders();
    void setupMesh(bool lowLod);
    void cleanupMesh();

    DiceProgramUniforms highLodProgram_;
    DiceProgramUniforms lowLodProgram_;

    GLuint cubeVao_ = 0;
    GLuint cubeVbo_ = 0;
    GLuint cubeEbo_ = 0;
    int cubeIndexCount_ = 0;

    AutoLodController lodController_;
    bool initialized_ = false;
    float aspect_ = 1.0f;
    float totalTime_ = 0.0f;

    // Smooth 3D physical tumble & settle state
    bool wasRolling_ = false;
    int lastTargetValue_ = 1;
    float rotX_ = 0.12f;
    float rotY_ = -0.14f;
    float rotZ_ = 0.0f;
    float velX_ = 0.0f;
    float velY_ = 0.0f;
    float velZ_ = 0.0f;
    float settleBounce_ = 0.0f;
    float rollLift_ = 0.0f;
};

#endif // PASA_DICE_RENDERER_H
