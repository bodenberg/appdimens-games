// AppDimens Games 3.0 — inline kernels (header-only).
// Every kernel mirrors GameMath.kt exactly. Fast lanes are one multiply.
#pragma once

#include "core.h"

namespace appdimens::games::math {

// ─── Fast lanes (O(1), no branches) ────────────────────────────────────────
inline float scaledDp(float base, const Metrics& m) { return base * m.scale; }
inline float scaledArDp(float base, const Metrics& m) { return base * m.scaledArMul; }
inline float widthDp(float base, const Metrics& m) { return base * m.wFactor; }
inline float heightDp(float base, const Metrics& m) { return base * m.hFactor; }

// ─── SCALED (generic path, qualifier/inverter aware) ──────────────────────
inline float calculateScaledDp(float base, const Metrics& m, Axis a,
                               bool applyAspectRatio = false, float customK = -1.0f) {
    if (!applyAspectRatio && a == Axis::SmallestWidth) return base * m.scale;
    if (applyAspectRatio && a == Axis::SmallestWidth && customK < 0.0f) return base * m.scaledArMul;
    const float d = m.axis(a);
    if (applyAspectRatio) {
        const float k = customK < 0.0f ? Constants::SENSITIVITY_DEFAULT : customK;
        return base * (1.0f + (d - Constants::BASE_WIDTH_DP) *
            (Constants::ADJUSTMENT_SCALE + k * std::log(((m.widthDp < m.heightDp ? m.heightDp : m.widthDp) /
              (m.widthDp < m.heightDp ? m.widthDp : m.heightDp)) / Constants::REFERENCE_AR)));
    }
    return base * (d * Constants::INV_BASE_RATIO);
}

// ─── PERCENT ───────────────────────────────────────────────────────────────
inline float percentOfAxisDp(float percent, const Metrics& m, Axis a) {
    return (percent / 100.0f) * m.axis(a);
}

// ─── POWER ─────────────────────────────────────────────────────────────────
inline float powerDp(float base, const Metrics& m, float exponent = Constants::POWER_EXP_DEFAULT,
                     bool ar = false) {
    float out = base * std::pow(m.smallestWidthDp / Constants::BASE_WIDTH_DP, exponent);
    return ar ? out * m.arMul : out;
}

// ─── FLUID (band 320–768) ──────────────────────────────────────────────────
inline float fluidDp(float base, const Metrics& m, float lo = 0.0f, float hi = 0.0f,
                     bool ar = false) {
    if (lo <= 0.0f) lo = base * 0.8f;
    if (hi <= 0.0f) hi = base * 1.2f;
    const float d = m.smallestWidthDp;
    float v = d <= Constants::FLUID_MIN_W ? lo
            : d >= Constants::FLUID_MAX_W ? hi
            : lo + (hi - lo) * (d - Constants::FLUID_MIN_W) /
                  (Constants::FLUID_MAX_W - Constants::FLUID_MIN_W);
    return ar ? v * m.arMul : v;
}

// ─── AUTO (linear ≤480 then log) ───────────────────────────────────────────
inline float autoDp(float base, const Metrics& m, bool ar = false) {
    float out = base * m.autoScale;
    return ar ? out * m.arMul : out;
}

// ─── LOGARITHMIC ───────────────────────────────────────────────────────────
inline float logarithmicDp(float base, const Metrics& m, bool ar = false) {
    float out = base * m.logarithmicScale;
    return ar ? out * m.arMul : out;
}

// ─── INTERPOLATED ──────────────────────────────────────────────────────────
inline float interpolatedDp(float base, const Metrics& m, bool ar = false) {
    float out = base * m.interpolatedScale;
    return ar ? out * m.arMul : out;
}

// ─── DIAGONAL / PERIMETER / FIT / FILL / DENSITY ───────────────────────────
inline float diagonalDp(float base, const Metrics& m, bool ar = false) {
    float out = base * m.diagonalScale;
    return ar ? out * m.arMul : out;
}
inline float perimeterDp(float base, const Metrics& m, bool ar = false) {
    float out = base * m.perimeterScale;
    return ar ? out * m.arMul : out;
}
inline float fitDp(float base, const Metrics& m, bool ar = false) {
    float out = base * m.fitScale;
    return ar ? out * m.arMul : out;
}
inline float fillDp(float base, const Metrics& m, bool ar = false) {
    float out = base * m.fillScale;
    return ar ? out * m.arMul : out;
}
inline float densityDp(float base, const Metrics& m, bool ar = false) {
    float out = base * m.density;
    return ar ? out * m.arMul : out;
}

// ─── Conversions & vectors ─────────────────────────────────────────────────
inline float toPx(float dp, const Metrics& m) { return dp * m.density; }

struct Vec2 { float x, y; };
struct Vec3 { float x, y, z; };

/// Scales a world/design point with the FIT factor (letterbox-consistent).
inline Vec2 scaleVecFit(Vec2 v, const Metrics& m,
                        float designW, float designH) {
    const float mn = m.widthDp < m.heightDp ? m.widthDp : m.heightDp;
    const float mx = m.widthDp < m.heightDp ? m.heightDp : m.widthDp;
    const float s = std::fmin(std::fmin(mn / designW, mx / designH),
                              std::fmin(mx / designW, mn / designH));
    return {v.x * s * m.density, v.y * s * m.density};
}

} // namespace appdimens::games::math
