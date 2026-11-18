#include "cosmium_renderer.h"
#include <EGL/egl.h>
#include <GLES3/gl3.h>
#include <android/log.h>
#include <cmath>
#include <cstdlib>
#include <algorithm>

static inline bool hasCurrentEglContext() {
    return eglGetCurrentContext() != EGL_NO_CONTEXT;
}

static inline bool hasDrawableEglContext() {
    return eglGetCurrentContext() != EGL_NO_CONTEXT &&
           eglGetCurrentSurface(EGL_DRAW) != EGL_NO_SURFACE;
}

#define LOG_TAG "CosmiumNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// --- HIGH-DENSITY SHADERS (Smooth Spherical Lava-Lamp Oil Blob + Internal Plasma Convection) ---
static const char* kHighLodVertexShaderSrc = R"glsl(#version 300 es
precision highp float;
precision highp int;
layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;

uniform mat4 uViewProj;
uniform mat4 uModel;
uniform float uTime;
uniform float uInteractionIntensity;
uniform float uMoodTurbulence;
uniform highp int uIsThinking;
uniform highp int uIsOuterLayer;

out vec3 vNormal;
out vec3 vLocalPos;
out float vDistortionFactor;

// Smooth low-frequency spherical-harmonic lava-lamp oil-drop displacement
float oilBlobField(vec3 p, float t, float speechBoost) {
    float w1 = sin(p.x * 1.45 + t * 1.15) * cos(p.y * 1.35 - t * 0.95);
    float w2 = sin(p.y * 1.95 + p.z * 1.55 + t * 1.45) * 0.65;
    float w3 = cos(p.z * 2.25 - p.x * 1.65 + t * 1.85) * 0.42;
    float cadence = sin(p.y * 2.6 + t * 4.8) * (0.35 * speechBoost);
    return (w1 + w2 + w3 + cadence) * 0.14;
}

void main() {
    vec3 dir = normalize(aPosition);
    float speedScale = uIsOuterLayer == 1 ? (0.85 + 0.35 * uInteractionIntensity) : 1.0;
    float t = uTime * speedScale;

    float speechBoost = clamp(uInteractionIntensity + uMoodTurbulence * 0.85, 0.0, 1.5);
    float amp = uIsOuterLayer == 1
        ? (0.55 + 0.30 * uInteractionIntensity)
        : (0.62 + 0.32 * uInteractionIntensity + 0.38 * uMoodTurbulence);
    if (uIsThinking == 1 && uIsOuterLayer == 0) {
        amp += 0.18 * (0.5 + 0.5 * sin(uTime * 3.8));
    }

    float disp = oilBlobField(dir, t, speechBoost) * amp;
    vec3 displaced = aPosition + dir * disp;

    // Finite-difference tangent perturbation so specular highlights glide smoothly over lobes
    vec3 upHint = abs(dir.y) < 0.9 ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0);
    vec3 tangent1 = normalize(cross(upHint, dir));
    vec3 tangent2 = cross(dir, tangent1);
    float eps = 0.06;
    float d1 = oilBlobField(normalize(dir + tangent1 * eps), t, speechBoost) * amp;
    float d2 = oilBlobField(normalize(dir + tangent2 * eps), t, speechBoost) * amp;
    vec3 p1 = (dir + tangent1 * eps) * (length(aPosition) + d1);
    vec3 p2 = (dir + tangent2 * eps) * (length(aPosition) + d2);
    vec3 smoothLocalNormal = normalize(mix(aNormal, normalize(cross(p1 - displaced, p2 - displaced)), 0.72));

    vDistortionFactor = disp;
    vec3 worldNormal = mat3(uModel) * smoothLocalNormal;
    float nLen = length(worldNormal);
    vNormal = nLen > 0.0001 ? (worldNormal / nLen) : vec3(0.0, 0.0, 1.0);
    vLocalPos = displaced;

    gl_Position = uViewProj * uModel * vec4(displaced, 1.0);
}
)glsl";

static const char* kHighLodFragmentShaderSrc = R"glsl(#version 300 es
precision highp float;
precision highp int;

in vec3 vNormal;
in vec3 vLocalPos;
in float vDistortionFactor;

uniform vec3 uBaseColor;
uniform float uEmissiveIntensity;
uniform float uOpacity;
uniform highp int uIsThinking;
uniform highp int uIsOuterLayer;
uniform float uTime;

out vec4 fragColor;

vec3 acesFilmicToneMap(vec3 x) {
    const float a = 2.51;
    const float b = 0.03;
    const float c = 2.43;
    const float d = 0.59;
    const float e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}

