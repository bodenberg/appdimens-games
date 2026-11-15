/*
 * AppDimens core — portable single-header C99 (C++ compatible).
 * Version 3.2.0 · Apache-2.0 · https://github.com/bodenberg/appdimens
 *
 * Same constants, strategies, tokens and results as every AppDimens library
 * (Android, iOS, Flutter, React Native, Web, .NET, Python, Qt, GTK, LVGL, Games).
 *
 * Design:
 *   - ad_metrics is an immutable snapshot. ad_metrics_init() precomputes every factor
 *     (12 strategies × 11 axis variants × 4 flag combos = 528 values, ~2 KiB float).
 *   - The hot path is ONE load + ONE multiply: ad_dp(m, AD_SCALED, AD_SLOT_S, v).
 *   - No allocation, no global state, no locks. Thread-safe by construction (publish a
 *     new snapshot and swap a pointer; see ad_live in appdimens_live.h if you need it).
 *
 * Configuration macros (define before including):
 *   AD_USE_DOUBLE      ad_real = double (default float, the family's IEEE-754 binary32)
 *   AD_FIXED_POINT     also build Q16.16 tables for FPU-less MCUs (ad_dp_q / ad_q_round)
 *   AD_NO_LIBM         use the built-in log/pow/sqrt (no libm link; rel. error < 2e-7)
 *   AD_IMPLEMENTATION  not needed: everything is static inline.
 */
#ifndef APPDIMENS_H
#define APPDIMENS_H

#include <stdint.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

#define AD_VERSION_MAJOR 3
#define AD_VERSION_MINOR 2
#define AD_VERSION_PATCH 0
#define AD_VERSION_STRING "3.2.0"

#ifdef AD_USE_DOUBLE
typedef double ad_real;
#define AD_R(x) (x)
#else
typedef float ad_real;
#define AD_R(x) (x##f)
#endif

#if defined(__GNUC__) || defined(__clang__)
#define AD_INLINE static __inline__ __attribute__((always_inline, unused))
#define AD_FN static __inline__ __attribute__((unused))
#elif defined(_MSC_VER)
#define AD_INLINE static __forceinline
#define AD_FN static __inline
#else
#define AD_INLINE static inline
#define AD_FN static inline
#endif

/* ── Canonical constants (DesignScaleConstants.kt) ─────────────────────────────── */
#define AD_BASE_WIDTH_DP        AD_R(300.0)
#define AD_BASE_HEIGHT_DP       AD_R(533.0)
#define AD_BASE_DIAGONAL_DP     AD_R(611.6305)
#define AD_BASE_PERIMETER_DP    AD_R(833.0)
#define AD_REFERENCE_AR         AD_R(1.78)
#define AD_INV_BASE_RATIO       (AD_R(1.0) / AD_R(300.0))
#define AD_ADJUSTMENT_SCALE     (AD_R(0.1) / AD_R(30.0))
#define AD_SENSITIVITY_DEFAULT  (AD_R(0.08) / AD_R(30.0))
#define AD_FLUID_MIN_WIDTH      AD_R(320.0)
#define AD_FLUID_MAX_WIDTH      AD_R(768.0)
#define AD_FLUID_MIN_FACTOR     AD_R(0.8)
#define AD_FLUID_MAX_FACTOR     AD_R(1.2)
#define AD_AUTO_TRANSITION_DP   AD_R(480.0)
#define AD_LOG_SENSITIVITY      AD_R(0.4)
#define AD_POWER_EXPONENT       AD_R(0.75)
#define AD_DENSITY_DPI_BASE     AD_R(160.0)
#define AD_MM_PER_INCH          AD_R(25.4)

/* ── Enums ──────────────────────────────────────────────────────────────────────── */
typedef enum ad_strategy {
    AD_SCALED = 0, AD_AUTO, AD_PERCENT, AD_POWER, AD_FLUID, AD_INTERPOLATED,
    AD_LOGARITHMIC, AD_DIAGONAL, AD_PERIMETER, AD_FIT, AD_FILL, AD_DENSITY,
    AD_STRATEGY_COUNT
} ad_strategy;

typedef enum ad_qualifier { AD_Q_SMALL_WIDTH = 0, AD_Q_HEIGHT = 1, AD_Q_WIDTH = 2 } ad_qualifier;

typedef enum ad_inverter {
    AD_INV_DEFAULT = 0, AD_INV_PH_TO_LW, AD_INV_PW_TO_LH, AD_INV_LH_TO_PW, AD_INV_LW_TO_PH,
    AD_INV_SW_TO_LH, AD_INV_SW_TO_LW, AD_INV_SW_TO_PH, AD_INV_SW_TO_PW
} ad_inverter;

typedef enum ad_orientation { AD_ORIENT_AUTO = 0, AD_ORIENT_PORTRAIT = 1, AD_ORIENT_LANDSCAPE = 2 } ad_orientation;

