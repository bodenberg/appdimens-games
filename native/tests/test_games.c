/* Tests for appdimens/games.h — viewports, projections, FOV, upscalers, HUD, LOD. */
#include <math.h>
#include <stdio.h>
#include "appdimens/games.h"

static int fails = 0, checks = 0;
#define NEAR(got, want, tol, what) do { double g_ = (got), w_ = (want); checks++; \
    if (fabs(g_ - w_) > (tol)) { printf("FAIL %s:%d %s: got %.6f want %.6f\n", __FILE__, __LINE__, what, g_, w_); fails++; } } while (0)
#define TRUE_(c, what) do { checks++; if (!(c)) { printf("FAIL %s:%d %s\n", __FILE__, __LINE__, what); fails++; } } while (0)

static void mul4(const float m[16], const float v[4], float o[4]) { /* column-major */
    for (int r = 0; r < 4; r++) o[r] = m[0 * 4 + r] * v[0] + m[1 * 4 + r] * v[1] + m[2 * 4 + r] * v[2] + m[3 * 4 + r] * v[3];
}

static void test_surface(void) {
    adg_surface s; ad_metrics m;
    adg_surface_default(&s, 1920, 1080);
    adg_metrics_from_surface(&m, &s);
    NEAR(adg_ref_px(&m, AD_SCALED, 64), 64, 1e-3, "ref 1080p identity sdp");
    NEAR(adg_ref_px(&m, AD_AUTO, 64), 64, 1e-3, "ref 1080p identity asdp");
    NEAR(ad_dp(&m, AD_FIT, 0, 100), 100 * fminf(1.0f, (1920 / 3.6f) / 533.0f), 1e-3, "fit at 1080p");
    adg_surface_default(&s, 3840, 2160);
    adg_metrics_from_surface(&m, &s);
    NEAR(adg_ref_px(&m, AD_SCALED, 64), 128, 1e-3, "4K sdp ×2");
    NEAR(adg_ref_px(&m, AD_AUTO, 64), 64 * (1.6 + 0.4 * log(1.4)), 1e-3, "4K asdp calmer");
    adg_surface_default(&s, 1280, 800);   /* Steam Deck, 16:10 */
    adg_metrics_from_surface(&m, &s);
    NEAR(adg_ref_px(&m, AD_SCALED, 64), 64 * 800.0 / 1080.0, 1e-3, "deck sdp");
    s.mode = ADG_SCALE_CONTENT; s.param = 1.5f; s.width_px = 2880; s.height_px = 1620;
    adg_metrics_from_surface(&m, &s);
    NEAR(m.in.width, 1920, 1e-3, "content scale logical width");
    s.mode = ADG_SCALE_DPI; s.param = 320; s.width_px = 1080; s.height_px = 2400;
    adg_metrics_from_surface(&m, &s);
    NEAR(m.in.density, 2.0, 1e-6, "dpi density");
    NEAR(adg_min_touch_px(&m), 9 / 25.4 * 320, 1e-2, "touch 9mm");
}

