# 🧩 AppDimens Games — Modules & Artifacts

```
appdimens-games/
├── library/            → io.github.bodenberg:appdimens-games          CORE (required)
│   ├── common/  enums (qualifiers, inverters, strategies, element/device types)
│   ├── core/    GameScreenConstants · GameMetrics · GameScreen · GameCache
│   ├── math/    GameMath (13 kernels — single source of truth)
│   ├── code/    Kotlin/Java extensions + fluent builder + legacy facade
│   ├── compose/ @Composable extensions + provider
│   ├── world/   Vec2/Vec3/Rect · ViewportMode · world↔screen
│   ├── units/   mm/cm/inch
│   └── resize/  binary-search auto-fit
├── library-{strategy}/ → io.github.bodenberg:appdimens-games-{strategy}
│   percent · power · fluid · auto · diagonal · fill · fit · interpolated ·
│   logarithmic · perimeter · density · resize · units
├── library-bom/        → appdimens-games-bom
├── library-native/     → appdimens-games-native (C++20 core + C99 header + JNI)
├── csharp/AppDimensGames/  Unity/Godot single-file port
├── sample/             demo game (Compose · OpenGL ES · Vulkan surface)
└── benchlab/           on-device comparison vs games-2.0.1 and dynamic-3.1.9
```

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
| `…-native` | JNI + C/C++ + render interop | core |
| `…-bom` | version alignment | — |

Satellites depend **only** on core (no cross-imports — family contract). Compose runtime is `compileOnly` in core: the code API works without Compose on the classpath.

## Migration from 2.0.1

| 2.0.1 (deprecated) | 3.0.0 |
|---|---|
| `AppDimensGames.getInstance().initialize(ctx)` | `GameScreen.updateFromContext(ctx)` (compat: `GamesCompat.initialize`) |
| `games.calculateButtonSize(48f)` | `48.sdpa(ctx)` (DEFAULT ≈ sdpa) |
| `games.calculatePlayerSize(64f)` | `64.asdp(ctx)` (auto satellite) |
| `GameVector2D` + `calculateVector2D` | `Vec2` + `WorldScale`/`ViewportTransform` (world layer) |
| `games.cm(2f)` | `2f.cmPx(ctx)` (units module) |
| fluent `smart()` builder | family `scaledDp()` builder (`16.scaledDp().aspectRatio(true).screen(...).sdp(ctx)`) |
| hash-per-call cache | snapshot factors (≈2 ns) |