typedef enum ad_ui_mode {
    AD_UI_NORMAL = 0, AD_UI_TELEVISION, AD_UI_CAR, AD_UI_WATCH, AD_UI_DESK, AD_UI_APPLIANCE,
    AD_UI_VR_HEADSET, AD_UI_UNDEFINED, AD_UI_FOLD_OPEN, AD_UI_FOLD_CLOSED, AD_UI_FLIP_OPEN,
    AD_UI_FLIP_CLOSED, AD_UI_FOLD_HALF_OPENED, AD_UI_FLIP_HALF_OPENED
} ad_ui_mode;

typedef enum ad_kind { AD_KIND_DP = 0, AD_KIND_SP = 1, AD_KIND_EM = 2 } ad_kind;

/* Axis variants of the precomputed tables (same order in every AppDimens port). */
typedef enum ad_variant {
    AD_VAR_S = 0, AD_VAR_H, AD_VAR_W,
    AD_VAR_S_PH, AD_VAR_S_LH, AD_VAR_S_PW, AD_VAR_S_LW,
    AD_VAR_H_LW, AD_VAR_H_PW, AD_VAR_W_LH, AD_VAR_W_PH,
    AD_VARIANT_COUNT
} ad_variant;

#define AD_FLAG_NONE 0u
#define AD_FLAG_A    1u   /* aspect-ratio correction (suffix a)   */
#define AD_FLAG_I    2u   /* ignore multi-window      (suffix i)   */
#define AD_FLAG_IA   3u   /* both                     (suffix ia)  */
#define AD_SLOT_COUNT (AD_VARIANT_COUNT * 4)
#define AD_SLOT(variant, flags) ((int)(variant) * 4 + (int)(flags))

/* ── Inputs and snapshot ────────────────────────────────────────────────────────── */
typedef struct ad_input {
    ad_real width;            /* logical units (dp / GTK app px / Qt logical px / LVGL px) */
    ad_real height;
    ad_real smallest_width;   /* 0 → min(width, height)                                    */
    ad_real density;          /* physical px per logical unit; 0 → 1                       */
    ad_real font_scale;       /* user text scale; 0 → 1                                    */
    ad_real dpi;              /* physical px per inch; 0 → 160 × density                   */
    ad_orientation orientation; /* AUTO → landscape when width > height                    */
    ad_ui_mode ui_mode;
    int multi_window;         /* split / floating window → `i` variants return the base   */
} ad_input;

/** Zero/auto defaults for every input (warning-clean alternative to `= {0}`). */
AD_INLINE void ad_input_default(ad_input *in) {
    in->width = 0; in->height = 0; in->smallest_width = 0; in->density = 0; in->font_scale = 0; in->dpi = 0;
    in->orientation = AD_ORIENT_AUTO; in->ui_mode = AD_UI_NORMAL; in->multi_window = 0;
}

typedef struct ad_metrics {
    ad_input in;              /* sanitised inputs */
    ad_real min_dim, max_dim, sw;
    int is_landscape, is_portrait;
    ad_real scale, log_nar, ar_mul, scaled_ar_mul;
    ad_real power_scale, interpolated_scale, diagonal_scale, perimeter_scale, logarithmic_scale;
    uint32_t generation;      /* bumped by the caller when publishing; free for user use */
    ad_real table[AD_STRATEGY_COUNT][AD_SLOT_COUNT];
#ifdef AD_FIXED_POINT
    int32_t qtable[AD_STRATEGY_COUNT][AD_SLOT_COUNT]; /* Q16.16 */
#endif
} ad_metrics;