static void test_viewports(void) {
    adg_viewport v = adg_viewport_compute(ADG_VIEW_FIT, 2560, 1080, 1920, 1080);
    NEAR(v.x, 320, 1e-3, "pillarbox x"); NEAR(v.w, 1920, 1e-3, "pillarbox w"); NEAR(v.scale_x, 1, 1e-6, "fit scale");
    TRUE_(!adg_viewport_contains(&v, 100, 500) && adg_viewport_contains(&v, 400, 500), "contains bars");
    v = adg_viewport_compute(ADG_VIEW_FILL, 2560, 1080, 1920, 1080);
    NEAR(v.scale_x, 2560.0 / 1920.0, 1e-6, "fill scale"); NEAR(v.world_h, 1080 / (2560.0 / 1920.0), 1e-3, "fill world h");
    float dx, dy, sx, sy;
    adg_screen_to_design(&v, 0, 0, &dx, &dy);
    NEAR(dx, 0, 1e-3, "fill origin x"); NEAR(dy, (1080 - v.world_h) / 2, 1e-3, "fill origin y");
    adg_design_to_screen(&v, dx, dy, &sx, &sy);
    NEAR(sx, 0, 1e-3, "round trip x"); NEAR(sy, 0, 1e-3, "round trip y");
    v = adg_viewport_compute(ADG_VIEW_INTEGER, 1366, 768, 320, 180);
    NEAR(v.scale_x, 4, 0, "integer scale"); NEAR(v.x, 43, 0, "integer x"); NEAR(v.y, 24, 0, "integer y");
    v = adg_viewport_compute(ADG_VIEW_INTEGER, 1920, 1080, 320, 180);
    NEAR(v.scale_x, 6, 0, "integer 6x");
    v = adg_viewport_compute(ADG_VIEW_EXPAND, 2560, 1080, 1920, 1080);
    NEAR(v.world_w, 2560, 1e-3, "expand world"); NEAR(v.world_x, -320, 1e-3, "expand origin");
    adg_screen_to_design(&v, 320, 0, &dx, &dy);
    NEAR(dx, 0, 1e-3, "expand map");
    v = adg_viewport_compute(ADG_VIEW_FIT_HEIGHT, 1080, 2400, 1920, 1080);
    NEAR(v.scale_y, 2400.0 / 1080, 1e-5, "fit height");
    /* GL viewport flips y; native keeps top-left; Vulkan flip uses negative height */
    v = adg_viewport_compute(ADG_VIEW_FIT, 1920, 1200, 1920, 1080);
    adg_gl_rect gl = adg_gl_viewport(&v, 1920, 1200);
    TRUE_(gl.x == 0 && gl.y == 60 && gl.width == 1920 && gl.height == 1080, "gl viewport");
    adg_viewport_f vk = adg_native_viewport(&v, 1);
    NEAR(vk.y, 1140, 1e-3, "vk flip y"); NEAR(vk.height, -1080, 1e-3, "vk flip h");
    adg_rect_i sc = adg_scissor(ADG_GFX_OPENGL, &v, 1920, 1200);
    TRUE_(sc.y == 60 && sc.height == 1080, "gl scissor");
    sc = adg_scissor(ADG_GFX_D3D12, &v, 1920, 1200);
    TRUE_(sc.y == 60, "dx scissor");
    adg_mtl_viewport mv = adg_metal_viewport(&v);
    NEAR(mv.origin_y, 60, 1e-9, "metal viewport");
}

static void test_projections(void) {
    float m[16], o[4];
    const adg_graphics_api apis[] = {ADG_GFX_OPENGL, ADG_GFX_VULKAN, ADG_GFX_D3D11, ADG_GFX_METAL, ADG_GFX_WEBGPU};
    for (int i = 0; i < 5; i++) {
        const adg_graphics_api a = apis[i];
        adg_ortho(a, 0, 0, 1920, 1080, 0, 1, 0, m);
        float tl[4] = {0, 0, 0, 1}, br[4] = {1920, 1080, 1, 1};
        mul4(m, tl, o);
        NEAR(o[0], -1, 1e-6, "ortho left");
        NEAR(o[1], adg_gfx_ndc_y_down(a) ? -1 : 1, 1e-6, "ortho top");
        NEAR(o[2], adg_gfx_depth_minus_one(a) ? -1 : 0, 1e-6, "ortho near");
        mul4(m, br, o);
        NEAR(o[0], 1, 1e-6, "ortho right");
        NEAR(o[1], adg_gfx_ndc_y_down(a) ? 1 : -1, 1e-6, "ortho bottom");
        NEAR(o[2], 1, 1e-6, "ortho far");
        for (int rz = 0; rz < 2; rz++) {
            if (rz && adg_gfx_depth_minus_one(a)) continue;
            adg_perspective(a, 1.0471976f, 16.0f / 9.0f, 0.1f, 1000.0f, rz, 0, m);
            float pn[4] = {0, 0, -0.1f, 1}, pf[4] = {0, 0, -1000.0f, 1}, up[4] = {0, 1, -1, 1};
            mul4(m, pn, o);
            NEAR(o[2] / o[3], rz ? 1 : (adg_gfx_depth_minus_one(a) ? -1 : 0), 1e-4, "persp near");
            mul4(m, pf, o);
            NEAR(o[2] / o[3], rz ? 0 : 1, 1e-4, "persp far");
            mul4(m, up, o);
            TRUE_(adg_gfx_ndc_y_down(a) ? o[1] < 0 : o[1] > 0, "persp y direction");
        }
        /* row-major is the transpose */
        float r[16];
        adg_ortho(a, 0, 0, 100, 50, 0, 1, 1, r);
        adg_ortho(a, 0, 0, 100, 50, 0, 1, 0, m);
        TRUE_(r[3] == m[12] && r[7] == m[13], "row-major transpose");
    }
    adg_perspective(ADG_GFX_D3D12, 1.0f, 1.5f, 0.1f, 0, 1, 0, m); /* infinite reverse-Z */
    float far_pt[4] = {0, 0, -1e7f, 1};
    mul4(m, far_pt, o);
    NEAR(o[2] / o[3], 0, 1e-6, "infinite reverse z");
}

