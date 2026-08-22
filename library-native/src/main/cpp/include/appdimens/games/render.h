// AppDimens Games 3.0 — graphics interop (OpenGL / Vulkan / DirectX).
// SDK-independent structs mirror the native viewport types 1:1, so engines can
// memcpy them into GL calls, VkViewport / D3D11_VIEWPORT without conversion.
#pragma once

#include "core.h"
#include <cmath>

namespace appdimens::games::render {

/// Letterbox/crop result in px.
struct SurfaceRect { float x, y, w, h; };

enum class Mode : uint8_t { FitAll, FitWidth, FitHeight, Stretch, Crop };

/// Computes the design-space → surface transform for a swapchain/window size.
inline SurfaceRect viewportRect(Mode mode, float surfaceWpx, float surfaceHpx,
                                float designW, float designH) {
    const float sw = surfaceWpx > 1.0f ? surfaceWpx : 1.0f;
    const float sh = surfaceHpx > 1.0f ? surfaceHpx : 1.0f;
    switch (mode) {
        case Mode::FitAll: {
            const float s = std::fmin(sw / designW, sh / designH);
            return {(sw - designW * s) * 0.5f, (sh - designH * s) * 0.5f, designW * s, designH * s};
        }
        case Mode::Crop: {
            const float s = std::fmax(sw / designW, sh / designH);
            return {-((designW * s) - sw) * 0.5f, -((designH * s) - sh) * 0.5f, designW * s, designH * s};
        }
        case Mode::FitWidth: {
            const float s = sw / designW;
            return {0.0f, (sh - designH * s) * 0.5f, sw, designH * s};
        }
        case Mode::FitHeight: {
            const float s = sh / designH;
            return {(sw - designW * s) * 0.5f, 0.0f, designW * s, sh};
        }
        default:
            return {0.0f, 0.0f, sw, sh}; // Stretch
    }
}

// ─── OpenGL (ES/desktop): glViewport(gl_rect) — no GL headers required ─────
struct GLRect { int x, y, width, height; };
inline GLRect glRect(Mode mode, float wpx, float hpx, float dw, float dh) {
    const auto r = viewportRect(mode, wpx, hpx, dw, dh);
    return {static_cast<int>(r.x), static_cast<int>(r.y),
            static_cast<int>(r.w), static_cast<int>(r.h)};
}

// ─── Vulkan: layout-compatible with VkViewport/VkRect2D fields ─────────────
struct VkLikeViewport { float x, y, width, height, minDepth, maxDepth; };
inline VkLikeViewport vkViewport(Mode mode, float wpx, float hpx, float dw, float dh) {
    const auto r = viewportRect(mode, wpx, hpx, dw, dh);
    return {r.x, r.y, r.w, r.h, 0.0f, 1.0f};
}

// ─── DirectX: field-compatible with D3D11_VIEWPORT / D3D12_VIEWPORT ───────
struct DxViewport { float TopLeftX, TopLeftY, Width, Height, MinDepth, MaxDepth; };
inline DxViewport dxViewport(Mode mode, float wpx, float hpx, float dw, float dh) {
    const auto r = viewportRect(mode, wpx, hpx, dw, dh);
    return {r.x, r.y, r.w, r.h, 0.0f, 1.0f};
}

/// Column-major orthographic projection mapping design units → clip space
/// (usable as-is in GLSL `mat4`, HLSL `float4x4` with transpose, or std140 UBO).
inline void ortho(float left, float top, float right, float bottom, float nearZ, float farZ,
                  float* out16 /* column-major */) {
    const float rl = 1.0f / (right - left);
    const float tb = 1.0f / (bottom - top);
    const float fn = 1.0f / (farZ - nearZ);
    out16[0]  = 2.0f * rl;      out16[1] = 0.0f;          out16[2]  = 0.0f;                out16[3]  = 0.0f;
    out16[4]  = 0.0f;           out16[5] = 2.0f * tb;     out16[6]  = 0.0f;                out16[7]  = 0.0f;
    out16[8]  = 0.0f;           out16[9] = 0.0f;          out16[10] = fn * (farZ - nearZ); out16[11] = 0.0f;
    out16[12] = -(left + right) * rl; out16[13] = -(top + bottom) * tb;
    out16[14] = nearZ * fn * (farZ - nearZ) - 1.0f;   out16[15] = 1.0f;
}

} // namespace appdimens::games::render
