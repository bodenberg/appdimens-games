# Engines

## Unity
Install the UPM package (`engines/unity/com.bodenberg.appdimens.games`, Unity 2019.4+). `AppDimensScreen` is created
before the first scene and publishes a snapshot whenever `Screen.width/height/dpi/safeArea/fullScreen` change.

| Component | Setup |
|-----------|-------|
| `AppDimensCanvasScaler` | add to each Canvas; author uGUI at 1920×1080; CanvasScaler → Constant Pixel Size (1) or remove |
| `AppDimensSafeArea` | on a full-stretch RectTransform under the Canvas; overscan 0.05 for TV; `maxAspect` 1.777 to keep HUD in 16:9 on ultrawide |
| `AppDimensCamera` | on the camera: Hybrid FOV policy; orthographic cameras get `orthographicSize` |
| `AppDimensDynamicResolution` | cameras with *Allow Dynamic Resolution* |
| UI Toolkit | set `PanelSettings.scale = Live.Current.Factor(Strategy.Auto, 0)` in `AppDimensScreen.Changed` |

Code: `64f.ASdp()` (every token as an extension method, like AppDimens.Net), `AppDimensScreen.ASdp(64)`, `Live.Current.Resolve("ftsdp", 24)`, `AppDimensUpscaling.RenderScale(...)`.

## Unreal Engine 5
Copy `engines/unreal/AppDimens` to `Plugins/`. Set *User Interface ▸ DPI Scale Rule = Custom* with
`AppDimensDPIScalingRule`; configure *Plugins ▸ AppDimens* (reference short side, strategy). Blueprint nodes live under
*AppDimens* (sizes `Sdp`, `Hdp`, `Wdp`, `ASdp`, `Ssp`, `ASsp`, `LOGSsp`, `FTSdp`, `FLSdp`, `LOGSdp`, any token with
`ResolveToken("hdpLwa", v)`; `HorizontalFovForAspect`, `ApplyUpscalerPreset`, `RenderResolution`, `MipBias`, `LodDistance`).
`UAppDimensSubsystem::OnChanged` fires on game-viewport resize. Note: UE camera FOV is horizontal; the helper converts.

## Godot 4
Copy `engines/godot/addons/appdimens`, enable the plugin (adds the `AppDimens` autoload).
- Whole-UI scaling: set *Display ▸ Window ▸ Stretch ▸ Mode = disabled* and `AppDimens.auto_ui_scale = true` (uses
  `Window.content_scale_factor`), or size controls with `AppDimens.asdp(…)` / `AppDimens.font_size(…)` — every family token is a function on the
  autoload (`AppDimens.sdp(64)`, `AppDimens.ftsdpa(24)`, `AppDimens.sdpPh(16)`, `AppDimens.logssp(18)`).
- 3D: `AppDimensGames.apply_camera_fov(camera, HYBRID, 60, 16.0/9.0)`, `configure_3d_scaling(viewport, FSR2, QUALITY)`.
- Tests: `godot --headless --path engines/godot -s tests/test_runner.gd`.

## Custom C / C++ engines
`add_subdirectory(native)` and link `AppDimens::games_headers` (header-only) or `AppDimens::games` (C ABI + live snapshot).
C++ token calls over the live snapshot: `#include "appdimens/tokens.hpp"` → `appdimens::asdp(64)`, literals
`64_asdp` (`using namespace appdimens::literals`). Works with SDL2/3, GLFW, raylib, sokol, bgfx, Dear ImGui (`io.FontGlobalScale = factor`, `style.ScaleAllSizes(factor)`).

## .NET engines
`dotnet/AppDimens.Games` (netstandard2.0): MonoGame/FNA (`GraphicsDevice.PresentationParameters.BackBufferWidth`),
Stride, Godot .NET, Unity. Publish with `GameDimens.Publish(new Surface(w, h))`, then `64f.ASdp()`, `24f.FTSdp()` …

## Other languages
The shared library exports a C ABI (`appdimens_games_api.h`): Rust (`bindgen`), Zig (`@cImport`), Python (`ctypes`), LuaJIT
FFI, Swift (module map), Java 22 Panama. Bevy/Rust users can also port `games.h` 1:1 (no state, pure functions).
