/*
 * AppDimens Games — compiled C ABI (shared/static library `appdimens_games`).
 * Stable, FFI-friendly entry points for engines and languages that cannot use the
 * header-only API: Unity (P/Invoke), Unreal (C++ module), Godot GDExtension, Rust, Zig,
 * Python ctypes/cffi, Lua/LuaJIT FFI, Java (Panama/JNA), Swift.
 *
 * Thread-safety: adg_publish*() may be called from any thread (e.g. the platform/window
 * thread on resize); readers (render/game/UI threads) are wait-free. adg_acquire() returns
 * a pointer valid for at least 3 further publishes (a frame or more); adg_read() copies
 * a consistent snapshot (seqlock) when you need a strict guarantee.
 */
#ifndef APPDIMENS_GAMES_API_H
#define APPDIMENS_GAMES_API_H

#include "games.h"

#if defined(_WIN32) || defined(__CYGWIN__)
#  if defined(ADG_BUILD_SHARED)
#    define ADG_API __declspec(dllexport)
#  elif defined(ADG_USE_SHARED)
#    define ADG_API __declspec(dllimport)
#  else
#    define ADG_API
#  endif
#elif defined(__GNUC__) || defined(__clang__)
#  define ADG_API __attribute__((visibility("default")))
#else
#  define ADG_API
#endif

#ifdef __cplusplus
extern "C" {
#endif

/** Library version as 0xMMmmpp (3.3.0 → 0x030300). */
ADG_API uint32_t adg_version(void);

/* ── Live snapshot ──────────────────────────────────────────────────────────── */
ADG_API void adg_publish(const ad_input *input);
ADG_API void adg_publish_surface(const adg_surface *surface);
/** Convenience for FFI: surface px, scale mode, mode parameter (see adg_surface). */
ADG_API void adg_publish_px(float width_px, float height_px, int32_t mode, float param, float font_scale,
                            int32_t multi_window);
ADG_API const ad_metrics *adg_acquire(void);
ADG_API void adg_read(ad_metrics *out);
/** Increments on every publish that changed the snapshot (cheap "did it change?" check). */
ADG_API uint32_t adg_generation(void);

/* ── Values (from the live snapshot) ────────────────────────────────────────── */
/** Factor for a strategy/slot (cache it per frame: size = value × factor). */
ADG_API float adg_live_factor(int32_t strategy, int32_t slot);
/** Logical units (dp) / final pixels for a strategy/slot. */
ADG_API float adg_live_dp(int32_t strategy, int32_t slot, float value);
ADG_API float adg_live_px(int32_t strategy, int32_t slot, float value);
/** REFERENCE mode: value in reference pixels → final pixels (= adg_dp). */
ADG_API float adg_live_ref_px(int32_t strategy, int32_t slot, float value);
ADG_API float adg_live_sp_px(int32_t strategy, int32_t slot, float value);
/** Token by name ("sdp", "asspPx", "hdpLw") — 0 if invalid. Parses; cache with adg_token_slot(). */
ADG_API float adg_live_resolve(const char *token, float value);
/** Parses a token once: writes strategy, slot, kind (0 dp, 1 sp, 2 em) and px flag. Returns 1 on success. */
ADG_API int32_t adg_token_slot(const char *token, int32_t *strategy, int32_t *slot, int32_t *kind, int32_t *px);
/** "16sdp" / "14assp" / "5mm" / "50spaceW" in logical units (fallback if invalid). */
ADG_API float adg_live_dimen(const char *expr, float fallback);

/* ── Stateless helpers (re-exported from games.h for FFI) ───────────────────── */
ADG_API void adg_viewport_get(int32_t mode, float surface_w, float surface_h, float design_w, float design_h,
                              adg_viewport *out);
ADG_API float adg_fov(int32_t policy, float design_vfov_rad, float design_aspect, float aspect, float max_hfov_rad);
ADG_API void adg_render_size_get(int32_t upscaler, int32_t quality, uint32_t display_w, uint32_t display_h,
                                 adg_render_size *out);
ADG_API float adg_mip_bias(int32_t upscaler, uint32_t render_w, uint32_t display_w);
ADG_API void adg_jitter_get(uint32_t frame, uint32_t render_w, uint32_t render_h, uint32_t display_w, adg_jitter *out);
ADG_API void adg_ortho_get(int32_t api, float l, float t, float r, float b, float n, float f, int32_t row_major,
                           float *out16);
ADG_API void adg_perspective_get(int32_t api, float vfov_rad, float aspect, float n, float f, int32_t reverse_z,
                                 int32_t row_major, float *out16);

/* ── Platform detection ─────────────────────────────────────────────────────── */
#define ADG_PLATFORM_WINDOWS   (1u << 0)
#define ADG_PLATFORM_LINUX     (1u << 1)
#define ADG_PLATFORM_MACOS     (1u << 2)
#define ADG_PLATFORM_IOS       (1u << 3)
#define ADG_PLATFORM_ANDROID   (1u << 4)
#define ADG_PLATFORM_WEB       (1u << 5)
#define ADG_PLATFORM_WINE      (1u << 6)   /* Wine or Proton (Windows build running on Linux/macOS) */
#define ADG_PLATFORM_PROTON    (1u << 7)   /* Steam Proton specifically                              */
#define ADG_PLATFORM_STEAMDECK (1u << 8)   /* Steam Deck / SteamOS handheld (gamescope)              */
#define ADG_PLATFORM_GAMESCOPE (1u << 9)
#define ADG_PLATFORM_TVOS      (1u << 10)
#define ADG_PLATFORM_CONSOLE   (1u << 11)  /* set by the integrator on console SDKs                  */
/** Bitmask of ADG_PLATFORM_* for the running process (cached after the first call). */
ADG_API uint32_t adg_platform(void);
/** Suggested device class from the platform and surface (handheld for Steam Deck…). */
ADG_API int32_t adg_suggest_device_class(float width_px, float height_px, float dpi);

#ifdef __cplusplus
}
#endif
#endif