void main() {
    vec3 normal = normalize(vNormal);
    float ndotv = clamp(abs(dot(normal, vec3(0.0, 0.0, 1.0))), 0.0, 1.0);

    // Soft liquid-oil Fresnel rim & top-left key specular sheen
    float fresnel = pow(1.0 - ndotv, 2.35);
    vec3 lightDir = normalize(vec3(-0.42, 0.58, 0.70));
    vec3 halfVec = normalize(lightDir + vec3(0.0, 0.0, 1.0));
    float specSheen = pow(max(dot(normal, halfVec), 0.0), 16.0);

    // Internal plasma convection eddies (smooth lava-lamp currents)
    float flow1 = 0.5 + 0.5 * sin(vLocalPos.x * 2.1 + vLocalPos.y * 1.9 + uTime * 1.15 + vDistortionFactor * 2.2);
    float flow2 = 0.5 + 0.5 * cos(vLocalPos.y * 2.4 - vLocalPos.z * 2.0 + uTime * 0.9);
    float plasmaSwirl = mix(flow1, flow2, 0.45);

    vec3 creamCore = vec3(1.0, 0.975, 0.82);
    vec3 deepAmber = uBaseColor * vec3(0.88, 0.72, 0.34);

    vec3 bodyTone = mix(deepAmber, uBaseColor, 0.42 + 0.58 * ndotv);
    bodyTone = mix(bodyTone, creamCore, plasmaSwirl * (uIsThinking == 1 ? 0.38 : 0.26) + pow(ndotv, 2.2) * 0.35);

    vec3 glow = bodyTone * (0.78 + 0.10 * uEmissiveIntensity)
              + creamCore * specSheen * 0.34
              + mix(uBaseColor, creamCore, 0.5) * fresnel * 0.42;

    vec3 finalColor = acesFilmicToneMap(glow);

    float alpha = uOpacity;
    if (uIsOuterLayer == 1) {
        float softEnvelope = clamp(0.18 + fresnel * 0.65 + abs(vDistortionFactor) * 0.45, 0.0, 1.0);
        alpha = clamp(uOpacity * softEnvelope, 0.0, 0.28);
    } else {
        // Soft anti-aliased liquid edge falloff
        float edgeFeather = smoothstep(0.0, 0.16, ndotv);
        alpha = clamp(uOpacity * (0.72 + 0.28 * edgeFeather), 0.0, 0.98);
    }

    fragColor = vec4(finalColor * alpha, alpha);
}
)glsl";

// --- LOW-DENSITY SHADERS (Mediump smooth oil-blob path for >33ms or emulator GPUs) ---
static const char* kLowLodVertexShaderSrc = R"glsl(#version 300 es
precision mediump float;
precision mediump int;
layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;

uniform mat4 uViewProj;
uniform mat4 uModel;
uniform float uTime;
uniform float uInteractionIntensity;
uniform float uMoodTurbulence;
uniform mediump int uIsThinking;
uniform mediump int uIsOuterLayer;

out vec3 vNormal;
out float vIridescence;
out float vDistortionFactor;

void main() {
    vec3 dir = normalize(aPosition);
    float speedScale = uIsOuterLayer == 1 ? (0.85 + 0.35 * uInteractionIntensity) : 1.0;
    float t = uTime * speedScale;

    float w1 = sin(dir.x * 1.45 + t * 1.15) * cos(dir.y * 1.35 - t * 0.95);
    float w2 = cos(dir.z * 2.0 - dir.x * 1.4 + t * 1.65) * 0.48;
    float wave = (w1 + w2) * 0.14;

    float amp = uIsOuterLayer == 1
        ? (0.55 + 0.30 * uInteractionIntensity)
        : (0.62 + 0.32 * uInteractionIntensity + 0.38 * uMoodTurbulence);
    if (uIsThinking == 1 && uIsOuterLayer == 0) {
        amp += 0.18 * (0.5 + 0.5 * sin(uTime * 3.8));
    }

    vec3 pos = aPosition + dir * wave * amp;
    vDistortionFactor = wave * amp;
    vIridescence = 0.5 + 0.5 * sin(pos.x * 2.1 + pos.y * 1.8 + uTime * 1.1);

    vec3 worldNormal = mat3(uModel) * aNormal;
    float nLen = length(worldNormal);
    vNormal = nLen > 0.0001 ? (worldNormal / nLen) : vec3(0.0, 0.0, 1.0);

    gl_Position = uViewProj * uModel * vec4(pos, 1.0);
}
)glsl";

static const char* kLowLodFragmentShaderSrc = R"glsl(#version 300 es
precision mediump float;
precision mediump int;

in vec3 vNormal;
in float vIridescence;
in float vDistortionFactor;

uniform vec3 uBaseColor;
uniform float uEmissiveIntensity;
uniform float uOpacity;
uniform mediump int uIsThinking;
uniform mediump int uIsOuterLayer;

out vec4 fragColor;

void main() {
    vec3 n = normalize(vNormal);
    float ndotv = clamp(abs(n.z), 0.0, 1.0);
    float rim = pow(1.0 - ndotv, 2.2);
    vec3 lightDir = normalize(vec3(-0.42, 0.58, 0.70));
    float sheen = pow(max(dot(n, normalize(lightDir + vec3(0.0, 0.0, 1.0))), 0.0), 12.0);

    vec3 creamCore = vec3(1.0, 0.975, 0.82);
    vec3 col = mix(uBaseColor * vec3(0.90, 0.75, 0.38), uBaseColor, 0.45 + 0.55 * ndotv);
    col = mix(col, creamCore, vIridescence * 0.25 + ndotv * ndotv * 0.30);

    vec3 glow = col * (0.80 + 0.08 * uEmissiveIntensity) + creamCore * sheen * 0.30 + uBaseColor * rim * 0.35;
    vec3 finalColor = glow / (vec3(1.0) + glow * 0.22);

    float alpha = uOpacity;
    if (uIsOuterLayer == 1) {
        alpha = clamp(uOpacity * (0.20 + rim * 0.65), 0.0, 0.26);
    } else {
        alpha = clamp(uOpacity * (0.75 + 0.25 * smoothstep(0.0, 0.18, ndotv)), 0.0, 0.98);
    }

    fragColor = vec4(finalColor * alpha, alpha);
}
)glsl";