static void test_fov(void) {
    const float v60 = 60 * ADG_DEG2RAD;
    NEAR(adg_hfov_from_vfov(v60, 16.0f / 9) * ADG_RAD2DEG, 91.4928, 1e-3, "60v@16:9 = 91.49h");
    NEAR(adg_fov_for_aspect(ADG_FOV_HOR_PLUS, v60, 16.0f / 9, 21.0f / 9, 0), v60, 1e-6, "hor+ keeps v");
    float v = adg_fov_for_aspect(ADG_FOV_HYBRID, v60, 16.0f / 9, 4.0f / 3, 0);
    NEAR(adg_hfov_from_vfov(v, 4.0f / 3), adg_hfov_from_vfov(v60, 16.0f / 9), 1e-5, "hybrid keeps h on 4:3");
    v = adg_fov_for_aspect(ADG_FOV_HOR_PLUS, v60, 16.0f / 9, 32.0f / 9, 120 * ADG_DEG2RAD);
    NEAR(adg_hfov_from_vfov(v, 32.0f / 9) * ADG_RAD2DEG, 120, 1e-3, "hfov cap");
    NEAR(adg_ortho_half_height(1920, 1080, 4.0f / 3, 0), 0.5f * 1920 / (4.0f / 3), 1e-3, "ortho size fit narrow");
    NEAR(adg_ortho_half_height(1920, 1080, 21.0f / 9, 0), 540, 1e-3, "ortho size fit wide");
    NEAR(adg_sensitivity_scale(1.2f, 1.2f, 0), 1, 1e-6, "same fov");
    NEAR(adg_sensitivity_scale(1.6f, 0.8f, 0), tanf(0.4f) / tanf(0.8f), 1e-6, "zoom 0% mdm");
}

