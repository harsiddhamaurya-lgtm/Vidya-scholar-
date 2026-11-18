#include "pasa_dice_renderer.h"
#include <EGL/egl.h>
#include <GLES3/gl3.h>
#include <android/log.h>
#include <cmath>
#include <vector>
#include <string>
#include <algorithm>

static inline bool hasDiceEglContext() {
    return eglGetCurrentContext() != EGL_NO_CONTEXT;
}

static inline bool hasDiceDrawableContext() {
    return eglGetCurrentContext() != EGL_NO_CONTEXT &&
           eglGetCurrentSurface(EGL_DRAW) != EGL_NO_SURFACE;
}

#define LOG_TAG "PasaDiceNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Subdivided superellipsoid / bevelled rounded cube with per-face UV and face index (1..6)
static const char* kDiceHighVertexShaderSrc = R"glsl(#version 300 es
layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aUv;
layout(location = 3) in float aFaceId;

uniform mat4 uViewProj;
uniform mat4 uModel;

out vec3 vNormal;
out vec3 vWorldPos;
out vec2 vUv;
out float vFaceId;

void main() {
    vec4 worldPos = uModel * vec4(aPosition, 1.0);
    vec3 worldNorm = mat3(uModel) * aNormal;
    float len = length(worldNorm);
    vNormal = len > 0.0001 ? (worldNorm / len) : vec3(0.0, 0.0, 1.0);
    vWorldPos = worldPos.xyz;
    vUv = aUv;
    vFaceId = aFaceId;
    gl_Position = uViewProj * worldPos;
}
)glsl";

static const char* kDiceHighFragmentShaderSrc = R"glsl(#version 300 es
precision highp float;

in vec3 vNormal;
in vec3 vWorldPos;
in vec2 vUv;
in float vFaceId;

uniform vec3 uAccentColor;
uniform int uIsRolling;
uniform int uEnabled;
uniform float uTime;

out vec4 fragColor;

float pipDistance(vec2 uv, vec2 center) {
    return length(uv - center);
}

float computePipsMask(vec2 uv, int face) {
    float r = 0.105;
    float d = 1.0;

    if (face == 1) {
        d = pipDistance(uv, vec2(0.5, 0.5));
    } else if (face == 2) {
        d = min(pipDistance(uv, vec2(0.27, 0.27)), pipDistance(uv, vec2(0.73, 0.73)));
    } else if (face == 3) {
        d = min(pipDistance(uv, vec2(0.26, 0.26)),
                min(pipDistance(uv, vec2(0.5, 0.5)), pipDistance(uv, vec2(0.74, 0.74))));
    } else if (face == 4) {
        float d1 = min(pipDistance(uv, vec2(0.27, 0.27)), pipDistance(uv, vec2(0.73, 0.27)));
        float d2 = min(pipDistance(uv, vec2(0.27, 0.73)), pipDistance(uv, vec2(0.73, 0.73)));
        d = min(d1, d2);
    } else if (face == 5) {
        float d1 = min(pipDistance(uv, vec2(0.26, 0.26)), pipDistance(uv, vec2(0.74, 0.26)));
        float d2 = min(pipDistance(uv, vec2(0.26, 0.74)), pipDistance(uv, vec2(0.74, 0.74)));
        d = min(min(d1, d2), pipDistance(uv, vec2(0.5, 0.5)));
    } else {
        float d1 = min(pipDistance(uv, vec2(0.28, 0.22)), pipDistance(uv, vec2(0.72, 0.22)));
        float d2 = min(pipDistance(uv, vec2(0.28, 0.50)), pipDistance(uv, vec2(0.72, 0.50)));
        float d3 = min(pipDistance(uv, vec2(0.28, 0.78)), pipDistance(uv, vec2(0.72, 0.78)));
        d = min(d1, min(d2, d3));
    }
    return 1.0 - smoothstep(r - 0.025, r + 0.015, d);
}