static const char* kUnlitVertexShaderSrc = R"glsl(#version 300 es
precision mediump float;
layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;
uniform mat4 uMVP;
out vec3 vNormal;
void main() {
    vec3 n = (uMVP * vec4(aNormal, 0.0)).xyz;
    float len = length(n);
    vNormal = len > 0.0001 ? (n / len) : vec3(0.0, 0.0, 1.0);
    gl_Position = uMVP * vec4(aPosition, 1.0);
}
)glsl";

static const char* kUnlitFragmentShaderSrc = R"glsl(#version 300 es
precision mediump float;
in vec3 vNormal;
uniform vec3 uColor;
uniform float uAlpha;
out vec4 fragColor;
void main() {
    float ndotv = abs(vNormal.z);
    float rimOnly = pow(1.0 - clamp(ndotv, 0.0, 1.0), 2.4);
    float a = clamp(uAlpha * (0.25 + 0.75 * rimOnly), 0.0, 1.0);
    vec3 c = clamp(uColor, 0.0, 1.0);
    fragColor = vec4(c * a, a);
}
)glsl";

static const char* kEmberInstancedVertexShaderSrc = R"glsl(#version 300 es
precision mediump float;
layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec4 aInstCol0;
layout(location = 3) in vec4 aInstCol1;
layout(location = 4) in vec4 aInstCol2;
layout(location = 5) in vec4 aInstCol3;

out vec3 vNormal;
out float vInstanceId;

void main() {
    mat4 mvp = mat4(aInstCol0, aInstCol1, aInstCol2, aInstCol3);
    vec3 n = (mvp * vec4(aNormal, 0.0)).xyz;
    float len = length(n);
    vNormal = len > 0.0001 ? (n / len) : vec3(0.0, 0.0, 1.0);
    vInstanceId = float(gl_InstanceID);
    gl_Position = mvp * vec4(aPosition, 1.0);
}
)glsl";

static const char* kEmberInstancedFragmentShaderSrc = R"glsl(#version 300 es
precision mediump float;
in vec3 vNormal;
in float vInstanceId;
uniform vec3 uColor;
uniform float uTime;
out vec4 fragColor;
void main() {
    float ndotv = abs(vNormal.z);
    float twinkle = 0.55 + 0.45 * sin(uTime * 3.0 + vInstanceId * 1.9);
    vec3 creamCore = vec3(1.0, 0.96, 0.74);
    vec3 col = mix(uColor, creamCore, (ndotv * ndotv) * 0.65) * twinkle;
    float a = 0.65 * twinkle;
    fragColor = vec4(col * a, a);
}
)glsl";

// --- AutoLodController Implementation ---

void AutoLodController::reset(bool startLowLod) {
    samples_.fill(startLowLod ? 34.0f : 16.6f);
    writeIndex_ = 0;
    sampleCount_ = 0;
    rollingSum_ = 0.0f;
    cooldownFrames_ = 0;
    lowLod_ = startLowLod;
}

bool AutoLodController::recordFrameTime(float frameTimeMs) {
    if (sampleCount_ < kWindowSize) {
        samples_[writeIndex_] = frameTimeMs;
        rollingSum_ += frameTimeMs;
        sampleCount_++;
    } else {
        rollingSum_ -= samples_[writeIndex_];
        samples_[writeIndex_] = frameTimeMs;
        rollingSum_ += frameTimeMs;
    }
    writeIndex_ = (writeIndex_ + 1) % kWindowSize;

    if (cooldownFrames_ > 0) {
        cooldownFrames_--;
        return false;
    }

    if (!lowLod_ && sampleCount_ >= 15) {
        float avg = rollingSum_ / static_cast<float>(sampleCount_);
        if (avg > kHighToLowThresholdMs) {
            lowLod_ = true;
            cooldownFrames_ = 60;
            LOGI("AutoLodController: avg frame time %.2f ms > 33ms -> switching to LOW-DENSITY shaders & meshes", avg);
            return true;
        }
    } else if (lowLod_ && sampleCount_ >= kWindowSize) {
        float avg = rollingSum_ / static_cast<float>(sampleCount_);
        if (avg < kLowToHighThresholdMs) {
            lowLod_ = false;
            cooldownFrames_ = 60;
            LOGI("AutoLodController: avg frame time %.2f ms < 22ms -> restoring HIGH-DENSITY shaders & meshes", avg);
            return true;
        }
    }

    return false;
}

// --- CosmiumRenderer Implementation ---

CosmiumRenderer::CosmiumRenderer() = default;

CosmiumRenderer::~CosmiumRenderer() {
    destroy();
}

static GLuint compileShader(GLenum type, const char* src) {
    GLuint shader = glCreateShader(type);
    glShaderSource(shader, 1, &src, nullptr);
    glCompileShader(shader);
    GLint success = 0;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &success);
    if (!success) {
        char infoLog[512];
        glGetShaderInfoLog(shader, 512, nullptr, infoLog);
        LOGE("Shader compilation error: %s", infoLog);
    }
    return shader;
}

