# 🧩 AppDimens Games — Modules & Artifacts

## Repository layout (by platform)

```
appdimens-games/
├── android/                 Android — Gradle build: Kotlin · Compose · Java · NDK (Maven Central artifacts)
│   ├── library/             → io.github.bodenberg:appdimens-games          CORE (required)
│   │   ├── common/  enums (qualifiers, inverters, strategies, element/device types)
│   │   ├── core/    GameScreenConstants · GameMetrics · GameScreen · GameCache
│   │   ├── math/    GameMath (13 kernels — single source of truth on the JVM)
│   │   ├── code/    Kotlin/Java extensions + fluent builder + legacy facade
│   │   ├── compose/ @Composable extensions + provider
│   │   ├── world/   Vec2/Vec3/Rect · ViewportMode · world↔screen
│   │   ├── units/   mm/cm/inch
│   │   └── resize/  binary-search auto-fit
│   ├── library-{strategy}/  → appdimens-games-{strategy}
│   │   percent · power · fluid · auto · diagonal · fill · fit · interpolated ·
│   │   logarithmic · perimeter · density · resize · units · custom (unreleased)
│   ├── library-bom/         → appdimens-games-bom
│   ├── library-native/      → appdimens-games-native (AAR: JNI over ../native)
│   ├── sample/              demo game (Compose · OpenGL ES · Vulkan surface)
│   ├── benchlab/            on-device comparison vs games-2.0.1 and dynamic-3.1.9
│   └── scripts/             gen_satellites.py (generates the library-{strategy} modules)
├── native/                  C / C++ SDK for every platform (Windows, Proton/Wine, Linux, Steam Deck,
│   │                        macOS, iOS, Android NDK, WebAssembly, consoles)
│   ├── include/appdimens/   appdimens.h (core) · games.h (viewports, projections, FOV, upscaling, HUD, LOD)
│   │                        appdimens_custom.h (custom strategies) · appdimens_games_api.h (C ABI)
│   ├── src/                 shared library (C ABI, lock-free snapshots, platform detection)
│   └── tests/               core, games, layout, ABI and custom-strategy tests
├── engines/
│   ├── unity/               UPM package com.bodenberg.appdimens.games (+ tests/ compile check)
│   ├── unreal/              UE5 plugin AppDimens (+ tests/ with API stubs)
│   └── godot/               Godot 4 addon (addons/appdimens) + headless test project
├── dotnet/
│   ├── AppDimens.Games/     netstandard2.0 package (MonoGame, FNA, Stride, Godot .NET, Unity)
│   ├── AppDimens.Games.Tests/
│   └── dropin/AppDimensGames/  copy-in C# sources (single-file core + custom strategies, Unity asmdef)
├── tools/                   oracle.py (formula audit) · sync_bindings.py (copies shared sources into
│                            engines) · sync-csharp-custom.py
├── skills/                  agent skills for this repository
└── DOCUMENTATION/           guides (index: README.md)
```

| Folder | Build / test | CI workflow |
|---|---|---|
| `android/` | `cd android && ./gradlew assembleDebug testDebugUnitTest` | `android.yml` |
| `native/` | `cmake -S native -B build && cmake --build build && ctest --test-dir build` | `cross-platform.yml` (Linux, arm64, Windows, macOS, Wine, iOS, NDK, WASM) |
| `engines/unity` | `dotnet build engines/unity/tests/UnityCompileCheck.csproj` | `cross-platform.yml` |
| `engines/unreal` | clang++ against `engines/unreal/tests/stubs` | `cross-platform.yml` |
| `engines/godot` | `godot --headless --path engines/godot -s tests/test_runner.gd` | `cross-platform.yml` |
| `dotnet/` | `dotnet run --project dotnet/AppDimens.Games.Tests` | `cross-platform.yml` |
| formulas | `python3 tools/oracle.py` | `cross-platform.yml` |


## Artifact matrix

| Artifact | Content | Depends on |
|---|---|---|
| `appdimens-games` | core + scaled family | annotation |
| `…-percent` | `psdp…` + `space*` | core |
| `…-power` | `pwsdp…` | core |
| `…-fluid` | `fsdp…` | core |
| `…-auto` | `asdp…` | core |
| `…-diagonal` | `dgsdp…` | core |
| `…-fill` | `flsdp…` | core |
| `…-fit` | `ftsdp…` | core |
| `…-interpolated` | `isdp…` | core |
| `…-logarithmic` | `logsdp…` | core |
| `…-perimeter` | `prsdp…` | core |
| `…-density` | `dsdp…` | core |
| `…-resize` | container auto-fit | core |
| `…-units` | mm/cm/inch | core |
| `…-custom` *(unreleased)* | `cdp/csp` — your own strategies (code and text formulas, DSL, JSON, rules, presets); see [custom.md](strategies/custom.md) and [custom/](strategies/custom/README.md) | core |
| `…-native` | JNI + C/C++ + render interop | core |
| `…-bom` | version alignment | — |

Satellites depend **only** on core (no cross-imports — family contract). Compose (`androidx.compose.runtime` + `androidx.compose.ui`) is `compileOnly` in core and satellites: the code API works without Compose on the classpath, and the published POM carries **no** Compose version constraint — the consumer's `compose-bom` (any version) is the single runtime. `material3` is not used by the library.

## Migration from 2.0.1

| 2.0.1 (deprecated) | 3.x (3.2.0) |
|---|---|
| `AppDimensGames.getInstance().initialize(ctx)` | `GameScreen.updateFromContext(ctx)` (compat: `GamesCompat.initialize`) |
| `games.calculateButtonSize(48f)` | `48.sdpa(ctx)` (DEFAULT ≈ sdpa) |
| `games.calculatePlayerSize(64f)` | `64.asdp(ctx)` (auto satellite) |
| `GameVector2D` + `calculateVector2D` | `Vec2` + `WorldScale`/`ViewportTransform` (world layer) |
| `games.cm(2f)` | `2f.cmPx(ctx)` (units module) |
| fluent `smart()` builder | family `scaledDp()` builder (`16.scaledDp().aspectRatio(true).screen(...).sdp(ctx)`) |
| hash-per-call cache | snapshot factors (≈2 ns) |