void main() {
    vec3 N = normalize(vNormal);
    vec3 V = vec3(0.0, 0.0, 1.0);
    vec3 L = normalize(vec3(0.45, 0.75, 0.90));
    vec3 H = normalize(L + V);

    float diff = max(dot(N, L), 0.0);
    float spec = pow(max(dot(N, H), 0.0), 42.0);
    float fresnel = 0.06 + 0.94 * pow(1.0 - clamp(abs(dot(N, V)), 0.0, 1.0), 3.0);

    // Rich carved lacquer & gold-rimmed bevel aesthetic
    vec3 gold = vec3(0.95, 0.78, 0.25);
    vec3 ivory = vec3(0.99, 0.98, 0.95);

    // Subtle bevel border ring per face
    float edgeDist = max(abs(vUv.x - 0.5), abs(vUv.y - 0.5));
    float goldBevel = smoothstep(0.41, 0.46, edgeDist) * (1.0 - smoothstep(0.47, 0.50, edgeDist));

    int faceInt = int(floor(vFaceId + 0.5));
    float pipMask = computePipsMask(vUv, faceInt);

    vec3 bodyBase = mix(uAccentColor, uAccentColor * 0.65, edgeDist * 0.6);
    if (uEnabled == 0 && uIsRolling == 0) {
        bodyBase = mix(bodyBase, vec3(0.35), 0.35);
    }

    vec3 litBody = bodyBase * (0.42 + 0.68 * diff) + vec3(1.0) * spec * 0.45 + gold * fresnel * (uIsRolling == 1 ? 0.55 : 0.25);
    litBody = mix(litBody, gold * (0.75 + 0.45 * diff), goldBevel * 0.72);

    // Luminous ivory/gold pips with soft bevel rim
    vec3 pipColor = mix(ivory, gold, 0.15) * (0.92 + 0.2 * diff);
    vec3 finalColor = mix(litBody, pipColor, pipMask);

    if (uIsRolling == 1) {
        float pulse = 0.12 + 0.10 * sin(uTime * 12.0);
        finalColor += gold * pulse * fresnel;
    }

    finalColor = clamp(finalColor, 0.0, 1.0);
    fragColor = vec4(finalColor, 1.0);
}
)glsl";

// --- LOW-DENSITY SHADER PATH (>33ms fallback) ---
static const char* kDiceLowVertexShaderSrc = R"glsl(#version 300 es
precision mediump float;
layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aUv;
layout(location = 3) in float aFaceId;

uniform mat4 uViewProj;
uniform mat4 uModel;

out vec3 vNormal;
out vec2 vUv;
out float vFaceId;

void main() {
    vec3 worldNorm = mat3(uModel) * aNormal;
    float len = length(worldNorm);
    vNormal = len > 0.0001 ? (worldNorm / len) : vec3(0.0, 0.0, 1.0);
    vUv = aUv;
    vFaceId = aFaceId;
    gl_Position = uViewProj * (uModel * vec4(aPosition, 1.0));
}
)glsl";

static const char* kDiceLowFragmentShaderSrc = R"glsl(#version 300 es
precision mediump float;

in vec3 vNormal;
in vec2 vUv;
in float vFaceId;

uniform vec3 uAccentColor;
uniform int uIsRolling;
uniform int uEnabled;

out vec4 fragColor;

float pipDist(vec2 uv, vec2 c) { return length(uv - c); }