static GLuint linkProgram(const char* vsSrc, const char* fsSrc) {
    GLuint vs = compileShader(GL_VERTEX_SHADER, vsSrc);
    GLuint fs = compileShader(GL_FRAGMENT_SHADER, fsSrc);
    GLuint prog = glCreateProgram();
    glAttachShader(prog, vs);
    glAttachShader(prog, fs);
    glLinkProgram(prog);
    GLint linked = 0;
    glGetProgramiv(prog, GL_LINK_STATUS, &linked);
    if (!linked) {
        char infoLog[512];
        glGetProgramInfoLog(prog, 512, nullptr, infoLog);
        LOGE("Program link error: %s", infoLog);
    }
    glDeleteShader(vs);
    glDeleteShader(fs);
    return prog;
}

static void cacheFluidUniforms(FluidProgramUniforms& target) {
    target.uViewProj = glGetUniformLocation(target.program, "uViewProj");
    target.uModel = glGetUniformLocation(target.program, "uModel");
    target.uTime = glGetUniformLocation(target.program, "uTime");
    target.uInteraction = glGetUniformLocation(target.program, "uInteractionIntensity");
    target.uMood = glGetUniformLocation(target.program, "uMoodTurbulence");
    target.uIsThinking = glGetUniformLocation(target.program, "uIsThinking");
    target.uIsOuter = glGetUniformLocation(target.program, "uIsOuterLayer");
    target.uBaseColor = glGetUniformLocation(target.program, "uBaseColor");
    target.uEmissive = glGetUniformLocation(target.program, "uEmissiveIntensity");
    target.uOpacity = glGetUniformLocation(target.program, "uOpacity");
}

void CosmiumRenderer::cleanupShaders() {
    const bool hasCtx = hasCurrentEglContext();
    if (highLodProgram_.program) {
        if (hasCtx) glDeleteProgram(highLodProgram_.program);
        highLodProgram_.program = 0;
    }
    if (lowLodProgram_.program) {
        if (hasCtx) glDeleteProgram(lowLodProgram_.program);
        lowLodProgram_.program = 0;
    }
    if (unlitShaderProgram_) {
        if (hasCtx) glDeleteProgram(unlitShaderProgram_);
        unlitShaderProgram_ = 0;
    }
    if (emberInstancedProgram_) {
        if (hasCtx) glDeleteProgram(emberInstancedProgram_);
        emberInstancedProgram_ = 0;
    }
}

void CosmiumRenderer::setupShaders() {
    if (!hasCurrentEglContext()) return;
    cleanupShaders();

    highLodProgram_.program = linkProgram(kHighLodVertexShaderSrc, kHighLodFragmentShaderSrc);
    cacheFluidUniforms(highLodProgram_);

    lowLodProgram_.program = linkProgram(kLowLodVertexShaderSrc, kLowLodFragmentShaderSrc);
    cacheFluidUniforms(lowLodProgram_);

    unlitShaderProgram_ = linkProgram(kUnlitVertexShaderSrc, kUnlitFragmentShaderSrc);
    uUnlitMvpLoc_ = glGetUniformLocation(unlitShaderProgram_, "uMVP");
    uUnlitColorLoc_ = glGetUniformLocation(unlitShaderProgram_, "uColor");
    uUnlitAlphaLoc_ = glGetUniformLocation(unlitShaderProgram_, "uAlpha");

    emberInstancedProgram_ = linkProgram(kEmberInstancedVertexShaderSrc, kEmberInstancedFragmentShaderSrc);
    uEmberColorLoc_ = glGetUniformLocation(emberInstancedProgram_, "uColor");
    uEmberTimeLoc_ = glGetUniformLocation(emberInstancedProgram_, "uTime");
}

static void generateSphereMesh(
    float radius,
    int lats,
    int longs,
    std::vector<float>& vertices,
    std::vector<unsigned int>& indices
) {
    vertices.clear();
    indices.clear();
    vertices.reserve((lats + 1) * (longs + 1) * 6);
    indices.reserve(lats * longs * 6);

    for (int i = 0; i <= lats; ++i) {
        float theta = static_cast<float>(i) * static_cast<float>(M_PI) / static_cast<float>(lats);
        float sinTheta = sinf(theta);
        float cosTheta = cosf(theta);

        for (int j = 0; j <= longs; ++j) {
            float phi = static_cast<float>(j) * 2.0f * static_cast<float>(M_PI) / static_cast<float>(longs);
            float sinPhi = sinf(phi);
            float cosPhi = cosf(phi);

            float x = cosPhi * sinTheta;
            float y = cosTheta;
            float z = sinPhi * sinTheta;

            vertices.push_back(x * radius);
            vertices.push_back(y * radius);
            vertices.push_back(z * radius);
            vertices.push_back(x);
            vertices.push_back(y);
            vertices.push_back(z);
        }
    }

    for (int i = 0; i < lats; ++i) {
        for (int j = 0; j < longs; ++j) {
            unsigned int first = static_cast<unsigned int>(i * (longs + 1) + j);
            unsigned int second = first + static_cast<unsigned int>(longs + 1);

            indices.push_back(first);
            indices.push_back(second);
            indices.push_back(first + 1);

            indices.push_back(second);
            indices.push_back(second + 1);
            indices.push_back(first + 1);
        }
    }
}