static void test_upscalers(void) {
    adg_render_size r = adg_upscale_render_size(ADG_UPSCALER_DLSS, ADG_Q_QUALITY, 3840, 2160);
    TRUE_(r.width == 2560 && r.height == 1440, "dlss quality 4K");
    r = adg_upscale_render_size(ADG_UPSCALER_DLSS, ADG_Q_BALANCED, 3840, 2160);
    TRUE_(r.width == 2227 && r.height == 1253, "dlss balanced 4K");
    r = adg_upscale_render_size(ADG_UPSCALER_FSR3, ADG_Q_PERFORMANCE, 1920, 1080);
    TRUE_(r.width == 960 && r.height == 540, "fsr perf 1080p");
    r = adg_upscale_render_size(ADG_UPSCALER_XESS, ADG_Q_ULTRA_QUALITY_PLUS, 2560, 1440);
    TRUE_(r.width == 1969 && r.height == 1108, "xess uq+");
    NEAR(adg_upscale_ratio(ADG_UPSCALER_FSR1, ADG_Q_NATIVE_AA), 1.3, 1e-6, "fallback nearest");
    NEAR(adg_upscale_ratio(ADG_UPSCALER_FSR2, ADG_Q_ULTRA_QUALITY), 1.5, 1e-6, "fallback fsr2 uq");
    NEAR(adg_upscale_mip_bias(ADG_UPSCALER_DLSS, 1280, 1920), log2(1280.0 / 1920) - 1, 1e-5, "dlss mip");
    NEAR(adg_upscale_mip_bias(ADG_UPSCALER_FSR1, 1280, 1920), log2(1280.0 / 1920), 1e-5, "fsr1 mip");
    NEAR(adg_upscale_mip_bias(ADG_UPSCALER_NONE, 1280, 1920), 0, 0, "no mip");
    TRUE_(adg_jitter_phase_count(960, 1920) == 32, "phase count");
    adg_jitter j = adg_upscale_jitter(0, 960, 540, 1920);
    NEAR(j.px, 0, 1e-6, "halton(1,2)-.5"); NEAR(j.py, 1.0 / 3 - 0.5, 1e-6, "halton(1,3)-.5");
    NEAR(j.clip_y, -2 * j.py / 540, 1e-9, "clip jitter");
    double sx = 0, sy = 0;
    for (uint32_t f = 0; f < 32; f++) { j = adg_upscale_jitter(f, 960, 540, 1920); sx += j.px; sy += j.py; }
    NEAR(sx / 32, 0, 0.03, "jitter mean x"); NEAR(sy / 32, 0, 0.03, "jitter mean y");
    adg_dynres d;
    adg_dynres_init(&d, 16.6f, 0.5f, 1.0f);
    for (int i = 0; i < 400; i++) adg_dynres_update(&d, 25.0f * d.scale * d.scale);
    NEAR(d.scale, sqrt(16.6 * 0.92 / 25), 0.03, "dynres converges");
    float before = d.scale;
    for (int i = 0; i < 50; i++) adg_dynres_update(&d, 25.0f * d.scale * d.scale);
    NEAR(d.scale, before, 1.0 / 64 + 1e-6, "dynres stable");
}

static void test_hud_lod(void) {
    adg_insets os = {0, 47, 0, 34};
    adg_rect r = adg_safe_rect(1179, 2556, &os, 0);
    NEAR(r.y, 47, 0, "notch"); NEAR(r.h, 2556 - 81, 0, "home indicator");
    r = adg_safe_rect(1920, 1080, NULL, 0.05f);
    NEAR(r.x, 96, 1e-3, "overscan x"); NEAR(r.w, 1920 * 0.9, 1e-3, "overscan w");
    adg_rect full = {0, 0, 5120, 1440};
    adg_rect band = adg_hud_band(&full, 16.0f / 9);
    NEAR(band.w, 2560, 1e-3, "hud band 32:9"); NEAR(band.x, 1280, 1e-3, "hud band x");
    NEAR(adg_min_text_px(ADG_DEVICE_TV, 2160, NULL), 52, 1e-3, "tv text 4K");
    const float vf = 1.0f;
    float d = adg_lod_distance(2.0f, 100.0f, vf, 1080);
    NEAR(adg_projected_px(2.0f, d, vf, 1080), 100, 1e-3, "lod inverse");
    NEAR(adg_lod_distance_scale(2160, vf, 1080, vf), 2, 1e-6, "lod 4K scale");
    NEAR(adg_pixel_snap(10.3f, 4), 10.25, 1e-6, "pixel snap");
}

int main(void) {
    test_surface();
    test_viewports();
    test_projections();
    test_fov();
    test_upscalers();
    test_hud_lod();
    printf("games: %d checks, %d failures\n", checks, fails);
    return fails != 0;
}