void main() {
    float diff = max(dot(normalize(vNormal), normalize(vec3(0.45, 0.75, 0.90))), 0.0);
    float r = 0.11;
    int face = int(floor(vFaceId + 0.5));
    float d = 1.0;
    if (face == 1) d = pipDist(vUv, vec2(0.5, 0.5));
    else if (face == 2) d = min(pipDist(vUv, vec2(0.27, 0.27)), pipDist(vUv, vec2(0.73, 0.73)));
    else if (face == 3) d = min(pipDist(vUv, vec2(0.26, 0.26)), min(pipDist(vUv, vec2(0.5, 0.5)), pipDist(vUv, vec2(0.74, 0.74))));
    else if (face == 4) d = min(min(pipDist(vUv, vec2(0.27, 0.27)), pipDist(vUv, vec2(0.73, 0.27))), min(pipDist(vUv, vec2(0.27, 0.73)), pipDist(vUv, vec2(0.73, 0.73))));
    else if (face == 5) d = min(min(min(pipDist(vUv, vec2(0.26, 0.26)), pipDist(vUv, vec2(0.74, 0.26))), min(pipDist(vUv, vec2(0.26, 0.74)), pipDist(vUv, vec2(0.74, 0.74)))), pipDist(vUv, vec2(0.5, 0.5)));
    else d = min(min(pipDist(vUv, vec2(0.28, 0.22)), pipDist(vUv, vec2(0.72, 0.22))), min(min(pipDist(vUv, vec2(0.28, 0.50)), pipDist(vUv, vec2(0.72, 0.50))), min(pipDist(vUv, vec2(0.28, 0.78)), pipDist(vUv, vec2(0.72, 0.78)))));

    float pip = 1.0 - smoothstep(r - 0.03, r + 0.02, d);
    vec3 base = uAccentColor * (0.45 + 0.65 * diff);
    if (uEnabled == 0 && uIsRolling == 0) base = mix(base, vec3(0.35), 0.35);
    vec3 col = clamp(mix(base, vec3(0.99, 0.97, 0.92), pip), 0.0, 1.0);
    fragColor = vec4(col, 1.0);
}
)glsl";

PasaDiceRenderer::PasaDiceRenderer() = default;

PasaDiceRenderer::~PasaDiceRenderer() {
    destroy();
}

static GLuint compileDiceShader(GLenum type, const char* src) {
    GLuint shader = glCreateShader(type);
    glShaderSource(shader, 1, &src, nullptr);
    glCompileShader(shader);
    GLint ok = 0;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &ok);
    if (!ok) {
        char log[512];
        glGetShaderInfoLog(shader, 512, nullptr, log);
        LOGE("Dice shader compile error: %s", log);
    }
    return shader;
}

static GLuint linkDiceProgram(const char* vsSrc, const char* fsSrc) {
    GLuint vs = compileDiceShader(GL_VERTEX_SHADER, vsSrc);
    GLuint fs = compileDiceShader(GL_FRAGMENT_SHADER, fsSrc);
    GLuint prog = glCreateProgram();
    glAttachShader(prog, vs);
    glAttachShader(prog, fs);
    glLinkProgram(prog);
    glDeleteShader(vs);
    glDeleteShader(fs);
    return prog;
}

static void cacheDiceUniforms(DiceProgramUniforms& target) {
    target.uViewProj = glGetUniformLocation(target.program, "uViewProj");
    target.uModel = glGetUniformLocation(target.program, "uModel");
    target.uAccentColor = glGetUniformLocation(target.program, "uAccentColor");
    target.uIsRolling = glGetUniformLocation(target.program, "uIsRolling");
    target.uEnabled = glGetUniformLocation(target.program, "uEnabled");
    target.uTime = glGetUniformLocation(target.program, "uTime");
}

void PasaDiceRenderer::cleanupShaders() {
    const bool hasCtx = hasDiceEglContext();
    if (highLodProgram_.program) {
        if (hasCtx) glDeleteProgram(highLodProgram_.program);
        highLodProgram_.program = 0;
    }
    if (lowLodProgram_.program) {
        if (hasCtx) glDeleteProgram(lowLodProgram_.program);
        lowLodProgram_.program = 0;
    }
}

void PasaDiceRenderer::setupShaders() {
    if (!hasDiceEglContext()) return;
    cleanupShaders();
    highLodProgram_.program = linkDiceProgram(kDiceHighVertexShaderSrc, kDiceHighFragmentShaderSrc);
    cacheDiceUniforms(highLodProgram_);
    lowLodProgram_.program = linkDiceProgram(kDiceLowVertexShaderSrc, kDiceLowFragmentShaderSrc);
    cacheDiceUniforms(lowLodProgram_);
}

