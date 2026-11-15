// AppDimens Games — compiled C ABI. Apache-2.0.
#include "appdimens/appdimens_games_api.h"

#include <atomic>
#include <cstdlib>
#include <cstring>

#if defined(_WIN32)
#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif
#include <windows.h>
#endif
#if defined(__APPLE__)
#include <TargetConditionals.h>
#endif

namespace {

// Ring of snapshots. Writers fill the next slot under a per-slot seqlock and publish its
// index; readers load the index (acquire). Slots are reused only after 3 more publishes.
constexpr int kRing = 4;
struct Slot {
    std::atomic<uint32_t> seq{0};
    ad_metrics m;
};
Slot g_slots[kRing];
std::atomic<int> g_front{-1};
std::atomic<uint32_t> g_generation{0};
std::atomic<bool> g_writer{false};   // serialises concurrent publishers (rare)
ad_metrics g_default;
std::atomic<bool> g_default_ready{false};

const ad_metrics *default_metrics() {
    if (!g_default_ready.load(std::memory_order_acquire)) {
        ad_input in;
        ad_input_default(&in);
        ad_metrics_init(&g_default, &in);
        g_default_ready.store(true, std::memory_order_release);
    }
    return &g_default;
}

void publish_metrics(const ad_metrics &next) {
    bool expected = false;
    while (!g_writer.compare_exchange_weak(expected, true, std::memory_order_acquire)) expected = false;
    const int front = g_front.load(std::memory_order_relaxed);
    if (front >= 0 && ad_metrics_same(&g_slots[front].m, &next)) {
        g_writer.store(false, std::memory_order_release);
        return;
    }
    const int idx = (front + 1) % kRing;
    Slot &s = g_slots[idx];
    const uint32_t seq = s.seq.load(std::memory_order_relaxed);
    s.seq.store(seq + 1, std::memory_order_relaxed);   // odd: writing
    std::atomic_thread_fence(std::memory_order_release);
    s.m = next;
    s.m.generation = g_generation.load(std::memory_order_relaxed) + 1;
    s.seq.store(seq + 2, std::memory_order_release);   // even: stable
    g_front.store(idx, std::memory_order_release);
    g_generation.fetch_add(1, std::memory_order_release);
    g_writer.store(false, std::memory_order_release);
}

inline const ad_metrics *live() {
    const int f = g_front.load(std::memory_order_acquire);
    return f >= 0 ? &g_slots[f].m : default_metrics();
}

inline bool valid(int32_t s, int32_t slot) {
    return s >= 0 && s < AD_STRATEGY_COUNT && slot >= 0 && slot < AD_SLOT_COUNT;
}

} // namespace