/* ── Math backend ───────────────────────────────────────────────────────────────── */
#ifndef AD_NO_LIBM
#include <math.h>
#ifdef AD_USE_DOUBLE
#define ad_log  log
#define ad_pow  pow
#define ad_sqrt sqrt
#else
#define ad_log  logf
#define ad_pow  powf
#define ad_sqrt sqrtf
#endif
#else
/* Freestanding fallbacks (double internally, then rounded). */
AD_FN double ad__logd(double x) {
    if (!(x > 0.0)) return -1e300;
    int e = 0;
    while (x > 1.4142135623730951) { x *= 0.5; e++; }
    while (x < 0.7071067811865476) { x *= 2.0; e--; }
    double t = (x - 1.0) / (x + 1.0), t2 = t * t, s = 0.0, p = t;
    for (int k = 1; k < 40; k += 2) { s += p / k; p *= t2; }
    return 2.0 * s + e * 0.6931471805599453;
}
AD_FN double ad__expd(double x) {
    if (x < -745.0) return 0.0;
    if (x > 709.0) return 1.0e308;
    int k = (int)(x / 0.6931471805599453 + (x >= 0 ? 0.5 : -0.5));
    double r = x - k * 0.6931471805599453, s = 1.0, t = 1.0;
    for (int n = 1; n < 24; n++) { t *= r / n; s += t; }
    while (k > 0) { s *= 2.0; k--; }
    while (k < 0) { s *= 0.5; k++; }
    return s;
}
AD_FN double ad__sqrtd(double x) {
    if (!(x > 0.0)) return 0.0;
    double r = x > 1.0 ? x : 1.0;
    for (int i = 0; i < 64; i++) { double n = 0.5 * (r + x / r); if (n == r) break; r = n; }
    return r;
}
#define ad_log(x)    ((ad_real)ad__logd((double)(x)))
AD_FN double ad__powd(double x, double y) {
    if (x == 1.0 || y == 0.0) return 1.0;
    if (!(x > 0.0)) return 0.0; /* domain of the AppDimens kernels: x ≥ 0, y > 0 */
    return ad__expd(y * ad__logd(x));
}
#define ad_pow(x, y) ((ad_real)ad__powd((double)(x), (double)(y)))
#define ad_sqrt(x)   ((ad_real)ad__sqrtd((double)(x)))
#endif

AD_INLINE int ad__finite_pos(ad_real v) { return v > AD_R(0.0) && v < AD_R(1e30); }

/* ── Plumbing (DimenCalculationPlumbing) ────────────────────────────────────────── */
AD_FN ad_qualifier ad_effective_qualifier(ad_qualifier q, ad_inverter inv, int land, int port) {
    switch (inv) {
    case AD_INV_PH_TO_LW: return (land && q == AD_Q_HEIGHT) ? AD_Q_WIDTH : q;
    case AD_INV_PW_TO_LH: return (land && q == AD_Q_WIDTH) ? AD_Q_HEIGHT : q;
    case AD_INV_LH_TO_PW: return (port && q == AD_Q_HEIGHT) ? AD_Q_WIDTH : q;
    case AD_INV_LW_TO_PH: return (port && q == AD_Q_WIDTH) ? AD_Q_HEIGHT : q;
    case AD_INV_SW_TO_LH: return (land && q == AD_Q_SMALL_WIDTH) ? AD_Q_HEIGHT : q;
    case AD_INV_SW_TO_LW: return (land && q == AD_Q_SMALL_WIDTH) ? AD_Q_WIDTH : q;
    case AD_INV_SW_TO_PH: return (port && q == AD_Q_SMALL_WIDTH) ? AD_Q_HEIGHT : q;
    case AD_INV_SW_TO_PW: return (port && q == AD_Q_SMALL_WIDTH) ? AD_Q_WIDTH : q;
    case AD_INV_DEFAULT: return q;
    default: return q;
    }
}

AD_INLINE ad_real ad_read_axis(const ad_metrics *m, ad_qualifier q) {
    return q == AD_Q_HEIGHT ? m->in.height : (q == AD_Q_WIDTH ? m->in.width : m->sw);
}

AD_FN ad_real ad_logarithmic_scale_for(ad_real d) {
    if (d > AD_BASE_WIDTH_DP) return AD_R(1.0) + AD_LOG_SENSITIVITY * ad_log(d * AD_INV_BASE_RATIO);
    if (d > AD_R(0.0)) return AD_R(1.0) - AD_LOG_SENSITIVITY * ad_log(AD_BASE_WIDTH_DP / d);
    return AD_R(1.0);
}

static const uint8_t ad__variant_q[AD_VARIANT_COUNT] = {0, 1, 2, 0, 0, 0, 0, 1, 1, 2, 2};
static const uint8_t ad__variant_inv[AD_VARIANT_COUNT] = {0, 0, 0, 7, 5, 8, 6, 1, 3, 2, 4};

AD_INLINE int ad__linear_ar(ad_strategy s) { return s == AD_SCALED || s == AD_PERCENT; }
AD_INLINE int ad__axis_free(ad_strategy s) { return s >= AD_DIAGONAL; }