void PasaDiceRenderer::cleanupMesh() {
    const bool hasCtx = hasDiceEglContext();
    if (cubeVao_) {
        if (hasCtx) glDeleteVertexArrays(1, &cubeVao_);
        cubeVao_ = 0;
    }
    if (cubeVbo_) {
        if (hasCtx) glDeleteBuffers(1, &cubeVbo_);
        cubeVbo_ = 0;
    }
    if (cubeEbo_) {
        if (hasCtx) glDeleteBuffers(1, &cubeEbo_);
        cubeEbo_ = 0;
    }
}

// Smoothly rounded bevelled cube via squircle / superellipsoid corner rounding
static void generateRoundedDiceMesh(
    int subdiv,
    std::vector<float>& vertices,
    std::vector<unsigned int>& indices
) {
    vertices.clear();
    indices.clear();

    struct FaceSpec {
        float faceId;
        float nx, ny, nz;
        float ux, uy, uz;
        float vx, vy, vz;
    };

    // Standard opposing faces (1 opposite 6, 2 opposite 5, 3 opposite 4)
    const FaceSpec faces[6] = {
        {1.0f,  0,  0,  1,   1,  0,  0,   0,  1,  0}, // +Z
        {6.0f,  0,  0, -1,  -1,  0,  0,   0,  1,  0}, // -Z
        {2.0f,  0,  1,  0,   1,  0,  0,   0,  0, -1}, // +Y
        {5.0f,  0, -1,  0,   1,  0,  0,   0,  0,  1}, // -Y
        {3.0f,  1,  0,  0,   0,  0, -1,   0,  1,  0}, // +X
        {4.0f, -1,  0,  0,   0,  0,  1,   0,  1,  0}, // -X
    };

    const float halfSize = 0.72f;
    const float roundness = 0.16f;

    for (int f = 0; f < 6; ++f) {
        const auto& spec = faces[f];
        unsigned int baseIdx = static_cast<unsigned int>(vertices.size() / 9);

        for (int iy = 0; iy <= subdiv; ++iy) {
            float tv = static_cast<float>(iy) / static_cast<float>(subdiv);
            float pv = (tv * 2.0f - 1.0f);
            for (int ix = 0; ix <= subdiv; ++ix) {
                float tu = static_cast<float>(ix) / static_cast<float>(subdiv);
                float pu = (tu * 2.0f - 1.0f);

                // Cube point in [-1, 1]^3
                float cx = spec.nx + spec.ux * pu + spec.vx * pv;
                float cy = spec.ny + spec.uy * pu + spec.vy * pv;
                float cz = spec.nz + spec.uz * pu + spec.vz * pv;

                // Smoothly blend between cube surface and sphere for bevelled corners
                float len = sqrtf(cx * cx + cy * cy + cz * cz);
                float sx = cx / len;
                float sy = cy / len;
                float sz = cz / len;

                // Clamp inner cube core & blend spherical bevel at edges
                float inner = 1.0f - roundness;
                float bx = std::clamp(cx, -inner, inner);
                float by = std::clamp(cy, -inner, inner);
                float bz = std::clamp(cz, -inner, inner);
                float dx = cx - bx;
                float dy = cy - by;
                float dz = cz - bz;
                float dLen = sqrtf(dx * dx + dy * dy + dz * dz);

                float px, py, pz, nx, ny, nz;
                if (dLen > 0.0001f) {
                    nx = dx / dLen;
                    ny = dy / dLen;
                    nz = dz / dLen;
                    px = (bx + nx * roundness) * halfSize;
                    py = (by + ny * roundness) * halfSize;
                    pz = (bz + nz * roundness) * halfSize;
                } else {
                    nx = spec.nx;
                    ny = spec.ny;
                    nz = spec.nz;
                    px = cx * halfSize;
                    py = cy * halfSize;
                    pz = cz * halfSize;
                }

                // Slight spherical smoothing on normal for luscious edge highlights
                nx = nx * 0.88f + sx * 0.12f;
                ny = ny * 0.88f + sy * 0.12f;
                nz = nz * 0.88f + sz * 0.12f;
                float nLen = sqrtf(nx * nx + ny * ny + nz * nz);
                nx /= nLen; ny /= nLen; nz /= nLen;

                vertices.push_back(px);
                vertices.push_back(py);
                vertices.push_back(pz);
                vertices.push_back(nx);
                vertices.push_back(ny);
                vertices.push_back(nz);
                vertices.push_back(tu);
                vertices.push_back(tv);
                vertices.push_back(spec.faceId);
            }
        }

        int rowStride = subdiv + 1;
        for (int iy = 0; iy < subdiv; ++iy) {
            for (int ix = 0; ix < subdiv; ++ix) {
                unsigned int i0 = baseIdx + iy * rowStride + ix;
                unsigned int i1 = i0 + 1;
                unsigned int i2 = i0 + rowStride;
                unsigned int i3 = i2 + 1;

                indices.push_back(i0);
                indices.push_back(i1);
                indices.push_back(i2);

                indices.push_back(i2);
                indices.push_back(i1);
                indices.push_back(i3);
            }
        }
    }
}