void CosmiumRenderer::cleanupMeshes() {
    const bool hasCtx = hasCurrentEglContext();
    auto deleteMesh = [hasCtx](GLuint& vao, GLuint& vbo, GLuint& ebo) {
        if (vao) {
            if (hasCtx) glDeleteVertexArrays(1, &vao);
            vao = 0;
        }
        if (vbo) {
            if (hasCtx) glDeleteBuffers(1, &vbo);
            vbo = 0;
        }
        if (ebo) {
            if (hasCtx) glDeleteBuffers(1, &ebo);
            ebo = 0;
        }
    };
    deleteMesh(mainSphereVao_, mainSphereVbo_, mainSphereEbo_);
    deleteMesh(outerLayerVao_, outerLayerVbo_, outerLayerEbo_);
    deleteMesh(coreVao_, coreVbo_, coreEbo_);
    deleteMesh(emberVao_, emberVbo_, emberEbo_);
    if (emberInstanceVbo_) {
        if (hasCtx) glDeleteBuffers(1, &emberInstanceVbo_);
        emberInstanceVbo_ = 0;
    }
    deleteMesh(ringVao_, ringVbo_, ringEbo_);
}

void CosmiumRenderer::setupMeshes(bool lowLod) {
    if (!hasCurrentEglContext()) return;
    cleanupMeshes();

    int res = lowLod ? 56 : 128;
    int outerRes = lowLod ? 40 : 72;

    std::vector<float> verts;
    std::vector<unsigned int> inds;

    auto uploadMesh = [&](GLuint& vao, GLuint& vbo, GLuint& ebo, int& indexCount) {
        indexCount = static_cast<int>(inds.size());
        glGenVertexArrays(1, &vao);
        glGenBuffers(1, &vbo);
        glGenBuffers(1, &ebo);
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, verts.size() * sizeof(float), verts.data(), GL_STATIC_DRAW);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, inds.size() * sizeof(unsigned int), inds.data(), GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, 6 * sizeof(float), (void*)0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, GL_FALSE, 6 * sizeof(float), (void*)(3 * sizeof(float)));
        glEnableVertexAttribArray(1);
    };

    // 1. Main Oil-Blob Plasma Body (radius 1.05)
    generateSphereMesh(1.05f, res, res, verts, inds);
    uploadMesh(mainSphereVao_, mainSphereVbo_, mainSphereEbo_, mainSphereIndexCount_);

    // 2. Outer Translucent Plasma Membrane (radius 1.18)
    generateSphereMesh(1.18f, outerRes, outerRes, verts, inds);
    uploadMesh(outerLayerVao_, outerLayerVbo_, outerLayerEbo_, outerLayerIndexCount_);

    // 3. Soft Internal Convection Pocket (radius 0.52)
    generateSphereMesh(0.52f, 28, 28, verts, inds);
    uploadMesh(coreVao_, coreVbo_, coreEbo_, coreIndexCount_);

    // 4. Subtle Micro Plasma Motes (GPU Instanced)
    int emberCount = lowLod ? 4 : 6;
    embers_.clear();
    emberInstanceMatrices_.assign(emberCount * 16, 0.0f);
    float invRandMax = 1.0f / static_cast<float>(RAND_MAX);
    for (int i = 0; i < emberCount; ++i) {
        EmberParticle emb;
        emb.baseX = (static_cast<float>(rand()) * invRandMax - 0.5f) * 1.4f;
        emb.baseY = (static_cast<float>(rand()) * invRandMax - 0.5f) * 0.9f;
        emb.baseZ = (static_cast<float>(rand()) * invRandMax - 0.5f) * 1.4f;
        emb.radius = 1.25f + static_cast<float>(rand()) * invRandMax * 0.35f;
        emb.speed = 0.45f + static_cast<float>(rand()) * invRandMax * 0.55f;
        emb.angleOffset = static_cast<float>(rand()) * invRandMax * 2.0f * static_cast<float>(M_PI);
        emb.size = 0.018f + static_cast<float>(rand()) * invRandMax * 0.02f;
        embers_.push_back(emb);
    }
    generateSphereMesh(1.0f, 12, 12, verts, inds);
    uploadMesh(emberVao_, emberVbo_, emberEbo_, emberIndexCount_);

    glGenBuffers(1, &emberInstanceVbo_);
    glBindBuffer(GL_ARRAY_BUFFER, emberInstanceVbo_);
    glBufferData(GL_ARRAY_BUFFER, emberInstanceMatrices_.size() * sizeof(float), nullptr, GL_STREAM_DRAW);
    for (int col = 0; col < 4; ++col) {
        GLuint loc = static_cast<GLuint>(2 + col);
        glEnableVertexAttribArray(loc);
        glVertexAttribPointer(loc, 4, GL_FLOAT, GL_FALSE, 16 * sizeof(float), (void*)(col * 4 * sizeof(float)));
        glVertexAttribDivisor(loc, 1);
    }

    // 5. Soft Speaking Halo Pulse Shell (radius 1.02)
    generateSphereMesh(1.02f, 28, 28, verts, inds);
    uploadMesh(ringVao_, ringVbo_, ringEbo_, ringIndexCount_);

    glBindVertexArray(0);
}

