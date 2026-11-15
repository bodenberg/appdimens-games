# Cross-platform guide

## Scale modes — pick one per game

| Mode | Density | Design unit | Use for |
|------|---------|-------------|---------|
| `ADG_SCALE_REFERENCE` (default) | `reference_short_side / 300` (1080 → 3.6) | **pixels at the reference resolution** | HUD/UI of almost every game: author at 1920×1080, ship anywhere |
| `ADG_SCALE_CONTENT` | OS content scale (Windows DPI/96, macOS backing scale, Wayland fractional, Android density, iOS scale) | dp / pt | desktop-app-like tools, launchers, editors |
| `ADG_SCALE_DPI` | physical DPI / 160 | Android dp | handheld/phone UIs that must match physical size |

In **REFERENCE** mode the plain tokens (`sdp`, `asdp`, `ftsdp`…) already return final pixels for values given in
reference pixels (`adg_ref_px`). At the reference every strategy is identity; on 4K `sdp` doubles while `asdp` grows by
×1.73, on 720p both give ×0.67.

## Per platform

| Platform | Surface size | Scale source | Safe area | Notes |
|----------|--------------|--------------|-----------|-------|
| Windows | swapchain / client rect (physical px) | `GetDpiForWindow(hwnd) / 96` (declare Per-Monitor-V2 awareness) | — | Exclusive/borderless fullscreen: treat as `multi_window = 0` |
| Proton / Wine | same as Windows | Wine reports **96 DPI** unless `LogPixels` is set | — | `adg_platform()` sets `WINE`, `PROTON`; prefer REFERENCE mode so HiDPI Linux desktops still look right |
| Steam Deck | 1280×800 (gamescope) | 1 | — | `ADG_PLATFORM_STEAMDECK`, `adg_suggest_device_class()` → `ADG_DEVICE_HANDHELD`; text ≥ `adg_min_text_px(HANDHELD, 800)` ≈ 10 px |
| Linux X11 / Wayland | drawable size | `Xft.dpi/96` or `wp_fractional_scale_v1` | — | SDL: `SDL_GetWindowSizeInPixels` + `SDL_GetWindowDisplayScale` |
| macOS | drawable size (`CAMetalLayer.drawableSize`) | `backingScaleFactor` | menu bar only | |
| iOS / iPadOS | drawable size | `UIScreen.nativeScale` | `safeAreaInsets` (notch, Dynamic Island, home indicator) | Slide Over / Split View → `multi_window = 1` |
| tvOS / Android TV / consoles | output resolution | 1 | overscan 5 % (`adg_safe_rect(..., 0.05f)`) | `AD_UI_TELEVISION`, `ADG_DEVICE_TV` text ≥ 26 px @1080p |
| Android | surface size | `densityDpi / 160` | `WindowInsets` cutouts | Maven artifacts + NDK; the `.so` exports the C ABI too |
| Web (Emscripten) | canvas drawing buffer | `devicePixelRatio` | `env(safe-area-inset-*)` | link the static library; DOM UIs: webdimens |
| XR | eye buffer size | 1 | — | `ADG_DEVICE_VR`; HUD on world-space quads sized with `ftsdp` |

## Threads

`adg_publish*()` (C ABI) may run on the window thread; render/game threads read with `adg_acquire()` (pointer valid for 3
further publishes) or `adg_read()` (seqlock copy). Verified by a stress test: 14.7 M concurrent reads during 200 k
publishes, 0 torn snapshots (Linux) and 31 M under Wine. The header-only API has no global state at all.