void PasaDiceRenderer::setupMesh(bool lowLod) {
    if (!hasDiceEglContext()) return;
    cleanupMesh();

    int subdiv = lowLod ? 6 : 14;
    std::vector<float> verts;
    std::vector<unsigned int> inds;
    generateRoundedDiceMesh(subdiv, verts, inds);
    cubeIndexCount_ = static_cast<int>(inds.size());

    glGenVertexArrays(1, &cubeVao_);
    glGenBuffers(1, &cubeVbo_);
    glGenBuffers(1, &cubeEbo_);

    glBindVertexArray(cubeVao_);
    glBindBuffer(GL_ARRAY_BUFFER, cubeVbo_);
    glBufferData(GL_ARRAY_BUFFER, verts.size() * sizeof(float), verts.data(), GL_STATIC_DRAW);
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, cubeEbo_);
    glBufferData(GL_ELEMENT_ARRAY_BUFFER, inds.size() * sizeof(unsigned int), inds.data(), GL_STATIC_DRAW);

    const GLsizei stride = 9 * sizeof(float);
    glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, stride, (void*)0);
    glEnableVertexAttribArray(0);
    glVertexAttribPointer(1, 3, GL_FLOAT, GL_FALSE, stride, (void*)(3 * sizeof(float)));
    glEnableVertexAttribArray(1);
    glVertexAttribPointer(2, 2, GL_FLOAT, GL_FALSE, stride, (void*)(6 * sizeof(float)));
    glEnableVertexAttribArray(2);
    glVertexAttribPointer(3, 1, GL_FLOAT, GL_FALSE, stride, (void*)(8 * sizeof(float)));
    glEnableVertexAttribArray(3);

    glBindVertexArray(0);
}

void PasaDiceRenderer::init(int width, int height) {
    if (!hasDiceDrawableContext()) {
        initialized_ = false;
        return;
    }
    setupShaders();

    const char* rendererStr = reinterpret_cast<const char*>(glGetString(GL_RENDERER));
    std::string gpuName = rendererStr ? rendererStr : "";
    bool isSoftwareGpu =
        (gpuName.find("SwiftShader") != std::string::npos) ||
        (gpuName.find("llvmpipe") != std::string::npos) ||
        (gpuName.find("Android Emulator") != std::string::npos);

    lodController_.reset(isSoftwareGpu);
    setupMesh(lodController_.isLowLod());

    glEnable(GL_DEPTH_TEST);
    glEnable(GL_CULL_FACE);
    glCullFace(GL_BACK);
    glDisable(GL_BLEND);

    initialized_ = true;
    resize(width, height);
}