AD_FN ad_real ad__base_scale(ad_strategy s, const ad_metrics *m, ad_qualifier q, int def_sw) {
    ad_real d;
    switch (s) {
    case AD_SCALED: case AD_PERCENT:
        return def_sw ? m->scale : ad_read_axis(m, q) * AD_INV_BASE_RATIO;
    case AD_AUTO:
        d = ad_read_axis(m, q);
        if (d <= AD_AUTO_TRANSITION_DP) return d * AD_INV_BASE_RATIO;
        return AD_AUTO_TRANSITION_DP * AD_INV_BASE_RATIO +
               AD_LOG_SENSITIVITY * ad_log(AD_R(1.0) + (d - AD_AUTO_TRANSITION_DP) * AD_INV_BASE_RATIO);
    case AD_POWER:
        return def_sw ? m->power_scale : ad_pow(ad_read_axis(m, q) / AD_BASE_WIDTH_DP, AD_POWER_EXPONENT);
    case AD_FLUID:
        d = ad_read_axis(m, q);
        if (d <= AD_FLUID_MIN_WIDTH) return AD_FLUID_MIN_FACTOR;
        if (d >= AD_FLUID_MAX_WIDTH) return AD_FLUID_MAX_FACTOR;
        return AD_FLUID_MIN_FACTOR + ((AD_FLUID_MAX_FACTOR - AD_FLUID_MIN_FACTOR) * (d - AD_FLUID_MIN_WIDTH)) /
                                         (AD_FLUID_MAX_WIDTH - AD_FLUID_MIN_WIDTH);
    case AD_INTERPOLATED:
        return def_sw ? m->interpolated_scale
                      : AD_R(1.0) + (ad_read_axis(m, q) * AD_INV_BASE_RATIO - AD_R(1.0)) * AD_R(0.5);
    case AD_LOGARITHMIC:
        return def_sw ? m->logarithmic_scale : ad_logarithmic_scale_for(ad_read_axis(m, q));
    case AD_DIAGONAL: return m->diagonal_scale;
    case AD_PERIMETER: return m->perimeter_scale;
    case AD_FIT: {
        ad_real a = m->min_dim / AD_BASE_WIDTH_DP, b = m->max_dim / AD_BASE_HEIGHT_DP;
        return a < b ? a : b;
    }
    case AD_FILL: {
        ad_real a = m->min_dim / AD_BASE_WIDTH_DP, b = m->max_dim / AD_BASE_HEIGHT_DP;
        return a > b ? a : b;
    }
    case AD_DENSITY: return m->in.density;
    case AD_STRATEGY_COUNT: return AD_R(1.0);
    default: return AD_R(1.0);
    }
}

/**
 * General factor: any strategy / qualifier / inverter / flags and an optional custom
 * aspect-ratio sensitivity `k` (pass a negative-zero-free NaN-free value; use
 * AD_SENSITIVITY_DEFAULT for the default). The tables are built with this function.
 */
AD_FN ad_real ad_compute_factor_k(const ad_metrics *m, ad_strategy s, ad_qualifier qual, ad_inverter inv,
                                  int ignore_mw, int aspect, ad_real k) {
    if (ignore_mw && m->in.multi_window) return AD_R(1.0);
    const int def_sw = qual == AD_Q_SMALL_WIDTH && inv == AD_INV_DEFAULT;
    const ad_qualifier q = ad__axis_free(s) ? qual : ad_effective_qualifier(qual, inv, m->is_landscape, m->is_portrait);
    if (ad__linear_ar(s)) {
        const ad_real d = def_sw ? m->sw : ad_read_axis(m, q);
        if (!aspect) return def_sw ? m->scale : d * AD_INV_BASE_RATIO;
        return AD_R(1.0) + (d - AD_BASE_WIDTH_DP) * (AD_ADJUSTMENT_SCALE + k * m->log_nar);
    }
    const ad_real base = ad__base_scale(s, m, q, def_sw);
    return aspect ? base * (AD_R(1.0) + k * m->log_nar) : base;
}

AD_FN ad_real ad_compute_factor(const ad_metrics *m, ad_strategy s, ad_qualifier q, ad_inverter inv,
                                int ignore_mw, int aspect) {
    return ad_compute_factor_k(m, s, q, inv, ignore_mw, aspect, AD_SENSITIVITY_DEFAULT);
}

