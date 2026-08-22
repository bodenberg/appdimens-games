// AppDimens Games 3.0 — native core (C++20, header-first).
// Cross-platform: Android NDK, Windows (DirectX/ANGLE), Linux, macOS.
// Hot paths are single-multiply over precomputed factors; zero allocations.
// Math is bit-parity with the Kotlin core and scripts/oracle.py.
#pragma once

#include <atomic>
#include <cmath>
#include <cstdint>

namespace appdimens::games {

// ─── Canonical constants ───────────────────────────────────────────────────
struct Constants {
    static constexpr float BASE_WIDTH_DP     = 300.0f;
    static constexpr float BASE_HEIGHT_DP    = 533.0f;
    // AUDIT: family ships 611.6305f (true sqrt(300^2+533^2)=611.6281550…).
    // Kept for bit-parity across Kotlin/C++/C#/Python.
    static constexpr float BASE_DIAGONAL_DP  = 611.6305f;
    static constexpr float BASE_PERIMETER_DP = 833.0f;
    static constexpr float REFERENCE_AR      = 1.78f;
    static constexpr float INV_REFERENCE_AR  = 0.5617978f;
    static constexpr float INV_BASE_RATIO    = 0.0033333334f;   // 1/300
    static constexpr float ADJUSTMENT_SCALE  = 0.0033333334f;   // 0.10/30
    static constexpr float SENSITIVITY_DEFAULT = 0.0026666667f; // 0.08/30
    static constexpr float FLUID_MIN_W       = 320.0f;
    static constexpr float FLUID_MAX_W       = 768.0f;
    static constexpr float AUTO_TRANSITION   = 480.0f;
    static constexpr float SENSITIVITY_LOG   = 0.4f;
    static constexpr float POWER_EXP_DEFAULT = 0.75f;
};

enum class Axis : uint8_t { SmallestWidth, Width, Height };

/// Aspect-ratio multiplier: 1 + K·ln((max/min)/1.78).
inline float arMultiplier(float minDp, float maxDp, float k = Constants::SENSITIVITY_DEFAULT) {
    if (!(minDp > 0.0f)) return 1.0f;
    const float norm = (maxDp / minDp) / Constants::REFERENCE_AR;
    return 1.0f + k * std::log(norm);
}

// ─── Immutable window snapshot with all factors precomputed ────────────────
struct Metrics {
    float widthDp, heightDp, smallestWidthDp;
    float density;          // dpi/160
    float fontScale;
    bool fullscreen;

    // Precomputed factors (built by make()).
    float scale;            // sw/300
    float wFactor;          // w/300
    float hFactor;          // h/300
    float arMul;            // 1 + K_def·ln(ARn)
    float scaledArMul;      // AR-aware SCALED default multiplier
    float powerScale, interpolatedScale, diagonalScale, perimeterScale,
          logarithmicScale, autoScale, fitScale, fillScale;

    static Metrics make(float widthDp, float heightDp, float smallestWidthDp,
                        float densityDpi, float fontScale = 1.0f, bool fullscreen = true) {
        Metrics m{};
        m.widthDp = widthDp; m.heightDp = heightDp;
        m.smallestWidthDp = smallestWidthDp > 0.0f ? smallestWidthDp : (widthDp < heightDp ? widthDp : heightDp);
        m.density = densityDpi / 160.0f;
        m.fontScale = fontScale > 0.0f ? fontScale : 1.0f;
        m.fullscreen = fullscreen;

        const float mn = widthDp < heightDp ? widthDp : heightDp;
        const float mx = widthDp < heightDp ? heightDp : widthDp;
        const float sw = m.smallestWidthDp;

        m.scale = sw * Constants::INV_BASE_RATIO;
        m.wFactor = widthDp * Constants::INV_BASE_RATIO;
        m.hFactor = heightDp * Constants::INV_BASE_RATIO;
        m.arMul = arMultiplier(mn, mx);
        const float logAr = std::log(((mx / mn) / Constants::REFERENCE_AR));
        m.scaledArMul = 1.0f + (sw - Constants::BASE_WIDTH_DP) *
            (Constants::ADJUSTMENT_SCALE + Constants::SENSITIVITY_DEFAULT * logAr);

        m.powerScale = std::pow(sw / Constants::BASE_WIDTH_DP, Constants::POWER_EXP_DEFAULT);
        m.interpolatedScale = 1.0f + (m.scale - 1.0f) * 0.5f;
        m.diagonalScale = std::sqrt(mn * mn + mx * mx) / Constants::BASE_DIAGONAL_DP;
        m.perimeterScale = (mn + mx) / Constants::BASE_PERIMETER_DP;
        m.logarithmicScale = sw > Constants::BASE_WIDTH_DP
            ? 1.0f + Constants::SENSITIVITY_LOG * std::log(sw * Constants::INV_BASE_RATIO)
            : (sw > 0.0f ? 1.0f - Constants::SENSITIVITY_LOG * std::log(Constants::BASE_WIDTH_DP / sw) : 1.0f);
        m.autoScale = sw <= Constants::AUTO_TRANSITION
            ? sw * Constants::INV_BASE_RATIO
            : (Constants::AUTO_TRANSITION * Constants::INV_BASE_RATIO) +
              Constants::SENSITIVITY_LOG * std::log(1.0f + (sw - Constants::AUTO_TRANSITION) * Constants::INV_BASE_RATIO);
        m.fitScale = std::fmin(mn / Constants::BASE_WIDTH_DP, mx / Constants::BASE_HEIGHT_DP);
        m.fillScale = std::fmax(mn / Constants::BASE_WIDTH_DP, mx / Constants::BASE_HEIGHT_DP);
        return m;
    }

    inline float axis(Axis a) const {
        switch (a) {
            case Axis::Width: return widthDp;
            case Axis::Height: return heightDp;
            default: return smallestWidthDp;
        }
    }
};

// ─── Global live snapshot (lock-free reads, double-buffer publish) ─────────
namespace detail {
    inline std::atomic<const Metrics*>& slot() {
        static std::atomic<const Metrics*> s{nullptr};
        static Metrics def = Metrics::make(300.f, 533.f, 300.f, 160.f, 1.0f, true);
        const Metrics* expected = nullptr;
        s.compare_exchange_strong(expected, &def);
        return s;
    }
    inline std::atomic<const Metrics*>& frozenSlot() {
        static std::atomic<const Metrics*> f{nullptr};
        static Metrics def = Metrics::make(300.f, 533.f, 300.f, 160.f, 1.0f, true);
        const Metrics* expected = nullptr;
        f.compare_exchange_strong(expected, &def);
        return f;
    }
}

/// Publishes a new live snapshot (call on surface resize / config change).
inline void updateMetrics(const Metrics& m) {
    detail::slot().store(&m, std::memory_order_release);
    if (m.fullscreen && ((m.widthDp < m.heightDp ? m.widthDp : m.heightDp) > 0.0f))
        detail::frozenSlot().store(&m, std::memory_order_release);
}

/// Live metrics (auto-adjust path). / Métricas vivas (ajuste automático).
inline const Metrics& metrics() { return *detail::slot().load(std::memory_order_acquire); }

/// Frozen FULLSCREEN metrics for `i` variants. / Snapshot congelado p/ variantes `i`.
inline const Metrics& invariantMetrics() { return *detail::frozenSlot().load(std::memory_order_acquire); }

} // namespace appdimens::games