void PasaDiceRenderer::resize(int width, int height) {
    if (!initialized_ || !hasDiceDrawableContext()) return;
    if (width > 0 && height > 0) {
        glViewport(0, 0, width, height);
        aspect_ = std::max(0.01f, static_cast<float>(width) / static_cast<float>(height));
    }
}

static void mulMat4(float* result, const float* a, const float* b) {
    float out[16];
    for (int col = 0; col < 4; ++col) {
        for (int row = 0; row < 4; ++row) {
            out[col * 4 + row] =
                a[0 * 4 + row] * b[col * 4 + 0] +
                a[1 * 4 + row] * b[col * 4 + 1] +
                a[2 * 4 + row] * b[col * 4 + 2] +
                a[3 * 4 + row] * b[col * 4 + 3];
        }
    }
    for (int i = 0; i < 16; ++i) result[i] = out[i];
}

static void idMat4(float* m) {
    for (int i = 0; i < 16; ++i) m[i] = 0.0f;
    m[0] = m[5] = m[10] = m[15] = 1.0f;
}

static void perspectiveMat4(float* m, float fovYRad, float aspect, float nearZ, float farZ) {
    idMat4(m);
    float f = 1.0f / tanf(fovYRad * 0.5f);
    m[0] = f / aspect;
    m[5] = f;
    m[10] = (farZ + nearZ) / (nearZ - farZ);
    m[11] = -1.0f;
    m[14] = (2.0f * farZ * nearZ) / (nearZ - farZ);
    m[15] = 0.0f;
}

static void eulerXYZMat4(float* mat, float rx, float ry, float rz) {
    float cx = cosf(rx), sx = sinf(rx);
    float cy = cosf(ry), sy = sinf(ry);
    float cz = cosf(rz), sz = sinf(rz);

    float mRx[16], mRy[16], mRz[16], tmp[16];
    idMat4(mRx);
    mRx[5] = cx;  mRx[9] = -sx;
    mRx[6] = sx;  mRx[10] = cx;

    idMat4(mRy);
    mRy[0] = cy;  mRy[8] = sy;
    mRy[2] = -sy; mRy[10] = cy;

    idMat4(mRz);
    mRz[0] = cz;  mRz[4] = -sz;
    mRz[1] = sz;  mRz[5] = cz;

    mulMat4(tmp, mRz, mRy);
    mulMat4(mat, tmp, mRx);
}

static void targetAnglesForFace(int face, float& outRx, float& outRy, float& outRz) {
    const float halfPi = static_cast<float>(M_PI) * 0.5f;
    const float pi = static_cast<float>(M_PI);
    switch (face) {
        case 1: outRx = 0.0f;    outRy = 0.0f;    outRz = 0.0f; break;
        case 2: outRx = halfPi;  outRy = 0.0f;    outRz = 0.0f; break;
        case 3: outRx = 0.0f;    outRy = -halfPi; outRz = 0.0f; break;
        case 4: outRx = 0.0f;    outRy = halfPi;  outRz = 0.0f; break;
        case 5: outRx = -halfPi; outRy = 0.0f;    outRz = 0.0f; break;
        default: outRx = 0.0f;   outRy = pi;      outRz = 0.0f; break; // 6
    }
    // Slight 3D showcase tilt so rounded bevelled sides are subtly visible at rest
    outRx += 0.14f;
    outRy -= 0.16f;
}

static float wrapNearestTarget(float current, float target) {
    const float twoPi = static_cast<float>(M_PI) * 2.0f;
    float k = roundf((current - target) / twoPi);
    return target + k * twoPi;
}