/** Builds a snapshot (sanitises inputs, precomputes every table). ~1–3 µs on desktop CPUs. */
AD_FN void ad_metrics_init(ad_metrics *m, const ad_input *input) {
    ad_input in = *input;
    if (!ad__finite_pos(in.width)) in.width = AD_R(0.0);
    if (!ad__finite_pos(in.height)) in.height = AD_R(0.0);
    if (!ad__finite_pos(in.density)) in.density = AD_R(1.0);
    if (!ad__finite_pos(in.font_scale)) in.font_scale = AD_R(1.0);
    if (!ad__finite_pos(in.dpi)) in.dpi = AD_DENSITY_DPI_BASE * in.density;
    m->min_dim = in.width < in.height ? in.width : in.height;
    m->max_dim = in.width < in.height ? in.height : in.width;
    if (!ad__finite_pos(in.smallest_width)) in.smallest_width = m->min_dim;
    if (in.orientation == AD_ORIENT_AUTO && (in.width > 0 || in.height > 0))
        in.orientation = in.width > in.height ? AD_ORIENT_LANDSCAPE : AD_ORIENT_PORTRAIT;
    in.multi_window = in.multi_window ? 1 : 0;
    m->in = in;
    m->is_landscape = in.orientation == AD_ORIENT_LANDSCAPE;
    m->is_portrait = in.orientation == AD_ORIENT_PORTRAIT;
    m->sw = in.smallest_width > AD_R(0.0) ? in.smallest_width : AD_BASE_WIDTH_DP;
    m->scale = m->sw * AD_INV_BASE_RATIO;
    {
        ad_real nar = (m->min_dim > AD_R(0.0) ? m->max_dim / m->min_dim : AD_R(1.0)) / AD_REFERENCE_AR;
        if (!ad__finite_pos(nar)) nar = AD_R(1.0);
        m->log_nar = ad_log(nar);
    }
    m->ar_mul = AD_R(1.0) + AD_SENSITIVITY_DEFAULT * m->log_nar;
    m->scaled_ar_mul = AD_R(1.0) + (m->sw - AD_BASE_WIDTH_DP) * (AD_ADJUSTMENT_SCALE + AD_SENSITIVITY_DEFAULT * m->log_nar);
    m->power_scale = ad_pow(m->sw / AD_BASE_WIDTH_DP, AD_POWER_EXPONENT);
    m->interpolated_scale = AD_R(1.0) + (m->sw * AD_INV_BASE_RATIO - AD_R(1.0)) * AD_R(0.5);
    m->diagonal_scale = ad_sqrt(m->min_dim * m->min_dim + m->max_dim * m->max_dim) / AD_BASE_DIAGONAL_DP;
    m->perimeter_scale = (m->min_dim + m->max_dim) / AD_BASE_PERIMETER_DP;
    m->logarithmic_scale = ad_logarithmic_scale_for(m->sw);
    m->generation = 0;
    for (int s = 0; s < AD_STRATEGY_COUNT; s++) {
        for (int v = 0; v < AD_VARIANT_COUNT; v++) {
            const ad_qualifier q = (ad_qualifier)ad__variant_q[v];
            const ad_inverter inv = (ad_inverter)ad__variant_inv[v];
            const ad_real plain = ad_compute_factor(m, (ad_strategy)s, q, inv, 0, 0);
            const ad_real ar = ad_compute_factor(m, (ad_strategy)s, q, inv, 0, 1);
            ad_real *t = &m->table[s][v * 4];
            t[0] = plain;
            t[1] = ar;
            t[2] = in.multi_window ? AD_R(1.0) : plain;
            t[3] = in.multi_window ? AD_R(1.0) : ar;
        }
    }
#ifdef AD_FIXED_POINT
    for (int s = 0; s < AD_STRATEGY_COUNT; s++)
        for (int i = 0; i < AD_SLOT_COUNT; i++) {
            double f = (double)m->table[s][i] * 65536.0 + 0.5;
            m->qtable[s][i] = f > 2147483647.0 ? 2147483647 : (int32_t)f;
        }
#endif
}

/** Convenience: build from logical size + density (orientation AUTO, no multi-window). */
AD_FN void ad_metrics_make(ad_metrics *m, ad_real width, ad_real height, ad_real density, ad_real font_scale) {
    ad_input in;
    ad_input_default(&in);
    in.width = width; in.height = height; in.density = density; in.font_scale = font_scale;
    ad_metrics_init(m, &in);
}

/** Compares the inputs of two snapshots (skip republishing identical snapshots). */
AD_FN int ad_metrics_same(const ad_metrics *a, const ad_metrics *b) {
    const ad_input *x = &a->in, *y = &b->in;
    return x->width == y->width && x->height == y->height && x->smallest_width == y->smallest_width &&
           x->density == y->density && x->font_scale == y->font_scale && x->dpi == y->dpi &&
           x->orientation == y->orientation && x->ui_mode == y->ui_mode && x->multi_window == y->multi_window;
}

/* ── Hot path ───────────────────────────────────────────────────────────────────── */
AD_INLINE ad_real ad_factor(const ad_metrics *m, ad_strategy s, int slot) { return m->table[s][slot]; }
/** Layout size in logical units. */
AD_INLINE ad_real ad_dp(const ad_metrics *m, ad_strategy s, int slot, ad_real v) { return v * m->table[s][slot]; }
/** Text size in logical units, × user font scale. */
AD_INLINE ad_real ad_sp(const ad_metrics *m, ad_strategy s, int slot, ad_real v) { return v * m->table[s][slot] * m->in.font_scale; }
/** Text size ignoring the user font scale. */
AD_INLINE ad_real ad_em(const ad_metrics *m, ad_strategy s, int slot, ad_real v) { return v * m->table[s][slot]; }
/** Physical pixels (× density). */
AD_INLINE ad_real ad_px(const ad_metrics *m, ad_strategy s, int slot, ad_real v) { return v * m->table[s][slot] * m->in.density; }
AD_INLINE ad_real ad_sp_px(const ad_metrics *m, ad_strategy s, int slot, ad_real v) {
    return v * m->table[s][slot] * m->in.font_scale * m->in.density;
}
/** Generic resolve with kind and px. */
AD_INLINE ad_real ad_resolve(const ad_metrics *m, ad_strategy s, int slot, ad_kind kind, int px, ad_real v) {
    ad_real r = v * m->table[s][slot];
    if (kind == AD_KIND_SP) r *= m->in.font_scale;
    return px ? r * m->in.density : r;
}
/** Rounds to the nearest integer (for toolkits that take int pixels). */
AD_INLINE int32_t ad_round(ad_real v) { return (int32_t)(v >= 0 ? v + AD_R(0.5) : v - AD_R(0.5)); }

