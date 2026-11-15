/*
 * AppDimens Games — portable game layer (C99, header-only, C++ compatible).
 * Version 3.3.0 · Apache-2.0 · https://github.com/bodenberg/appdimens-games
 *
 * Built on the AppDimens core (appdimens.h): same tokens, strategies and results as every
 * AppDimens library. This header adds what games need on every platform and API:
 *
 *   - Surface → metrics in three scale modes (reference resolution, OS content scale, DPI)
 *   - Viewports: fit / fill / stretch / fit-width / fit-height / expand / integer (pixel art)
 *     and native viewport + projection structs for OpenGL(ES), Vulkan, Direct3D 11/12, Metal,
 *     WebGPU and WebGL (origin, Y direction and depth range handled per API)
 *   - Screen ↔ design ↔ world mapping for input (mouse, touch, gamepad cursor)
 *   - Camera: Hor+ / Vert- / hybrid FOV for 4:3 → 16:9 → 21:9 → 32:9 and portrait, 2D
 *     orthographic size, FOV-consistent mouse/ADS sensitivity
 *   - Upscalers: FSR 1/2/3, DLSS, XeSS, MetalFX, TSR, PSSR, SGSR presets → render resolution,
 *     mip bias, Halton jitter (phase count, pixel + clip offsets), dynamic resolution
 *   - HUD: safe area, TV overscan, ultrawide HUD band, minimum text and touch sizes
 *   - Open world / 3D: resolution- and FOV-independent LOD distances, projected size
 *
 * Everything is static inline, allocation-free and deterministic. Live snapshot publishing
 * (thread-safe) is in appdimens/games_live.h / the compiled C ABI (appdimens_games_api.h).
 */
#ifndef APPDIMENS_GAMES_H
#define APPDIMENS_GAMES_H

#include "appdimens.h"

#ifndef AD_NO_LIBM
#include <math.h>
#define adg__tanf(x)  ((float)tan((double)(x)))
#define adg__atanf(x) ((float)atan((double)(x)))
#define adg__log2f(x) ((float)(log((double)(x)) / 0.6931471805599453))
#define adg__floorf(x) ((float)floor((double)(x)))
#define adg__ceilf(x) ((float)ceil((double)(x)))
#else
#error "appdimens/games.h needs libm (tan/atan/log/floor). Define AD_NO_LIBM only for appdimens.h."
#endif

