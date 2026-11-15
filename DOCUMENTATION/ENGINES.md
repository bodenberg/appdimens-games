# Engines

## Unity
Install the UPM package (`bindings/unity/com.bodenberg.appdimens.games`, Unity 2019.4+). `AppDimensScreen` is created
before the first scene and publishes a snapshot whenever `Screen.width/height/dpi/safeArea/fullScreen` change.

| Component | Setup |
|-----------|-------|
| `AppDimensCanvasScaler` | add to each Canvas; author uGUI at 1920×1080; CanvasScaler → Constant Pixel Size (1) or remove |
| `AppDimensSafeArea` | on a full-stretch RectTransform under the Canvas; overscan 0.05 for TV; `maxAspect` 1.777 to keep HUD in 16:9 on ultrawide |
| `AppDimensCamera` | on the camera: Hybrid FOV policy; orthographic cameras get `orthographicSize` |
| `AppDimensDynamicResolution` | cameras with *Allow Dynamic Resolution* |
| UI Toolkit | set `PanelSettings.scale = Live.Current.Factor(Strategy.Auto, 0)` in `AppDimensScreen.Changed` |

Code: `AppDimensScreen.Asdp(64)`, `Live.Current.Resolve("ftsdp", 24)`, `AppDimensUpscaling.RenderScale(...)`.

## Unreal Engine 5
Copy `bindings/unreal/AppDimens` to `Plugins/`. Set *User Interface ▸ DPI Scale Rule = Custom* with
`AppDimensDPIScalingRule`; configure *Plugins ▸ AppDimens* (reference short side, strategy). Blueprint nodes live under
*AppDimens* (sizes, `HorizontalFovForAspect`, `ApplyUpscalerPreset`, `RenderResolution`, `MipBias`, `LodDistance`).
`UAppDimensSubsystem::OnChanged` fires on game-viewport resize. Note: UE camera FOV is horizontal; the helper converts.

## Godot 4
Copy `bindings/godot/addons/appdimens`, enable the plugin (adds the `AppDimens` autoload).
- Whole-UI scaling: set *Display ▸ Window ▸ Stretch ▸ Mode = disabled* and `AppDimens.auto_ui_scale = true` (uses
  `Window.content_scale_factor`), or size controls with `AppDimens.asdp(…)` / `AppDimens.font_size(…)`.
- 3D: `AppDimensGames.apply_camera_fov(camera, HYBRID, 60, 16.0/9.0)`, `configure_3d_scaling(viewport, FSR2, QUALITY)`.
- Tests: `godot --headless --path bindings/godot -s tests/test_runner.gd`.

## Custom C / C++ engines
`add_subdirectory(native)` and link `AppDimens::games_headers` (header-only) or `AppDimens::games` (C ABI + live snapshot).
Works with SDL2/3, GLFW, raylib, sokol, bgfx, Dear ImGui (`io.FontGlobalScale = factor`, `style.ScaleAllSizes(factor)`).

## .NET engines
`bindings/csharp/AppDimens.Games` (netstandard2.0): MonoGame/FNA (`GraphicsDevice.PresentationParameters.BackBufferWidth`),
Stride, Godot .NET, Unity. Publish with `GameDimens.Publish(new Surface(w, h))`.

## Other languages
The shared library exports a C ABI (`appdimens_games_api.h`): Rust (`bindgen`), Zig (`@cImport`), Python (`ctypes`), LuaJIT
FFI, Swift (module map), Java 22 Panama. Bevy/Rust users can also port `games.h` 1:1 (no state, pure functions).
