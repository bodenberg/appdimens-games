# Recipes by genre

| Genre | HUD / UI | World / camera | Extras |
|-------|----------|----------------|--------|
| **2D pixel art** | `ftsdp` for HUD, integer font sizes | `ADG_VIEW_INTEGER` viewport, `adg_pixel_snap` | render to a low-res target, upscale integer |
| **2D action / platformer** | `asdp` | `ADG_VIEW_EXPAND` (wider screens see more), `adg_screen_to_design` for input | orthographic size via `adg_ortho_half_height` |
| **Puzzle / board / card** | `sdp`/`ftsdp` | `ADG_VIEW_FIT` (board always whole) | touch targets ≥ `adg_min_touch_px` |
| **3D action / adventure** | `asdp`, safe area | `ADG_FOV_HYBRID` (keep framing from 4:3 to 21:9) | upscaler presets, dynamic resolution |
| **FPS** | crosshair `ftsdp(24)`, HUD band 16:9 on ultrawide | Hor+ with `max_hfov` cap (e.g. 120° on 32:9) | `adg_sensitivity_scale` for ADS/FOV slider (0 % MDM or 75 %/133 %) |
| **Open world / AAA** | `asdp` + text scale option + TV text minimums | reverse-Z + infinite far plane | `adg_lod_distance`/`adg_lod_distance_scale` keep LOD switches identical at 720p/4K and any FOV; DLSS/FSR/XeSS jitter + mip bias |
| **Racing / sim** | `asdp`, gauges with `ftsdp` | triple-screen / 32:9: Hor+ with cap or per-monitor viewports | `adg_viewport_compute` per monitor |
| **Strategy / MOBA / MMO** | dense UI: `sdp` + user UI scale slider (`userScale`) | `ADG_VIEW_EXPAND` | minimap `ftsdp` |
| **Mobile casual** | `sdp`/`ssp` (DPI mode for physical size) | `ADG_VIEW_FILL` backgrounds | notch/home-indicator safe area, split-screen → `i` tokens |
| **Couch / TV / console** | `AD_UI_TELEVISION`, text ≥ 26 px @1080p | — | 5 % overscan safe rect |
| **Handheld PC (Steam Deck, ROG Ally)** | `ADG_DEVICE_HANDHELD` text minimums | Hybrid FOV for 16:10 | Proton/Wine detection, 800p dynamic resolution |
| **VR / XR** | world-space panels sized with `ftsdp` | per-eye projection from the runtime | `ADG_DEVICE_VR` text minimum |
