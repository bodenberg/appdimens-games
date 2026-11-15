/* AppDimens Games 3.0 — pure C99 API for C game engines (raylib, SDL, custom).
 * Header-only; zero allocations; mirrors appdimens/games/math.h exactly. */
#ifndef APPDIMENS_GAMES_C_H
#define APPDIMENS_GAMES_C_H

#include <math.h>
#include <stdint.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

#define ADG_BASE_W      300.0f
#define ADG_BASE_H      533.0f
#define ADG_BASE_DIAG   611.6305f   /* family literal (audit note in core.h) */
#define ADG_BASE_PERIM  833.0f
#define ADG_REF_AR      1.78f
#define ADG_INV_REF_AR  0.5617978f
#define ADG_INV_BASE    0.0033333334f
#define ADJ_SCALE       0.0033333334f
#define SENS_DEFAULT    0.0026666667f
#define ADG_FLUID_MINW  320.0f
#define ADG_FLUID_MAXW  768.0f
#define ADG_AUTO_T      480.0f
#define ADG_SENS_LOG    0.4f
#define ADG_POW_EXP     0.75f

typedef enum { ADG_AXIS_SW = 0, ADG_AXIS_W = 1, ADG_AXIS_H = 2 } adg_axis;

typedef struct adg_metrics {
    float width_dp, height_dp, smallest_width_dp;
    float density, font_scale;
    bool fullscreen;
    /* precomputed factors */
    float scale, w_factor, h_factor, ar_mul, scaled_ar_mul;
    float power_scale, interpolated_scale, diagonal_scale, perimeter_scale,
          logarithmic_scale, auto_scale, fit_scale, fill_scale;
} adg_metrics;

static inline float adg_ar_mul(float min_dp, float max_dp) {
    if (!(min_dp > 0.0f)) return 1.0f;
    return 1.0f + SENS_DEFAULT * logf((max_dp / min_dp) / ADG_REF_AR);
}

static inline adg_metrics adg_make(float w, float h, float sw,
                                   float dpi, float font_scale, bool fullscreen) {
    adg_metrics m;
    const float mn = fminf(w, h), mx = fmaxf(w, h);
    m.width_dp = w; m.height_dp = h;
    m.smallest_width_dp = sw > 0.0f ? sw : mn;
    m.density = dpi / 160.0f;
    m.font_scale = font_scale > 0.0f ? font_scale : 1.0f;
    m.fullscreen = fullscreen;

    const float s = m.smallest_width_dp;
    m.scale = s * ADG_INV_BASE;
    m.w_factor = w * ADG_INV_BASE;
    m.h_factor = h * ADG_INV_BASE;
    m.ar_mul = adg_ar_mul(mn, mx);
    m.scaled_ar_mul = 1.0f + (s - ADG_BASE_W) *
        (ADJ_SCALE + SENS_DEFAULT * logf(((mx / mn) / ADG_REF_AR)));
    m.power_scale = powf(s / ADG_BASE_W, ADG_POW_EXP);
    m.interpolated_scale = 1.0f + (m.scale - 1.0f) * 0.5f;
    m.diagonal_scale = sqrtf(mn * mn + mx * mx) / ADG_BASE_DIAG;
    m.perimeter_scale = (mn + mx) / ADG_BASE_PERIM;
    m.logarithmic_scale = s > ADG_BASE_W
        ? 1.0f + ADG_SENS_LOG * logf(s * ADG_INV_BASE)
        : (s > 0.0f ? 1.0f - ADG_SENS_LOG * logf(ADG_BASE_W / s) : 1.0f);
    m.auto_scale = s <= ADG_AUTO_T
        ? s * ADG_INV_BASE
        : (ADG_AUTO_T * ADG_INV_BASE) +
          ADG_SENS_LOG * logf(1.0f + (s - ADG_AUTO_T) * ADG_INV_BASE);
    m.fit_scale = fminf(mn / ADG_BASE_W, mx / ADG_BASE_H);
    m.fill_scale = fmaxf(mn / ADG_BASE_W, mx / ADG_BASE_H);
    return m;
}

/* Fast lanes */
static inline float adg_scaled(adg_metrics const* m, float b)   { return b * m->scale; }
static inline float adg_scaled_a(adg_metrics const* m, float b) { return b * m->scaled_ar_mul; }
static inline float adg_hdp(adg_metrics const* m, float b)      { return b * m->h_factor; }
static inline float adg_wdp(adg_metrics const* m, float b)      { return b * m->w_factor; }

/* Kernels (ar flag applies the aspect-ratio multiplier) */
static inline float adg_power(adg_metrics const* m, float b, bool ar) {
    float o = b * m->power_scale; return ar ? o * m->ar_mul : o;
}
static inline float adg_fluid(adg_metrics const* m, float b, bool ar) {
    const float d = m->smallest_width_dp;
    const float lo = b * 0.8f, hi = b * 1.2f;
    float v = d <= ADG_FLUID_MINW ? lo : d >= ADG_FLUID_MAXW ? hi
            : lo + (hi - lo) * (d - ADG_FLUID_MINW) / (ADG_FLUID_MAXW - ADG_FLUID_MINW);
    return ar ? v * m->ar_mul : v;
}
static inline float adg_auto(adg_metrics const* m, float b, bool ar) {
    float o = b * m->auto_scale; return ar ? o * m->ar_mul : o;
}
static inline float adg_logarithmic(adg_metrics const* m, float b, bool ar) {
    float o = b * m->logarithmic_scale; return ar ? o * m->ar_mul : o;
}
static inline float adg_interpolated(adg_metrics const* m, float b, bool ar) {
    float o = b * m->interpolated_scale; return ar ? o * m->ar_mul : o;
}
static inline float adg_diagonal(adg_metrics const* m, float b, bool ar) {
    float o = b * m->diagonal_scale; return ar ? o * m->ar_mul : o;
}
static inline float adg_perimeter(adg_metrics const* m, float b, bool ar) {
    float o = b * m->perimeter_scale; return ar ? o * m->ar_mul : o;
}
static inline float adg_fit(adg_metrics const* m, float b, bool ar) {
    float o = b * m->fit_scale; return ar ? o * m->ar_mul : o;
}
static inline float adg_fill(adg_metrics const* m, float b, bool ar) {
    float o = b * m->fill_scale; return ar ? o * m->ar_mul : o;
}
static inline float adg_density(adg_metrics const* m, float b, bool ar) {
    float o = b * m->density; return ar ? o * m->ar_mul : o;
}
static inline float adg_to_px(adg_metrics const* m, float dp) { return dp * m->density; }

#ifdef __cplusplus
} /* extern "C" */
#endif
#endif /* APPDIMENS_GAMES_C_H */
