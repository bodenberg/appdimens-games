# Upscaling, render resolution and dynamic resolution

**Rule #1:** render the 3D scene at the *render* resolution, but size and draw the HUD/UI at the *display* (output)
resolution — build the AppDimens snapshot from the display size, never from the render size.

## Presets (per-axis display/render ratio)

| Preset | FSR 2/3 | DLSS | XeSS 1.3+ | FSR 1 | MetalFX / TSR / PSSR |
|--------|---------|------|-----------|-------|----------------------|
| Native AA / DLAA | 1.0 | 1.0 | 1.0 | – | 1.0 |
| Ultra Quality+ | – | – | 1.3 | – | – |
| Ultra Quality | – | – | 1.5 | 1.3 | 1.3 (MetalFX) |
| Quality | 1.5 | 1.5 | 1.7 | 1.5 | 1.5 |
| Balanced | 1.7 | 1.724 | 2.0 | 1.7 | 1.7 |
| Performance | 2.0 | 2.0 | 2.3 | 2.0 | 2.0 |
| Ultra Performance | 3.0 | 3.0 | 3.0 | – | 3.0 |

Unsupported presets fall back to the nearest offered one (`adg_upscale_ratio`). Prefer the vendor query when you link the
SDK (e.g. `ffxQuery…GetRenderResolutionFromQualityMode`, NGX optimal settings); these reproduce the published ratios —
e.g. DLSS Balanced at 4K → 2227×1253.

## Mip bias and jitter

- `adg_upscale_mip_bias()` — `log2(render/display) − 1` for temporal upscalers (FSR 2/3, DLSS, TSR, MetalFX, PSSR),
  `log2(render/display)` for spatial / XeSS. Apply to material samplers (or `texture_mipmap_bias`).
- `adg_upscale_jitter(frame, render_w, render_h, display_w)` — Halton(2,3) with `8 × (display/render)²` phases (FSR/DLSS
  recommendation): `px/py` for the upscaler dispatch, `clip_x/clip_y` to add to `proj[8]`/`proj[9]` (column-major).

## Dynamic resolution

```c
adg_dynres d; adg_dynres_init(&d, 16.6f, 0.5f, 1.0f);   // 60 FPS, 50–100 % per axis
float scale = adg_dynres_update(&d, last_gpu_ms);        // cost ∝ scale²; damped, quantised to 1/64
adg_render_size r = adg_render_size_for_scale(display_w, display_h, scale);
```

Drops fast when over budget, recovers slowly, never thrashes allocations (1/64 steps). Unity: `AppDimensDynamicResolution`
(FrameTimingManager → ScalableBufferManager). Godot: feed `scaling_3d_scale`. Unreal: `r.ScreenPercentage` /
`r.DynamicRes.*`.

## Engine wiring

| Engine | Call |
|--------|------|
| Unity | `AppDimensUpscaling.ApplyToPipeline(Upscaler.Dlss, UpscaleQuality.Quality)` (URP `renderScale`) |
| Unreal | `UAppDimensLibrary::ApplyUpscalerPreset(TSR, Quality)` (`r.ScreenPercentage`, TSR AA method) |
| Godot | `AppDimensGames.configure_3d_scaling(viewport, FSR2, QUALITY)` (`scaling_3d_mode`/`scale`) |
| Custom | `adg_upscale_render_size` + `adg_upscale_jitter` + `adg_upscale_mip_bias` |