#ifdef AD_FIXED_POINT
/** Integer hot path (no FPU): round(v × factor) with a Q16.16 table. |v| ≤ 32767. */
AD_INLINE int32_t ad_dp_q(const ad_metrics *m, ad_strategy s, int slot, int32_t v) {
    int64_t r = (int64_t)v * m->qtable[s][slot];
    return (int32_t)((r + (r >= 0 ? 32768 : -32768)) / 65536);
}
#endif

/* Percent space (literal % of a side). */
AD_INLINE ad_real ad_space_w(const ad_metrics *m, ad_real p) { return p * AD_R(0.01) * m->in.width; }
AD_INLINE ad_real ad_space_h(const ad_metrics *m, ad_real p) { return p * AD_R(0.01) * m->in.height; }
AD_INLINE ad_real ad_space_sw(const ad_metrics *m, ad_real p) { return p * AD_R(0.01) * m->sw; }

/* Physical units (dp = inches × dpi / density). */
AD_INLINE ad_real ad_inch(const ad_metrics *m, ad_real v) { return v * m->in.dpi / m->in.density; }
AD_INLINE ad_real ad_mm(const ad_metrics *m, ad_real v) { return ad_inch(m, v / AD_MM_PER_INCH); }
AD_INLINE ad_real ad_cm(const ad_metrics *m, ad_real v) { return ad_inch(m, v * AD_R(10.0) / AD_MM_PER_INCH); }
AD_INLINE ad_real ad_mm_px(const ad_metrics *m, ad_real v) { return v / AD_MM_PER_INCH * m->in.dpi; }

/* ── Named shortcuts: ad_<prefix><axis><kind>[_a|_i|_ia][_px](m, v) ──────────────
 * e.g. ad_sdp, ad_hdp_a, ad_assp, ad_ftsdp_px, ad_logwem_ia. Inverter variants:
 * ad_dp(m, AD_SCALED, AD_SLOT(AD_VAR_S_PH, AD_FLAG_A), v).                         */
#define AD__DEF4(name, S, V, fn)                                                                       \
    AD_INLINE ad_real ad_##name(const ad_metrics *m, ad_real v) { return fn(m, S, AD_SLOT(V, 0), v); }    \
    AD_INLINE ad_real ad_##name##_a(const ad_metrics *m, ad_real v) { return fn(m, S, AD_SLOT(V, 1), v); } \
    AD_INLINE ad_real ad_##name##_i(const ad_metrics *m, ad_real v) { return fn(m, S, AD_SLOT(V, 2), v); } \
    AD_INLINE ad_real ad_##name##_ia(const ad_metrics *m, ad_real v) { return fn(m, S, AD_SLOT(V, 3), v); }