#ifdef __cplusplus
extern "C" {
#endif

#define ADG_VERSION_STRING "3.3.0"
#define ADG_PI 3.14159265358979323846f
#define ADG_DEG2RAD (ADG_PI / 180.0f)
#define ADG_RAD2DEG (180.0f / ADG_PI)

/* ════════════════════════════════════════════════════════════════════════════════
 * 1. Surface → metrics
 * ════════════════════════════════════════════════════════════════════════════════ */
typedef enum adg_scale_mode {
    /** Games default. Design values are pixels at a reference resolution (e.g. 1080p):
     *  density = reference_short_side / 300, so every strategy is identity at the reference.
     *  In this mode the plain tokens (sdp, asdp, ftsdp… = ad_dp / adg_ref_px) already return
     *  FINAL PIXELS for values given in reference pixels — do not use the Px tokens.
     *  1080p reference: 4K → sdp ×2, asdp ×1.73 (calmer), 720p → ×0.667. */
    ADG_SCALE_REFERENCE = 0,
    /** Desktop-app style: density = OS content scale (Windows DPI/96, macOS backing scale,
     *  Wayland fractional scale, Android density, iOS scale). */
    ADG_SCALE_CONTENT = 1,
    /** Android dp: density = physical DPI / 160 (handhelds and phones with known DPI). */
    ADG_SCALE_DPI = 2
} adg_scale_mode;

typedef struct adg_surface {
    float width_px, height_px;   /* drawable / swapchain size in physical pixels          */
    adg_scale_mode mode;
    float param;                 /* REFERENCE: short side px of the reference (0 → 1080)  *
                                  * CONTENT: OS content scale (0 → 1)                      *
                                  * DPI: physical DPI (0 → 160)                            */
    float dpi;                   /* physical DPI if known (mm/inch helpers), 0 → derived  */
    float font_scale;            /* accessibility text scale, 0 → 1                       */
    int multi_window;            /* windowed/split (drives `i` tokens)                    */
    ad_ui_mode ui_mode;          /* AD_UI_TELEVISION for 10-foot UIs, AD_UI_VR_HEADSET…   */
} adg_surface;

AD_INLINE void adg_surface_default(adg_surface *s, float width_px, float height_px) {
    s->width_px = width_px; s->height_px = height_px; s->mode = ADG_SCALE_REFERENCE; s->param = 0;
    s->dpi = 0; s->font_scale = 0; s->multi_window = 0; s->ui_mode = AD_UI_NORMAL;
}

/** Density (px per logical unit) that the surface implies. */
AD_FN float adg_surface_density(const adg_surface *s) {
    switch (s->mode) {
    case ADG_SCALE_CONTENT: return s->param > 0 ? s->param : 1.0f;
    case ADG_SCALE_DPI: return (s->param > 0 ? s->param : 160.0f) / 160.0f;
    case ADG_SCALE_REFERENCE:
    default: return (s->param > 0 ? s->param : 1080.0f) / 300.0f;
    }
}

/** Builds the snapshot. Use `Px` tokens / ad_px() to get final pixels. */
AD_FN void adg_metrics_from_surface(ad_metrics *m, const adg_surface *s) {
    ad_input in;
    ad_input_default(&in);
    const float d = adg_surface_density(s);
    in.width = (ad_real)(s->width_px / d);
    in.height = (ad_real)(s->height_px / d);
    in.density = (ad_real)d;
    in.font_scale = (ad_real)(s->font_scale > 0 ? s->font_scale : 1.0f);
    in.dpi = (ad_real)(s->dpi > 0 ? s->dpi : (s->mode == ADG_SCALE_DPI && s->param > 0 ? s->param : 160.0f * d));
    in.multi_window = s->multi_window;
    in.ui_mode = s->ui_mode;
    ad_metrics_init(m, &in);
}

/** REFERENCE mode: `value` in pixels at the reference resolution → final pixels now. */
AD_INLINE float adg_ref_px(const ad_metrics *m, ad_strategy strategy, ad_real value) {
    return (float)ad_dp(m, strategy, AD_SLOT(AD_VAR_S, 0), value);
}
/** REFERENCE mode text: × accessibility text scale. */
AD_INLINE float adg_ref_text_px(const ad_metrics *m, ad_strategy strategy, ad_real value) {
    return (float)ad_sp(m, strategy, AD_SLOT(AD_VAR_S, 0), value);
}
/** CONTENT / DPI modes: `value` in dp/pt → final pixels (× density). */
AD_INLINE float adg_dp_to_px(const ad_metrics *m, ad_strategy strategy, ad_real value) {
    return (float)ad_px(m, strategy, AD_SLOT(AD_VAR_S, 0), value);
}

/* ════════════════════════════════════════════════════════════════════════════════
 * 2. Viewports
 * ════════════════════════════════════════════════════════════════════════════════ */
typedef enum adg_viewport_mode {
    ADG_VIEW_FIT = 0,        /* letterbox/pillarbox: whole design visible            */
    ADG_VIEW_FILL,           /* cover: no bars, edges cropped                         */
    ADG_VIEW_STRETCH,        /* non-uniform                                           */
    ADG_VIEW_FIT_WIDTH,      /* design width always fills                             */
    ADG_VIEW_FIT_HEIGHT,     /* design height always fills                            */
    ADG_VIEW_EXPAND,         /* uniform fit scale, world grows on the long axis (Godot "expand", no bars) */
    ADG_VIEW_INTEGER         /* pixel-perfect: largest integer scale ≥ 1 that fits, centred */
} adg_viewport_mode;

typedef struct adg_viewport {
    float x, y, w, h;          /* rectangle in surface px, origin top-left, Y down   */
    float scale_x, scale_y;    /* surface px per design unit                          */
    float world_x, world_y;    /* visible design-space origin (EXPAND/FILL ≠ 0)       */
    float world_w, world_h;    /* visible design-space size                           */
} adg_viewport;

AD_FN adg_viewport adg_viewport_compute(adg_viewport_mode mode, float surface_w, float surface_h,
                                        float design_w, float design_h) {
    adg_viewport v;
    const float sw = surface_w > 1.0f ? surface_w : 1.0f, sh = surface_h > 1.0f ? surface_h : 1.0f;
    const float dw = design_w > 0.0f ? design_w : 1.0f, dh = design_h > 0.0f ? design_h : 1.0f;
    const float fx = sw / dw, fy = sh / dh;
    float s = fx < fy ? fx : fy;
    v.world_x = 0; v.world_y = 0; v.world_w = dw; v.world_h = dh;
    switch (mode) {
    case ADG_VIEW_FILL:
        s = fx > fy ? fx : fy;
        v.w = dw * s; v.h = dh * s; v.x = (sw - v.w) * 0.5f; v.y = (sh - v.h) * 0.5f;
        v.scale_x = v.scale_y = s;
        v.world_w = sw / s; v.world_h = sh / s;
        v.world_x = (dw - v.world_w) * 0.5f; v.world_y = (dh - v.world_h) * 0.5f;
        break;
    case ADG_VIEW_STRETCH:
        v.x = 0; v.y = 0; v.w = sw; v.h = sh; v.scale_x = fx; v.scale_y = fy;
        break;
    case ADG_VIEW_FIT_WIDTH:
        v.scale_x = v.scale_y = fx; v.w = sw; v.h = dh * fx; v.x = 0; v.y = (sh - v.h) * 0.5f;
        v.world_h = sh / fx; v.world_y = (dh - v.world_h) * 0.5f; v.y = 0; v.h = sh;
        break;
    case ADG_VIEW_FIT_HEIGHT:
        v.scale_x = v.scale_y = fy; v.h = sh; v.w = sw; v.x = 0; v.y = 0;
        v.world_w = sw / fy; v.world_x = (dw - v.world_w) * 0.5f;
        break;
    case ADG_VIEW_EXPAND:
        v.scale_x = v.scale_y = s; v.x = 0; v.y = 0; v.w = sw; v.h = sh;
        v.world_w = sw / s; v.world_h = sh / s;
        v.world_x = (dw - v.world_w) * 0.5f; v.world_y = (dh - v.world_h) * 0.5f;
        break;
    case ADG_VIEW_INTEGER: {
        float k = adg__floorf(s + 1e-4f);
        if (k < 1.0f) k = 1.0f;
        v.scale_x = v.scale_y = k; v.w = dw * k; v.h = dh * k;
        v.x = adg__floorf((sw - v.w) * 0.5f); v.y = adg__floorf((sh - v.h) * 0.5f);
        break;
    }
    case ADG_VIEW_FIT:
    default:
        v.scale_x = v.scale_y = s; v.w = dw * s; v.h = dh * s; v.x = (sw - v.w) * 0.5f; v.y = (sh - v.h) * 0.5f;
        break;
    }
    return v;
}

/** Surface px (top-left origin) → design units (e.g. mouse/touch → game coordinates). */
AD_INLINE void adg_screen_to_design(const adg_viewport *v, float sx, float sy, float *dx, float *dy) {
    *dx = v->world_x + (sx - (v->x > 0 ? v->x : 0)) / v->scale_x;
    *dy = v->world_y + (sy - (v->y > 0 ? v->y : 0)) / v->scale_y;
}
/** Design units → surface px. */
AD_INLINE void adg_design_to_screen(const adg_viewport *v, float dx, float dy, float *sx, float *sy) {
    *sx = (v->x > 0 ? v->x : 0) + (dx - v->world_x) * v->scale_x;
    *sy = (v->y > 0 ? v->y : 0) + (dy - v->world_y) * v->scale_y;
}
/** True when a surface point lies inside the drawn area (not on letterbox bars). */
AD_INLINE int adg_viewport_contains(const adg_viewport *v, float sx, float sy) {
    const float x0 = v->x > 0 ? v->x : 0, y0 = v->y > 0 ? v->y : 0;
    const float x1 = v->x > 0 ? v->x + v->w : v->x + v->w, y1 = v->y > 0 ? v->y + v->h : v->y + v->h;
    return sx >= x0 && sy >= y0 && sx < x1 && sy < y1;
}

/* ── Graphics APIs ─────────────────────────────────────────────────────────────── */
typedef enum adg_graphics_api {
    ADG_GFX_OPENGL = 0, ADG_GFX_OPENGL_ES, ADG_GFX_WEBGL, ADG_GFX_VULKAN,
    ADG_GFX_D3D11, ADG_GFX_D3D12, ADG_GFX_METAL, ADG_GFX_WEBGPU
} adg_graphics_api;

/** Framebuffer origin at the bottom-left (glViewport / glScissor y). */
AD_INLINE int adg_gfx_origin_bottom_left(adg_graphics_api a) { return a <= ADG_GFX_WEBGL; }
/** Clip-space depth in [-1, 1] (GL family) instead of [0, 1]. */
AD_INLINE int adg_gfx_depth_minus_one(adg_graphics_api a) { return a <= ADG_GFX_WEBGL; }
/** NDC +Y points down (Vulkan without a negative-height viewport). */
AD_INLINE int adg_gfx_ndc_y_down(adg_graphics_api a) { return a == ADG_GFX_VULKAN; }

/** glViewport / glScissor arguments (integers, y from the bottom). */
typedef struct adg_gl_rect { int32_t x, y, width, height; } adg_gl_rect;
/** Field-compatible with VkViewport, D3D11_VIEWPORT, D3D12_VIEWPORT and WebGPU setViewport(). */
typedef struct adg_viewport_f { float x, y, width, height, min_depth, max_depth; } adg_viewport_f;
/** Field-compatible with MTLViewport (doubles). */
typedef struct adg_mtl_viewport { double origin_x, origin_y, width, height, znear, zfar; } adg_mtl_viewport;
/** Field-compatible with VkRect2D, D3D12_RECT/RECT (as l,t,r,b see adg_rect_ltrb) and MTLScissorRect (as uint). */
typedef struct adg_rect_i { int32_t x, y, width, height; } adg_rect_i;
typedef struct adg_rect_ltrb { int32_t left, top, right, bottom; } adg_rect_ltrb;

AD_INLINE int32_t adg__ri(float v) { return (int32_t)(v >= 0 ? v + 0.5f : v - 0.5f); }

/** Integer drawable rect clipped to the surface (Crop/Fill viewports extend outside). */
AD_FN adg_rect_i adg_viewport_pixels(const adg_viewport *v, float surface_w, float surface_h) {
    adg_rect_i r;
    float x0 = v->x, y0 = v->y, x1 = v->x + v->w, y1 = v->y + v->h;
    if (x0 < 0) x0 = 0;
    if (y0 < 0) y0 = 0;
    if (x1 > surface_w) x1 = surface_w;
    if (y1 > surface_h) y1 = surface_h;
    r.x = adg__ri(x0); r.y = adg__ri(y0); r.width = adg__ri(x1) - r.x; r.height = adg__ri(y1) - r.y;
    return r;
}

AD_FN adg_gl_rect adg_gl_viewport(const adg_viewport *v, float surface_w, float surface_h) {
    /* GL uses the full (possibly off-surface) rect so FILL crops correctly; y flipped. */
    adg_gl_rect r;
    r.x = adg__ri(v->x);
    r.width = adg__ri(v->w);
    r.height = adg__ri(v->h);
    r.y = adg__ri(surface_h - (v->y + v->h));
    (void)surface_w;
    return r;
}

/** Vulkan / D3D11 / D3D12 / WebGPU viewport (top-left origin). Vulkan: pass flip_y = 1 to use
 *  the VK_KHR_maintenance1 negative-height trick so +Y is up like the other APIs. */
AD_FN adg_viewport_f adg_native_viewport(const adg_viewport *v, int flip_y) {
    adg_viewport_f r;
    r.x = v->x; r.width = v->w; r.min_depth = 0.0f; r.max_depth = 1.0f;
    if (flip_y) { r.y = v->y + v->h; r.height = -v->h; }
    else { r.y = v->y; r.height = v->h; }
    return r;
}

AD_FN adg_mtl_viewport adg_metal_viewport(const adg_viewport *v) {
    adg_mtl_viewport r;
    r.origin_x = v->x; r.origin_y = v->y; r.width = v->w; r.height = v->h; r.znear = 0.0; r.zfar = 1.0;
    return r;
}

/** Scissor for the visible area, in the API's convention (GL: y from bottom). */
AD_FN adg_rect_i adg_scissor(adg_graphics_api api, const adg_viewport *v, float surface_w, float surface_h) {
    adg_rect_i r = adg_viewport_pixels(v, surface_w, surface_h);
    if (adg_gfx_origin_bottom_left(api)) r.y = adg__ri(surface_h) - (r.y + r.height);
    return r;
}

/**
 * Orthographic projection: design space (left..right, top..bottom, Y down like UI/2D
 * engines) → clip space of `api` (depth range and NDC Y handled). Column-major 4×4
 * (OpenGL / Vulkan / Metal / WebGPU / HLSL `column_major`, the HLSL cbuffer default).
 * Pass row_major = 1 for DirectXMath XMMATRIX / HLSL `row_major`.
 */
AD_FN void adg_ortho(adg_graphics_api api, float left, float top, float right, float bottom, float znear, float zfar,
                     int row_major, float out16[16]) {
    float m[16] = {0};
    const float rl = right - left, bt = bottom - top, fn = zfar - znear;
    m[0] = 2.0f / rl;
    m[12] = -(right + left) / rl;
    /* y: top → +1 (Y-up NDC) or −1 (Vulkan) */
    const float ys = adg_gfx_ndc_y_down(api) ? 1.0f : -1.0f;
    m[5] = ys * 2.0f / bt;
    m[13] = -ys * (bottom + top) / bt;
    if (adg_gfx_depth_minus_one(api)) { m[10] = 2.0f / fn; m[14] = -(zfar + znear) / fn; }
    else { m[10] = 1.0f / fn; m[14] = -znear / fn; }
    m[15] = 1.0f;
    for (int c = 0; c < 4; c++)
        for (int r = 0; r < 4; r++) out16[row_major ? r * 4 + c : c * 4 + r] = m[c * 4 + r];
}

/**
 * Perspective projection (right-handed view space, camera looks down −Z) for `api`.
 * reverse_z = 1 maps near → 1 and far → 0 (recommended for open worlds; [0,1] depth APIs).
 * zfar ≤ 0 → infinite far plane. Column-major unless row_major.
 */
AD_FN void adg_perspective(adg_graphics_api api, float vfov_rad, float aspect, float znear, float zfar,
                           int reverse_z, int row_major, float out16[16]) {
    float m[16] = {0};
    const float f = 1.0f / adg__tanf(vfov_rad * 0.5f);
    m[0] = f / aspect;
    m[5] = adg_gfx_ndc_y_down(api) ? -f : f;
    m[11] = -1.0f;
    const int gl = adg_gfx_depth_minus_one(api);
    if (zfar <= 0.0f) {                                   /* infinite */
        if (gl && !reverse_z) { m[10] = -1.0f; m[14] = -2.0f * znear; }
        else if (reverse_z) { m[10] = 0.0f; m[14] = znear; }
        else { m[10] = -1.0f; m[14] = -znear; }
    } else if (gl && !reverse_z) {
        m[10] = -(zfar + znear) / (zfar - znear); m[14] = -2.0f * zfar * znear / (zfar - znear);
    } else if (reverse_z) {
        m[10] = znear / (zfar - znear); m[14] = zfar * znear / (zfar - znear);
    } else {
        m[10] = zfar / (znear - zfar); m[14] = zfar * znear / (znear - zfar);
    }
    for (int c = 0; c < 4; c++)
        for (int r = 0; r < 4; r++) out16[row_major ? r * 4 + c : c * 4 + r] = m[c * 4 + r];
}

/* ════════════════════════════════════════════════════════════════════════════════
 * 3. Camera: FOV, orthographic size, sensitivity
 * ════════════════════════════════════════════════════════════════════════════════ */
AD_INLINE float adg_hfov_from_vfov(float vfov_rad, float aspect) { return 2.0f * adg__atanf(adg__tanf(vfov_rad * 0.5f) * aspect); }
AD_INLINE float adg_vfov_from_hfov(float hfov_rad, float aspect) { return 2.0f * adg__atanf(adg__tanf(hfov_rad * 0.5f) / aspect); }

typedef enum adg_fov_policy {
    ADG_FOV_HOR_PLUS = 0,   /* keep vertical FOV: wider screens see more (modern PC standard) */
    ADG_FOV_VERT_MINUS,     /* keep horizontal FOV: wider screens see less vertically          */
    ADG_FOV_HYBRID          /* Hor+ when wider than the design aspect, Vert- when narrower
                               (portrait phones, 4:3, Steam Deck 16:10 vs 16:9 design)       */
} adg_fov_policy;

/**
 * Vertical FOV (radians) to use on a screen with `aspect`, given the FOV designed for
 * `design_aspect` (`design_vfov` vertical, radians). `max_hfov_rad` > 0 caps the horizontal
 * FOV (e.g. 2.1 rad ≈ 120° on 32:9) by reducing the vertical FOV.
 */
AD_FN float adg_fov_for_aspect(adg_fov_policy policy, float design_vfov, float design_aspect, float aspect,
                               float max_hfov_rad) {
    const float design_hfov = adg_hfov_from_vfov(design_vfov, design_aspect);
    float v = design_vfov;
    if (policy == ADG_FOV_VERT_MINUS || (policy == ADG_FOV_HYBRID && aspect < design_aspect))
        v = adg_vfov_from_hfov(design_hfov, aspect);
    if (max_hfov_rad > 0.0f && adg_hfov_from_vfov(v, aspect) > max_hfov_rad) v = adg_vfov_from_hfov(max_hfov_rad, aspect);
    return v;
}

/** 2D orthographic half-height (Unity orthographicSize, Godot zoom⁻¹…) keeping the whole
 *  design rect visible (`fill` = 0) or covering the screen (`fill` = 1). */
AD_FN float adg_ortho_half_height(float design_w, float design_h, float aspect, int fill) {
    const float design_aspect = design_w / design_h;
    const int wider = aspect > design_aspect;
    if (fill ? wider : !wider) return 0.5f * design_w / aspect;
    return 0.5f * design_h;
}

/**
 * Mouse / ADS sensitivity multiplier when the FOV changes (zoom, FOV slider, aspect
 * change), by monitor-distance coefficient: 0 = match at the crosshair (focal-length
 * ratio, "0 % MDM"), 0.75 / 1.333 = classic monitor-distance matches, 1 = match at the
 * screen edge. FOVs are horizontal, radians.
 */
AD_FN float adg_sensitivity_scale(float old_hfov, float new_hfov, float coefficient) {
    if (coefficient <= 0.0f) return adg__tanf(new_hfov * 0.5f) / adg__tanf(old_hfov * 0.5f);
    return adg__atanf(coefficient * adg__tanf(new_hfov * 0.5f)) / adg__atanf(coefficient * adg__tanf(old_hfov * 0.5f));
}

/* ════════════════════════════════════════════════════════════════════════════════
 * 4. Upscalers and dynamic resolution
 * ════════════════════════════════════════════════════════════════════════════════ */
typedef enum adg_upscaler {
    ADG_UPSCALER_NONE = 0,
    ADG_UPSCALER_FSR1,       /* AMD FidelityFX Super Resolution 1 (spatial)              */
    ADG_UPSCALER_FSR2,       /* FSR 2.x (temporal)                                       */
    ADG_UPSCALER_FSR3,       /* FSR 3.x / 4 upscaling (temporal; frame gen is separate)  */
    ADG_UPSCALER_DLSS,       /* NVIDIA DLSS Super Resolution 2/3/4                        */
    ADG_UPSCALER_XESS,       /* Intel XeSS 1.3+                                          */
    ADG_UPSCALER_METALFX,    /* Apple MetalFX temporal                                   */
    ADG_UPSCALER_TSR,        /* Unreal Temporal Super Resolution                          */
    ADG_UPSCALER_PSSR,       /* PlayStation Spectral Super Resolution                     */
    ADG_UPSCALER_SGSR,       /* Snapdragon Game Super Resolution (mobile, spatial)        */
    ADG_UPSCALER_COUNT
} adg_upscaler;

typedef enum adg_upscale_quality {
    ADG_Q_NATIVE_AA = 0,     /* DLAA / FSR Native AA / XeSS Native AA: 1.0×   */
    ADG_Q_ULTRA_QUALITY_PLUS,
    ADG_Q_ULTRA_QUALITY,
    ADG_Q_QUALITY,
    ADG_Q_BALANCED,
    ADG_Q_PERFORMANCE,
    ADG_Q_ULTRA_PERFORMANCE,
    ADG_Q_COUNT
} adg_upscale_quality;

/* Published per-axis ratios (display / render); 0 = preset not offered by that vendor. */
static const float adg__upscale_ratio[ADG_UPSCALER_COUNT][ADG_Q_COUNT] = {
    /* NONE    */ {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f},
    /* FSR1    */ {0.0f, 0.0f, 1.3f, 1.5f, 1.7f, 2.0f, 0.0f},
    /* FSR2    */ {1.0f, 0.0f, 0.0f, 1.5f, 1.7f, 2.0f, 3.0f},
    /* FSR3    */ {1.0f, 0.0f, 0.0f, 1.5f, 1.7f, 2.0f, 3.0f},
    /* DLSS    */ {1.0f, 0.0f, 0.0f, 1.5f, 1.7241379f, 2.0f, 3.0f},
    /* XESS    */ {1.0f, 1.3f, 1.5f, 1.7f, 2.0f, 2.3f, 3.0f},
    /* METALFX */ {1.0f, 0.0f, 1.3f, 1.5f, 1.7f, 2.0f, 3.0f},
    /* TSR     */ {1.0f, 0.0f, 0.0f, 1.5f, 1.7f, 2.0f, 3.0f},
    /* PSSR    */ {1.0f, 0.0f, 0.0f, 1.5f, 1.7f, 2.0f, 3.0f},
    /* SGSR    */ {1.0f, 0.0f, 1.3f, 1.5f, 1.7f, 2.0f, 0.0f},
};

/** Per-axis display/render ratio. Unsupported presets fall back to the nearest offered one. */
AD_FN float adg_upscale_ratio(adg_upscaler u, adg_upscale_quality q) {
    if ((unsigned)u >= ADG_UPSCALER_COUNT) u = ADG_UPSCALER_NONE;
    if ((unsigned)q >= ADG_Q_COUNT) q = ADG_Q_QUALITY;
    for (int d = 0; d < ADG_Q_COUNT; d++) {
        const int a = (int)q - d, b = (int)q + d;
        if (a >= 0 && adg__upscale_ratio[u][a] > 0.0f) return adg__upscale_ratio[u][a];
        if (b < ADG_Q_COUNT && adg__upscale_ratio[u][b] > 0.0f) return adg__upscale_ratio[u][b];
    }
    return 1.0f;
}

typedef struct adg_render_size { uint32_t width, height; float scale; } adg_render_size;

/** Render resolution for a display size and per-axis scale (0 < scale ≤ 1, e.g. 1/ratio). */
AD_FN adg_render_size adg_render_size_for_scale(uint32_t display_w, uint32_t display_h, float scale) {
    adg_render_size r;
    if (!(scale > 0.0f)) scale = 1.0f;
    if (scale > 1.0f) scale = 1.0f;
    r.width = (uint32_t)(display_w * scale + 0.5f);
    r.height = (uint32_t)(display_h * scale + 0.5f);
    if (r.width < 1) r.width = 1;
    if (r.height < 1) r.height = 1;
    r.scale = scale;
    return r;
}

/** Render resolution for a vendor preset. Prefer the vendor query (e.g. ffxFsr3GetRenderResolutionFromQualityMode,
 *  NGX optimal settings) when available; this reproduces their published ratios. */
AD_INLINE adg_render_size adg_upscale_render_size(adg_upscaler u, adg_upscale_quality q, uint32_t display_w, uint32_t display_h) {
    return adg_render_size_for_scale(display_w, display_h, 1.0f / adg_upscale_ratio(u, q));
}

/** Texture LOD (mip) bias for the render/display ratio: temporal upscalers use
 *  log2(render/display) − 1 (FSR 2/3, DLSS, TSR, MetalFX, PSSR); spatial / XeSS use log2(render/display). */
AD_FN float adg_upscale_mip_bias(adg_upscaler u, uint32_t render_w, uint32_t display_w) {
    if (u == ADG_UPSCALER_NONE || render_w == 0 || display_w == 0 || render_w >= display_w) return 0.0f;
    const float b = adg__log2f((float)render_w / (float)display_w);
    const int temporal = u == ADG_UPSCALER_FSR2 || u == ADG_UPSCALER_FSR3 || u == ADG_UPSCALER_DLSS ||
                         u == ADG_UPSCALER_TSR || u == ADG_UPSCALER_METALFX || u == ADG_UPSCALER_PSSR;
    return temporal ? b - 1.0f : b;
}

/** Halton low-discrepancy sequence value for `index` (≥ 1) in `base`. */
AD_FN float adg_halton(int32_t index, int32_t base) {
    float f = 1.0f, r = 0.0f;
    while (index > 0) { f /= (float)base; r += f * (float)(index % base); index /= base; }
    return r;
}

/** Jitter phase count for temporal upscalers: 8 × (display/render)², as FSR 2/3 and DLSS recommend. */
AD_FN int32_t adg_jitter_phase_count(uint32_t render_w, uint32_t display_w) {
    if (render_w == 0) return 8;
    const float r = (float)display_w / (float)render_w;
    const int32_t n = (int32_t)(8.0f * r * r);
    return n < 1 ? 1 : n;
}

typedef struct adg_jitter {
    float px, py;          /* sub-pixel offset in render pixels, [-0.5, 0.5)               */
    float clip_x, clip_y;  /* offset to add to projection[2][0] / [2][1] (clip space)       */
} adg_jitter;

/** Jitter for frame `frame_index` (Halton 2,3), matching ffxFsr*GetJitterOffset. Add clip_x/clip_y
 *  to the projection matrix's third column (m[8], m[9] column-major) and pass px/py to the upscaler. */
AD_FN adg_jitter adg_upscale_jitter(uint32_t frame_index, uint32_t render_w, uint32_t render_h, uint32_t display_w) {
    adg_jitter j;
    const int32_t phases = adg_jitter_phase_count(render_w, display_w);
    const int32_t i = (int32_t)(frame_index % (uint32_t)phases) + 1;
    j.px = adg_halton(i, 2) - 0.5f;
    j.py = adg_halton(i, 3) - 0.5f;
    j.clip_x = 2.0f * j.px / (float)(render_w ? render_w : 1);
    j.clip_y = -2.0f * j.py / (float)(render_h ? render_h : 1);
    return j;
}

/** Dynamic resolution controller (GPU-time driven, damped, quantised to avoid churn). */
typedef struct adg_dynres {
    float target_ms;       /* frame budget, e.g. 16.6 for 60 FPS                       */
    float min_scale, max_scale; /* per-axis bounds, e.g. 0.5 .. 1.0                    */
    float smoothing;       /* 0..1 EMA on GPU time (default 0.2)                       */
    float headroom;        /* aim below budget, e.g. 0.92                              */
    float step;            /* quantisation, e.g. 1/64                                  */
    float scale;           /* current per-axis scale (output)                          */
    float gpu_ms_avg;
} adg_dynres;

AD_INLINE void adg_dynres_init(adg_dynres *d, float target_ms, float min_scale, float max_scale) {
    d->target_ms = target_ms; d->min_scale = min_scale; d->max_scale = max_scale; d->smoothing = 0.2f;
    d->headroom = 0.92f; d->step = 1.0f / 64.0f; d->scale = max_scale; d->gpu_ms_avg = 0.0f;
}

/** Feeds the last frame's GPU time; returns the per-axis scale to use (cost ∝ scale²). */
AD_FN float adg_dynres_update(adg_dynres *d, float gpu_ms) {
    if (!(gpu_ms > 0.0f)) return d->scale;
    d->gpu_ms_avg = d->gpu_ms_avg > 0.0f ? d->gpu_ms_avg + d->smoothing * (gpu_ms - d->gpu_ms_avg) : gpu_ms;
    const float budget = d->target_ms * d->headroom;
    float want = d->scale * (float)ad_sqrt((ad_real)(budget / d->gpu_ms_avg));
    /* asymmetric response: drop fast when over budget, recover slowly */
    if (want > d->scale) want = d->scale + (want - d->scale) * 0.25f;
    if (want < d->min_scale) want = d->min_scale;
    if (want > d->max_scale) want = d->max_scale;
    const float q = adg__floorf(want / d->step + 0.5f) * d->step;
    if (q - d->scale > d->step * 0.5f || d->scale - q > d->step * 0.5f) d->scale = q;
    if (d->scale < d->min_scale) d->scale = d->min_scale;
    if (d->scale > d->max_scale) d->scale = d->max_scale;
    return d->scale;
}

/* ════════════════════════════════════════════════════════════════════════════════
 * 5. HUD: safe areas, overscan, ultrawide band, legibility
 * ════════════════════════════════════════════════════════════════════════════════ */
typedef struct adg_insets { float left, top, right, bottom; } adg_insets;
typedef struct adg_rect { float x, y, w, h; } adg_rect;

/** Safe rect: OS insets (notch, punch-hole, home indicator, rounded corners) ∪ TV overscan
 *  (`overscan` fraction per side, e.g. 0.05 for the classic 90 % title-safe area). */
AD_FN adg_rect adg_safe_rect(float surface_w, float surface_h, const adg_insets *os, float overscan) {
    adg_rect r;
    const float ox = surface_w * overscan, oy = surface_h * overscan;
    float l = os ? os->left : 0, t = os ? os->top : 0, rt = os ? os->right : 0, b = os ? os->bottom : 0;
    if (l < ox) l = ox;
    if (rt < ox) rt = ox;
    if (t < oy) t = oy;
    if (b < oy) b = oy;
    r.x = l; r.y = t; r.w = surface_w - l - rt; r.h = surface_h - t - b;
    if (r.w < 0) r.w = 0;
    if (r.h < 0) r.h = 0;
    return r;
}

/** Centred HUD band limited to `max_aspect` (e.g. 16/9 on 21:9 / 32:9 so HUD stays in view). */
AD_FN adg_rect adg_hud_band(const adg_rect *safe, float max_aspect) {
    adg_rect r = *safe;
    if (max_aspect > 0.0f && r.h > 0.0f && r.w / r.h > max_aspect) {
        const float w = r.h * max_aspect;
        r.x += (r.w - w) * 0.5f;
        r.w = w;
    }
    return r;
}

typedef enum adg_device_class { ADG_DEVICE_PHONE = 0, ADG_DEVICE_TABLET, ADG_DEVICE_HANDHELD, ADG_DEVICE_DESKTOP,
                                ADG_DEVICE_TV, ADG_DEVICE_VR } adg_device_class;

/** Guideline minimum body-text height in px for a surface height (not a standard: tune per title).
 *  TV/console ≈ 26 px @1080p (10-foot), handheld ≈ 10 px @800p, desktop ≈ 12 px @1080p. */
AD_FN float adg_min_text_px(adg_device_class c, float surface_h_px, const ad_metrics *m) {
    switch (c) {
    case ADG_DEVICE_TV: return surface_h_px * (26.0f / 1080.0f);
    case ADG_DEVICE_HANDHELD: return surface_h_px * (10.0f / 800.0f);
    case ADG_DEVICE_DESKTOP: return surface_h_px * (12.0f / 1080.0f);
    case ADG_DEVICE_VR: return surface_h_px * (20.0f / 1080.0f);
    case ADG_DEVICE_PHONE: case ADG_DEVICE_TABLET: default:
        return m ? (float)(ad_mm_px(m, 2.0f)) : 12.0f; /* ≈ 2 mm cap height on touch devices */
    }
}

/** Minimum comfortable touch target (9 mm ≈ 48 dp / 44 pt) in px. */
AD_INLINE float adg_min_touch_px(const ad_metrics *m) { return (float)ad_mm_px(m, 9.0f); }

/* ════════════════════════════════════════════════════════════════════════════════
 * 6. Open world / 3D: resolution- and FOV-independent LOD
 * ════════════════════════════════════════════════════════════════════════════════ */
/** On-screen height in px of an object of `size` world units at `distance` (vertical FOV, radians). */
AD_INLINE float adg_projected_px(float size, float distance, float vfov_rad, float surface_h_px) {
    return distance > 0.0f ? size * surface_h_px / (2.0f * distance * adg__tanf(vfov_rad * 0.5f)) : 1e30f;
}
/** Distance at which an object of `size` covers `target_px` (LOD/impostor/culling threshold).
 *  Keeps LOD transitions identical across 720p ↔ 4K and FOV sliders when expressed in px. */
AD_INLINE float adg_lod_distance(float size, float target_px, float vfov_rad, float surface_h_px) {
    return target_px > 0.0f ? size * surface_h_px / (2.0f * target_px * adg__tanf(vfov_rad * 0.5f)) : 0.0f;
}
/** Multiplier for authored LOD distances (authored at ref_h px / ref_vfov) on the current screen. */
AD_INLINE float adg_lod_distance_scale(float surface_h_px, float vfov_rad, float ref_h_px, float ref_vfov_rad) {
    return (surface_h_px / ref_h_px) * (adg__tanf(ref_vfov_rad * 0.5f) / adg__tanf(vfov_rad * 0.5f));
}

/* ════════════════════════════════════════════════════════════════════════════════
 * 7. Pixel art helpers
 * ════════════════════════════════════════════════════════════════════════════════ */
/** Snaps a coordinate to the device pixel grid of an integer-scaled viewport. */
AD_INLINE float adg_pixel_snap(float v, float scale) { return adg__floorf(v * scale + 0.5f) / scale; }

#ifdef __cplusplus
} /* extern "C" */
#endif
#endif /* APPDIMENS_GAMES_H */