void CosmiumRenderer::init(int width, int height) {
    if (!hasDrawableEglContext()) {
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
    setupMeshes(lodController_.isLowLod());

    glEnable(GL_DEPTH_TEST);
    glEnable(GL_BLEND);
    glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
    initialized_ = true;
    resize(width, height);
}

void CosmiumRenderer::resize(int width, int height) {
    if (!initialized_ || !hasDrawableEglContext()) return;
    if (width > 0 && height > 0) {
        glViewport(0, 0, width, height);
        aspect_ = std::max(0.01f, static_cast<float>(width) / static_cast<float>(height));
    }
}

static void multiplyMatrix(float* result, const float* a, const float* b) {
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

static void identityMatrix(float* mat) {
    for (int i = 0; i < 16; ++i) mat[i] = 0.0f;
    mat[0] = mat[5] = mat[10] = mat[15] = 1.0f;
}

static void translationMatrix(float* mat, float x, float y, float z) {
    identityMatrix(mat);
    mat[12] = x;
    mat[13] = y;
    mat[14] = z;
}

static void scaleMatrix(float* mat, float x, float y, float z) {
    identityMatrix(mat);
    mat[0] = x;
    mat[5] = y;
    mat[10] = z;
}

static void rotationEulerXYZ(float* mat, float rx, float ry, float rz) {
    float cx = cosf(rx), sx = sinf(rx);
    float cy = cosf(ry), sy = sinf(ry);
    float cz = cosf(rz), sz = sinf(rz);

    float mRx[16], mRy[16], mRz[16], tmp[16];
    identityMatrix(mRx);
    mRx[5] = cx;  mRx[9] = -sx;
    mRx[6] = sx;  mRx[10] = cx;

    identityMatrix(mRy);
    mRy[0] = cy;  mRy[8] = sy;
    mRy[2] = -sy; mRy[10] = cy;

    identityMatrix(mRz);
    mRz[0] = cz;  mRz[4] = -sz;
    mRz[1] = sz;  mRz[5] = cz;

    multiplyMatrix(tmp, mRz, mRy);
    multiplyMatrix(mat, tmp, mRx);
}

static void orthoMatrix(float* m, float left, float right, float bottom, float top, float nearVal, float farVal) {
    identityMatrix(m);
    m[0] = 2.0f / (right - left);
    m[5] = 2.0f / (top - bottom);
    m[10] = -2.0f / (farVal - nearVal);
    m[12] = -(right + left) / (right - left);
    m[13] = -(top + bottom) / (top - bottom);
    m[14] = -(farVal + nearVal) / (farVal - nearVal);
}

void CosmiumRenderer::render(float dt, const std::string& state, float interactionIntensity, float moodTurbulence, int colorHex) {
    if (!initialized_ || !hasDrawableEglContext()) return;

    totalTime_ += dt;
    float time = totalTime_;

    bool isSpeaking = (state == "speaking");
    bool isThinking = (state == "thinking" || state == "processing");
    bool isCurious = (state == "curious" || state == "listening");
    bool isCelebrate = (state == "celebrate" || state == "correct");
    bool isAlert = (state == "alert");
    bool isDormant = (state == "dormant");

    // Default luminous lava-lamp golden yellow (#FFE066) unless caller overrides
    float r = ((colorHex >> 16) & 0xFF) / 255.0f;
    float g = ((colorHex >> 8) & 0xFF) / 255.0f;
    float b = (colorHex & 0xFF) / 255.0f;

    if (isSpeaking) { r = 1.0f; g = 0.88f; b = 0.36f; }          // Sunlit golden plasma
    else if (isThinking) { r = 1.0f; g = 0.80f; b = 0.14f; }     // Warm amber-gold convection
    else if (isCurious) { r = 1.0f; g = 0.85f; b = 0.28f; }      // Attentive bright lemon-gold
    else if (isDormant) { r = 0.22f; g = 0.24f; b = 0.50f; }     // Calm twilight indigo
    else if (isCelebrate) { r = 0.32f; g = 0.90f; b = 0.62f; }   // Emerald-gold shimmer
    else if (isAlert) { r = 1.0f; g = 0.56f; b = 0.38f; }        // Reassuring warm coral-amber

    float motionBoost = 1.0f + (interactionIntensity * 0.9f) + (moodTurbulence * 0.7f);

    glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

    float halfExtent = 1.92f;
    float proj[16];
    if (aspect_ >= 1.0f) {
        orthoMatrix(proj, -halfExtent * aspect_, halfExtent * aspect_, -halfExtent, halfExtent, -10.0f, 10.0f);
    } else {
        orthoMatrix(proj, -halfExtent, halfExtent, -halfExtent / aspect_, halfExtent / aspect_, -10.0f, 10.0f);
    }

    // 1. Buoyant Zero-G Floating Oil-Blob Drift
    float floatAmp = isCelebrate ? 0.16f : 0.10f;
    float posX = sinf(time * 0.55f) * 0.07f;
    float posY = sinf(time * 0.78f) * floatAmp + (isCelebrate ? fabsf(sinf(time * 3.5f)) * 0.08f : 0.0f);
    float posZ = cosf(time * 0.42f) * 0.06f;
    float groupRotX = sinf(time * 0.28f) * 0.04f;
    float groupRotZ = cosf(time * 0.32f) * 0.04f;

    // 2. Volume-Preserving Gelatinous Squish & Stretch + Speech Cadence
    float squishSpeed = isSpeaking ? 5.2f : (isThinking ? 3.4f : (isCurious ? 2.6f : 1.35f));
    float speechSyllable = isSpeaking ? (0.5f + 0.5f * sinf(time * 8.4f)) * 0.045f : 0.0f;
    float squishAmp = (isSpeaking ? 0.065f : (isThinking ? 0.048f : (isCelebrate ? 0.075f : 0.032f))) + speechSyllable;
    float squishY = sinf(time * squishSpeed) * squishAmp;
    float stretchXZ = -squishY * 0.5f;

    float groupT[16], groupR[16], groupS[16], groupMat[16];
    translationMatrix(groupT, posX, posY, posZ);
    rotationEulerXYZ(groupR, groupRotX, 0.0f, groupRotZ);
    scaleMatrix(groupS, 1.0f + stretchXZ, 1.0f + squishY, 1.0f + stretchXZ);
    multiplyMatrix(groupMat, groupT, groupR);
    multiplyMatrix(groupMat, groupMat, groupS);

    // Smooth slow lava convection rotation
    mainRotY_ += dt * 0.24f * motionBoost;
    mainRotZ_ += dt * 0.16f * motionBoost;
    mainRotX_ += dt * 0.12f * motionBoost;
    outerRotY_ -= dt * 0.30f * motionBoost;
    outerRotX_ += dt * 0.18f * motionBoost;
    emberGroupRotY_ += dt * 0.32f;

    float modelMat[16], mvp[16];

    // --- TRANSLUCENT OIL-BLOB PLASMA LAYERS ---
    glDepthMask(GL_FALSE);
    glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

    const FluidProgramUniforms& fluidProg = lodController_.isLowLod() ? lowLodProgram_ : highLodProgram_;
    float effectiveInteraction = std::min(1.5f, interactionIntensity + (isSpeaking ? 0.55f : 0.0f));
    float effectiveTurbulence = std::min(1.5f, moodTurbulence + (isSpeaking ? 0.45f : (isCelebrate ? 0.50f : 0.0f)));

    // Subtle internal glowing lava-lamp convection core (buoyantly offset inside the orb)
    glUseProgram(unlitShaderProgram_);
    float corePulse = 0.52f + sinf(time * (isSpeaking ? 4.8f : 2.1f)) * 0.08f;
    float coreShiftX = sinf(time * 0.9f) * 0.12f;
    float coreShiftY = cosf(time * 1.1f) * 0.14f;
    float coreT[16], coreS[16];
    translationMatrix(coreT, coreShiftX, coreShiftY, 0.0f);
    scaleMatrix(coreS, corePulse, corePulse, corePulse);
    multiplyMatrix(modelMat, groupMat, coreT);
    multiplyMatrix(modelMat, modelMat, coreS);
    multiplyMatrix(mvp, proj, modelMat);
    glUniformMatrix4fv(uUnlitMvpLoc_, 1, GL_FALSE, mvp);
    glUniform3f(uUnlitColorLoc_, 1.0f, 0.97f, 0.80f);
    glUniform1f(uUnlitAlphaLoc_, 0.38f);
    glBindVertexArray(coreVao_);
    glDrawElements(GL_TRIANGLES, coreIndexCount_, GL_UNSIGNED_INT, 0);

    // Layer 1: Main Viscous Oil-Blob Plasma Body
    float pulseFreq = isSpeaking ? 5.2f : (isThinking ? 4.4f : (isCelebrate ? 3.8f : 1.4f));
    float pulseAmp = isSpeaking ? 0.06f : (isThinking ? 0.05f : 0.03f);
    float pulse = 1.0f + sinf(time * pulseFreq) * pulseAmp + (interactionIntensity * 0.03f);
    if (isAlert) {
        pulse *= 0.93f;
    }

    float mainR[16], mainS[16];
    rotationEulerXYZ(mainR, mainRotX_, mainRotY_, mainRotZ_);
    scaleMatrix(mainS, pulse, pulse, pulse);
    multiplyMatrix(modelMat, groupMat, mainR);
    multiplyMatrix(modelMat, modelMat, mainS);

    glUseProgram(fluidProg.program);
    glUniformMatrix4fv(fluidProg.uViewProj, 1, GL_FALSE, proj);
    glUniformMatrix4fv(fluidProg.uModel, 1, GL_FALSE, modelMat);
    glUniform1f(fluidProg.uTime, time);
    glUniform1f(fluidProg.uInteraction, effectiveInteraction);
    glUniform1f(fluidProg.uMood, effectiveTurbulence);
    glUniform1i(fluidProg.uIsThinking, isThinking ? 1 : 0);
    glUniform1i(fluidProg.uIsOuter, 0);
    glUniform3f(fluidProg.uBaseColor, r, g, b);
    glUniform1f(fluidProg.uEmissive, (isSpeaking || isThinking) ? 3.2f : (1.8f + 1.2f * interactionIntensity));
    glUniform1f(fluidProg.uOpacity, 0.94f);

    glBindVertexArray(mainSphereVao_);
    glDrawElements(GL_TRIANGLES, mainSphereIndexCount_, GL_UNSIGNED_INT, 0);

    // Layer 2: Soft Translucent Outer Plasma Envelope (lava-lamp oil aura)
    float frayPulse = 1.06f + sinf(time * 1.1f) * 0.06f + (effectiveInteraction * 0.05f);
    float outerR[16], outerS[16];
    rotationEulerXYZ(outerR, outerRotX_, outerRotY_, 0.0f);
    scaleMatrix(outerS, frayPulse, frayPulse, frayPulse);
    multiplyMatrix(modelMat, groupMat, outerR);
    multiplyMatrix(modelMat, modelMat, outerS);

    glUniformMatrix4fv(fluidProg.uModel, 1, GL_FALSE, modelMat);
    glUniform1i(fluidProg.uIsOuter, 1);
    glUniform1f(fluidProg.uOpacity, 0.24f + 0.08f * effectiveInteraction);

    glBindVertexArray(outerLayerVao_);
    glDrawElements(GL_TRIANGLES, outerLayerIndexCount_, GL_UNSIGNED_INT, 0);

    // Layer 3: Subtle Micro Plasma Motes only when Celebrate / Thinking
    if (isCelebrate || isThinking) {
        float emberGroupRotX = sinf(time * 0.45f) * 0.12f;
        float emberGroupR[16], emberGroupMat[16];
        rotationEulerXYZ(emberGroupR, emberGroupRotX, emberGroupRotY_, 0.0f);
        multiplyMatrix(emberGroupMat, groupMat, emberGroupR);

        for (size_t i = 0; i < embers_.size(); ++i) {
            const auto& emb = embers_[i];
            float angle = emb.angleOffset + time * emb.speed * 0.45f;
            float ex = cosf(angle) * emb.radius;
            float ez = sinf(angle) * emb.radius;
            float ey = emb.baseY + sinf(time * 1.8f + static_cast<float>(i)) * 0.08f;

            float emberT[16], emberS[16];
            translationMatrix(emberT, ex, ey, ez);
            scaleMatrix(emberS, emb.size, emb.size, emb.size);
            multiplyMatrix(modelMat, emberGroupMat, emberT);
            multiplyMatrix(modelMat, modelMat, emberS);
            multiplyMatrix(&emberInstanceMatrices_[i * 16], proj, modelMat);
        }

        glUseProgram(emberInstancedProgram_);
        glUniform3f(uEmberColorLoc_, r, g, b);
        glUniform1f(uEmberTimeLoc_, time);
        glBindVertexArray(emberVao_);
        glBindBuffer(GL_ARRAY_BUFFER, emberInstanceVbo_);
        GLsizeiptr instBytes = static_cast<GLsizeiptr>(emberInstanceMatrices_.size() * sizeof(float));
        glBufferData(GL_ARRAY_BUFFER, instBytes, nullptr, GL_STREAM_DRAW);
        glBufferSubData(GL_ARRAY_BUFFER, 0, instBytes, emberInstanceMatrices_.data());
        glDrawElementsInstanced(GL_TRIANGLES, emberIndexCount_, GL_UNSIGNED_INT, 0, static_cast<GLsizei>(embers_.size()));
    }

    // Layer 4: Soft Expanding Golden Sound Pulse Waves when Speaking
    if (isSpeaking) {
        glUseProgram(unlitShaderProgram_);
        glBlendFunc(GL_ONE, GL_ONE);
        glBindVertexArray(ringVao_);

        float t1 = fmodf(time * 1.6f, 1.5f) / 1.5f;
        float wave1Scale = 1.02f + t1 * 0.46f;
        float wave1Alpha = std::max(0.0f, 0.34f * (1.0f - t1));

        float t2 = fmodf(time * 1.6f + 0.75f, 1.5f) / 1.5f;
        float wave2Scale = 1.02f + t2 * 0.46f;
        float wave2Alpha = std::max(0.0f, 0.26f * (1.0f - t2));

        float ringScale1[16], ringScale2[16];
        scaleMatrix(ringScale1, wave1Scale, wave1Scale, wave1Scale);
        scaleMatrix(ringScale2, wave2Scale, wave2Scale, wave2Scale);

        multiplyMatrix(modelMat, groupMat, ringScale1);
        multiplyMatrix(mvp, proj, modelMat);
        glUniformMatrix4fv(uUnlitMvpLoc_, 1, GL_FALSE, mvp);
        glUniform3f(uUnlitColorLoc_, 1.0f, 0.90f, 0.42f);
        glUniform1f(uUnlitAlphaLoc_, wave1Alpha);
        glDrawElements(GL_TRIANGLES, ringIndexCount_, GL_UNSIGNED_INT, 0);

        multiplyMatrix(modelMat, groupMat, ringScale2);
        multiplyMatrix(mvp, proj, modelMat);
        glUniformMatrix4fv(uUnlitMvpLoc_, 1, GL_FALSE, mvp);
        glUniform3f(uUnlitColorLoc_, r, g, b);
        glUniform1f(uUnlitAlphaLoc_, wave2Alpha);
        glDrawElements(GL_TRIANGLES, ringIndexCount_, GL_UNSIGNED_INT, 0);

        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
    }

    glDepthMask(GL_TRUE);
    glBindVertexArray(0);

    if (lodController_.recordFrameTime(dt * 1000.0f)) {
        setupMeshes(lodController_.isLowLod());
    }
}

void CosmiumRenderer::destroy() {
    cleanupMeshes();
    cleanupShaders();
    initialized_ = false;
}