#define AD__DEF_AXIS(p, S, a, V)                \
    AD__DEF4(p##a##dp, S, V, ad_dp)             \
    AD__DEF4(p##a##sp, S, V, ad_sp)             \
    AD__DEF4(p##a##em, S, V, ad_em)             \
    AD__DEF4(p##a##dp_px, S, V, ad_px)          \
    AD__DEF4(p##a##sp_px, S, V, ad_sp_px)
#define AD__DEF_STRATEGY(p, S) AD__DEF_AXIS(p, S, s, AD_VAR_S) AD__DEF_AXIS(p, S, h, AD_VAR_H) AD__DEF_AXIS(p, S, w, AD_VAR_W)

AD__DEF_STRATEGY(, AD_SCALED)
AD__DEF_STRATEGY(a, AD_AUTO)
AD__DEF_STRATEGY(p, AD_PERCENT)
AD__DEF_STRATEGY(pw, AD_POWER)
AD__DEF_STRATEGY(f, AD_FLUID)
AD__DEF_STRATEGY(i, AD_INTERPOLATED)
AD__DEF_STRATEGY(log, AD_LOGARITHMIC)
AD__DEF_STRATEGY(dg, AD_DIAGONAL)
AD__DEF_STRATEGY(pr, AD_PERIMETER)
AD__DEF_STRATEGY(ft, AD_FIT)
AD__DEF_STRATEGY(fl, AD_FILL)
AD__DEF_STRATEGY(d, AD_DENSITY)

/* ── Token parser: "sdp", "asdpa", "hdpLw", "sdpPxiaPh", "logssp" … ────────────── */
typedef struct ad_token {
    ad_strategy strategy;
    ad_qualifier qualifier;
    ad_inverter inverter;
    ad_kind kind;
    int px;
    int slot;   /* precomputed table slot */
} ad_token;

static const char *const ad__prefixes[AD_STRATEGY_COUNT] = {"", "a", "p", "pw", "f", "i", "log", "dg", "pr", "ft", "fl", "d"};

AD_FN int ad__starts(const char *s, const char *p, size_t n) {
    for (size_t i = 0; i < n; i++) if (s[i] != p[i]) return 0;
    return 1;
}
AD_FN size_t ad__len(const char *s) { size_t n = 0; while (s[n]) n++; return n; }

AD_FN int ad__parse_rest(const char *s, size_t n, ad_token *t) {
    /* s = <axis><kind>[Px][a|i|ia][Ph|Lh|Pw|Lw][a|i|ia][Px] */
    if (n < 3) return 0;
    char axis = s[0];
    if (axis == 's') t->qualifier = AD_Q_SMALL_WIDTH;
    else if (axis == 'h') t->qualifier = AD_Q_HEIGHT;
    else if (axis == 'w') t->qualifier = AD_Q_WIDTH;
    else return 0;
    if (ad__starts(s + 1, "dp", 2)) t->kind = AD_KIND_DP;
    else if (ad__starts(s + 1, "sp", 2)) t->kind = AD_KIND_SP;
    else if (ad__starts(s + 1, "em", 2)) t->kind = AD_KIND_EM;
    else return 0;
    size_t i = 3;
    int px = 0, flags = -1, inv_seen = 0;
    char inv[3] = {0};
    while (i < n) {
        if (n - i >= 2 && s[i] == 'P' && s[i + 1] == 'x') { if (px) return 0; px = 1; i += 2; continue; }
        if (n - i >= 2 && (s[i] == 'P' || s[i] == 'L') && (s[i + 1] == 'h' || s[i + 1] == 'w')) {
            if (inv_seen) return 0;
            inv_seen = 1; inv[0] = s[i]; inv[1] = s[i + 1]; i += 2; continue;
        }
        if (s[i] == 'i' || s[i] == 'a') {
            if (flags >= 0) return 0;
            if (s[i] == 'i' && i + 1 < n && s[i + 1] == 'a') { flags = 3; i += 2; }
            else { flags = s[i] == 'a' ? 1 : 2; i += 1; }
            continue;
        }
        return 0;
    }
    if (flags < 0) flags = 0;
    ad_variant v = (ad_variant)t->qualifier; /* S/H/W = 0/1/2 */
    t->inverter = AD_INV_DEFAULT;
    if (inv_seen) {
        const int P = inv[0] == 'P', H = inv[1] == 'h';
        if (axis == 's') { v = P ? (H ? AD_VAR_S_PH : AD_VAR_S_PW) : (H ? AD_VAR_S_LH : AD_VAR_S_LW); }
        else if (axis == 'h' && !H) { v = P ? AD_VAR_H_PW : AD_VAR_H_LW; }
        else if (axis == 'w' && H) { v = P ? AD_VAR_W_PH : AD_VAR_W_LH; }
        else return 0;
        t->inverter = (ad_inverter)ad__variant_inv[v];
    }
    t->px = px;
    t->slot = AD_SLOT(v, flags);
    return 1;
}

/** Parses a token name of length `n` (or NUL-terminated when n == 0). Returns 1 on success. */
AD_FN int ad_parse_token_n(const char *name, size_t n, ad_token *out) {
    if (!name) return 0;
    if (n == 0) n = ad__len(name);
    /* longest prefix first, with backtracking (e.g. "pwdp" = percent + w, "pwsdp" = power) */
    static const int order[AD_STRATEGY_COUNT] = {6, 3, 7, 8, 9, 10, 1, 2, 4, 5, 11, 0};
    for (int k = 0; k < AD_STRATEGY_COUNT; k++) {
        const int s = order[k];
        const size_t pl = ad__len(ad__prefixes[s]);
        if (pl > n || !ad__starts(name, ad__prefixes[s], pl)) continue;
        ad_token t;
        t.strategy = (ad_strategy)s;
        if (ad__parse_rest(name + pl, n - pl, &t)) { *out = t; return 1; }
    }
    return 0;
}
AD_INLINE int ad_parse_token(const char *name, ad_token *out) { return ad_parse_token_n(name, 0, out); }

AD_INLINE ad_real ad_token_resolve(const ad_metrics *m, const ad_token *t, ad_real v) {
    return ad_resolve(m, t->strategy, t->slot, t->kind, t->px, v);
}

/**
 * Parses "<number><token>" (e.g. "16sdp", "14.5assp", "-4hdpLw", "50spaceW", "5mm", "3px",
 * "12dp"). Stops at the first char that cannot belong to a token. Writes the number of
 * consumed chars to *consumed (optional). Returns 1 on success.
 */
AD_FN int ad_dimen_n(const ad_metrics *m, const char *s, size_t n, ad_real *out, size_t *consumed) {
    size_t i = 0;
    if (n == 0) n = ad__len(s);
    double sign = 1.0, num = 0.0, frac = 0.1;
    int digits = 0;
    if (i < n && (s[i] == '-' || s[i] == '+')) { if (s[i] == '-') sign = -1.0; i++; }
    while (i < n && s[i] >= '0' && s[i] <= '9') { num = num * 10.0 + (s[i] - '0'); i++; digits++; }
    if (i < n && s[i] == '.') { i++; while (i < n && s[i] >= '0' && s[i] <= '9') { num += (s[i] - '0') * frac; frac *= 0.1; i++; digits++; } }
    if (!digits) return 0;
    const size_t t0 = i;
    while (i < n && ((s[i] >= 'a' && s[i] <= 'z') || (s[i] >= 'A' && s[i] <= 'Z'))) i++;
    const size_t tl = i - t0;
    const char *tok = s + t0;
    const ad_real v = (ad_real)(sign * num);
    ad_real r;
    ad_token t;
    if (tl == 2 && ad__starts(tok, "dp", 2)) r = v;
    else if (tl == 2 && ad__starts(tok, "px", 2)) r = v / m->in.density;
    else if (tl == 2 && ad__starts(tok, "mm", 2)) r = ad_mm(m, v);
    else if (tl == 2 && ad__starts(tok, "cm", 2)) r = ad_cm(m, v);
    else if (tl == 4 && ad__starts(tok, "inch", 4)) r = ad_inch(m, v);
    else if (tl == 6 && ad__starts(tok, "spaceW", 6)) r = ad_space_w(m, v);
    else if (tl == 6 && ad__starts(tok, "spaceH", 6)) r = ad_space_h(m, v);
    else if (tl == 7 && ad__starts(tok, "spaceSw", 7)) r = ad_space_sw(m, v);
    else if (tl > 0 && ad_parse_token_n(tok, tl, &t)) r = ad_token_resolve(m, &t, v);
    else return 0;
    *out = r;
    if (consumed) *consumed = i;
    return 1;
}
AD_INLINE int ad_dimen(const ad_metrics *m, const char *s, ad_real *out) {
    size_t used = 0;
    return ad_dimen_n(m, s, 0, out, &used) && s[used] == '\0';
}

/* ── Resize (largest step that fits, O(log n)) ──────────────────────────────────── */
typedef int (*ad_fits_fn)(ad_real candidate, void *user);

/** Largest value in {lo, lo+step, …, hi} (hi always included) where fits() is true; lo if none. */
AD_FN ad_real ad_resize_fit(ad_real lo, ad_real hi, ad_real step, ad_fits_fn fits, void *user) {
    if (hi < lo) { ad_real t = lo; lo = hi; hi = t; }
    if (!(step > AD_R(0.0))) return lo;
    long count = (long)((hi - lo) / step + AD_R(1e-4)) + 1;
    if (count > 4096) count = 4096;
    const int extra = (lo + (ad_real)(count - 1) * step) < hi - step * AD_R(1e-4);
    const long n = count + extra;
    long left = 0, right = n - 1;
    ad_real best = lo;
    int found = 0;
    while (left <= right) {
        const long mid = (left + right) / 2;
        ad_real c = (mid == n - 1 && extra) ? hi : lo + (ad_real)mid * step;
        if (c > hi) c = hi;
        if (fits(c, user)) { best = c; found = 1; left = mid + 1; }
        else right = mid - 1;
    }
    return found ? best : lo;
}

/* ── Facilitators ───────────────────────────────────────────────────────────────── */
AD_INLINE ad_real ad_pick_orientation(const ad_metrics *m, ad_real portrait, ad_real landscape) {
    return m->is_landscape ? landscape : portrait;
}
AD_INLINE ad_real ad_pick_mode(const ad_metrics *m, ad_real value, ad_ui_mode mode, ad_real mode_value) {
    return m->in.ui_mode == mode ? mode_value : value;
}
AD_INLINE ad_real ad_pick_min_sw(const ad_metrics *m, ad_real value, ad_real min_sw, ad_real large_value) {
    return m->sw >= min_sw ? large_value : value;
}

#ifdef __cplusplus
} /* extern "C" */
#endif
#endif /* APPDIMENS_H */