void PasaDiceRenderer::render(float dt, int diceValue, bool isRolling, int accentColorHex, bool enabled) {
    if (!initialized_ || !hasDiceDrawableContext()) return;

    totalTime_ += dt;

    float r = ((accentColorHex >> 16) & 0xFF) / 255.0f;
    float g = ((accentColorHex >> 8) & 0xFF) / 255.0f;
    float b = (accentColorHex & 0xFF) / 255.0f;

    if (isRolling) {
        if (!wasRolling_) {
            velX_ = 13.5f;
            velY_ = 17.2f;
            velZ_ = 8.4f;
        }
        rotX_ += velX_ * dt;
        rotY_ += velY_ * dt;
        rotZ_ += velZ_ * dt;
        rollLift_ = std::min(1.0f, rollLift_ + dt * 6.5f);
        settleBounce_ = 0.0f;
        wasRolling_ = true;
    } else {
        if (wasRolling_ || diceValue != lastTargetValue_) {
            settleBounce_ = 1.0f;
            lastTargetValue_ = diceValue;
        }
        wasRolling_ = false;
        rollLift_ = std::max(0.0f, rollLift_ - dt * 6.0f);
        settleBounce_ = std::max(0.0f, settleBounce_ - dt * 3.8f);

        float tx = 0.0f, ty = 0.0f, tz = 0.0f;
        targetAnglesForFace(diceValue, tx, ty, tz);
        float wTx = wrapNearestTarget(rotX_, tx);
        float wTy = wrapNearestTarget(rotY_, ty);
        float wTz = wrapNearestTarget(rotZ_, tz);

        float lerpRate = 1.0f - expf(-14.0f * dt);
        rotX_ += (wTx - rotX_) * lerpRate;
        rotY_ += (wTy - rotY_) * lerpRate;
        rotZ_ += (wTz - rotZ_) * lerpRate;
    }

    glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

    float proj[16], view[16], viewProj[16];
    perspectiveMat4(proj, 0.72f, aspect_, 0.1f, 20.0f);
    idMat4(view);
    // Smooth hover & camera zoom out during roll so bevelled corners stay framed cleanly
    float camZ = -3.55f - rollLift_ * 0.55f;
    view[14] = camZ;
    mulMat4(viewProj, proj, view);

    float bounceOffsetY = isRolling
        ? (sinf(totalTime_ * 14.0f) * 0.12f)
        : (sinf(settleBounce_ * static_cast<float>(M_PI) * 2.0f) * settleBounce_ * 0.08f);
    float scalePulse = 1.0f + rollLift_ * 0.08f + settleBounce_ * 0.06f;

    float rotMat[16], transMat[16], scaleMat[16], modelMat[16];
    eulerXYZMat4(rotMat, rotX_, rotY_, rotZ_);
    idMat4(transMat);
    transMat[13] = bounceOffsetY;
    idMat4(scaleMat);
    scaleMat[0] = scalePulse;
    scaleMat[5] = scalePulse;
    scaleMat[10] = scalePulse;

    float tmp[16];
    mulMat4(tmp, transMat, rotMat);
    mulMat4(modelMat, tmp, scaleMat);

    const DiceProgramUniforms& prog = lodController_.isLowLod() ? lowLodProgram_ : highLodProgram_;
    glUseProgram(prog.program);
    glUniformMatrix4fv(prog.uViewProj, 1, GL_FALSE, viewProj);
    glUniformMatrix4fv(prog.uModel, 1, GL_FALSE, modelMat);
    glUniform3f(prog.uAccentColor, r, g, b);
    glUniform1i(prog.uIsRolling, isRolling ? 1 : 0);
    glUniform1i(prog.uEnabled, enabled ? 1 : 0);
    if (prog.uTime >= 0) {
        glUniform1f(prog.uTime, totalTime_);
    }

    glBindVertexArray(cubeVao_);
    glDrawElements(GL_TRIANGLES, cubeIndexCount_, GL_UNSIGNED_INT, 0);
    glBindVertexArray(0);

    if (lodController_.recordFrameTime(dt * 1000.0f)) {
        setupMesh(lodController_.isLowLod());
    }
}

void PasaDiceRenderer::destroy() {
    cleanupMesh();
    cleanupShaders();
    initialized_ = false;
}