extern "C" {

ADG_API uint32_t adg_version(void) { return 0x030300u; }

ADG_API void adg_publish(const ad_input *input) {
    if (!input) return;
    ad_metrics m;
    ad_metrics_init(&m, input);
    publish_metrics(m);
}

ADG_API void adg_publish_surface(const adg_surface *surface) {
    if (!surface) return;
    ad_metrics m;
    adg_metrics_from_surface(&m, surface);
    publish_metrics(m);
}

ADG_API void adg_publish_px(float w, float h, int32_t mode, float param, float font_scale, int32_t mw) {
    adg_surface s;
    adg_surface_default(&s, w, h);
    s.mode = (adg_scale_mode)mode;
    s.param = param;
    s.font_scale = font_scale;
    s.multi_window = mw;
    adg_publish_surface(&s);
}

ADG_API const ad_metrics *adg_acquire(void) { return live(); }

ADG_API void adg_read(ad_metrics *out) {
    if (!out) return;
    for (;;) {
        const int f = g_front.load(std::memory_order_acquire);
        if (f < 0) { *out = *default_metrics(); return; }
        Slot &s = g_slots[f];
        const uint32_t a = s.seq.load(std::memory_order_acquire);
        if (a & 1u) continue;
        std::memcpy(out, &s.m, sizeof *out);
        std::atomic_thread_fence(std::memory_order_acquire);
        if (s.seq.load(std::memory_order_relaxed) == a) return;
    }
}

ADG_API uint32_t adg_generation(void) { return g_generation.load(std::memory_order_acquire); }

ADG_API float adg_live_factor(int32_t s, int32_t slot) {
    return valid(s, slot) ? (float)live()->table[s][slot] : 1.0f;
}
ADG_API float adg_live_dp(int32_t s, int32_t slot, float v) {
    return valid(s, slot) ? (float)ad_dp(live(), (ad_strategy)s, slot, v) : v;
}
ADG_API float adg_live_px(int32_t s, int32_t slot, float v) {
    return valid(s, slot) ? (float)ad_px(live(), (ad_strategy)s, slot, v) : v;
}
ADG_API float adg_live_ref_px(int32_t s, int32_t slot, float v) { return adg_live_dp(s, slot, v); }
ADG_API float adg_live_sp_px(int32_t s, int32_t slot, float v) {
    return valid(s, slot) ? (float)ad_sp_px(live(), (ad_strategy)s, slot, v) : v;
}
ADG_API float adg_live_resolve(const char *token, float v) {
    ad_token t;
    if (!token || !ad_parse_token(token, &t)) return 0.0f;
    return (float)ad_token_resolve(live(), &t, v);
}
ADG_API int32_t adg_token_slot(const char *token, int32_t *strategy, int32_t *slot, int32_t *kind, int32_t *px) {
    ad_token t;
    if (!token || !ad_parse_token(token, &t)) return 0;
    if (strategy) *strategy = (int32_t)t.strategy;
    if (slot) *slot = t.slot;
    if (kind) *kind = (int32_t)t.kind;
    if (px) *px = t.px;
    return 1;
}
ADG_API float adg_live_dimen(const char *expr, float fallback) {
    ad_real v;
    return (expr && ad_dimen(live(), expr, &v)) ? (float)v : fallback;
}

ADG_API void adg_viewport_get(int32_t mode, float sw, float sh, float dw, float dh, adg_viewport *out) {
    if (out) *out = adg_viewport_compute((adg_viewport_mode)mode, sw, sh, dw, dh);
}
ADG_API float adg_fov(int32_t policy, float dv, float da, float a, float mx) {
    return adg_fov_for_aspect((adg_fov_policy)policy, dv, da, a, mx);
}
ADG_API void adg_render_size_get(int32_t u, int32_t q, uint32_t w, uint32_t h, adg_render_size *out) {
    if (out) *out = adg_upscale_render_size((adg_upscaler)u, (adg_upscale_quality)q, w, h);
}
ADG_API float adg_mip_bias(int32_t u, uint32_t rw, uint32_t dw) { return adg_upscale_mip_bias((adg_upscaler)u, rw, dw); }
ADG_API void adg_jitter_get(uint32_t f, uint32_t rw, uint32_t rh, uint32_t dw, adg_jitter *out) {
    if (out) *out = adg_upscale_jitter(f, rw, rh, dw);
}
ADG_API void adg_ortho_get(int32_t api, float l, float t, float r, float b, float n, float f, int32_t rm, float *o) {
    if (o) adg_ortho((adg_graphics_api)api, l, t, r, b, n, f, rm, o);
}
ADG_API void adg_perspective_get(int32_t api, float v, float a, float n, float f, int32_t rz, int32_t rm, float *o) {
    if (o) adg_perspective((adg_graphics_api)api, v, a, n, f, rz, rm, o);
}

static uint32_t detect_platform() {
    uint32_t p = 0;
#if defined(__EMSCRIPTEN__)
    p |= ADG_PLATFORM_WEB;
#elif defined(_WIN32)
    p |= ADG_PLATFORM_WINDOWS;
    if (HMODULE nt = GetModuleHandleA("ntdll.dll")) {
        if (GetProcAddress(nt, "wine_get_version")) p |= ADG_PLATFORM_WINE;
    }
#elif defined(__ANDROID__)
    p |= ADG_PLATFORM_ANDROID;
#elif defined(__APPLE__)
#if TARGET_OS_TV
    p |= ADG_PLATFORM_TVOS;
#elif TARGET_OS_IPHONE
    p |= ADG_PLATFORM_IOS;
#else
    p |= ADG_PLATFORM_MACOS;
#endif
#elif defined(__linux__) || defined(__FreeBSD__)
    p |= ADG_PLATFORM_LINUX;
#endif
#if !defined(__EMSCRIPTEN__) && !defined(__ANDROID__)
    if (std::getenv("STEAM_COMPAT_DATA_PATH") || std::getenv("STEAM_COMPAT_CLIENT_INSTALL_PATH")) p |= ADG_PLATFORM_PROTON;
    if (p & ADG_PLATFORM_PROTON) p |= ADG_PLATFORM_WINE;
    if (std::getenv("GAMESCOPE_WAYLAND_DISPLAY") || std::getenv("GAMESCOPE_WIDTH")) p |= ADG_PLATFORM_GAMESCOPE;
    const char *deck = std::getenv("SteamDeck");
    if ((deck && deck[0] == '1') || std::getenv("STEAM_DECK")) p |= ADG_PLATFORM_STEAMDECK;
#endif
    return p;
}

ADG_API uint32_t adg_platform(void) {
    static const uint32_t p = detect_platform();
    return p;
}

ADG_API int32_t adg_suggest_device_class(float w, float h, float dpi) {
    const uint32_t p = adg_platform();
    if (p & ADG_PLATFORM_STEAMDECK) return ADG_DEVICE_HANDHELD;
    if (p & ADG_PLATFORM_TVOS) return ADG_DEVICE_TV;
    const float shortest = w < h ? w : h, longest = w < h ? h : w;
    if (dpi > 0) {
        const float diag_in = (float)ad_sqrt((ad_real)(shortest * shortest + longest * longest)) / dpi;
        if (diag_in < 7.5f) return (p & (ADG_PLATFORM_ANDROID | ADG_PLATFORM_IOS)) ? ADG_DEVICE_PHONE : ADG_DEVICE_HANDHELD;
        if (diag_in < 13.5f) return (p & (ADG_PLATFORM_ANDROID | ADG_PLATFORM_IOS)) ? ADG_DEVICE_TABLET : ADG_DEVICE_DESKTOP;
        if (diag_in > 40.0f) return ADG_DEVICE_TV;
    }
    if (p & (ADG_PLATFORM_ANDROID | ADG_PLATFORM_IOS)) return shortest / (dpi > 0 ? dpi / 160.0f : 2.0f) >= 600 ? ADG_DEVICE_TABLET : ADG_DEVICE_PHONE;
    return ADG_DEVICE_DESKTOP;
}

} // extern "C"
