# AppDimens Games — Library Map (for agents)

## Modules
- `:library` → `io.github.bodenberg:appdimens-games` — CORE
  - `common/`: DpQualifier, Inverter, Orientation, UiModeType, GameScalingStrategy, GameElementType, GameDeviceType
  - `core/`: GameScreenConstants, GameMetrics (snapshot), GameScreen (live hub + frozen fullscreen for `i`), GameCache (lock-free), MissingModule-ready
  - `math/`: GameMath — 13 kernels, single source of truth (pure Float)
  - `code/`: family extensions (sdp/hdp/wdp/ssp/sem…), Facilitators, DimenScaled builder, DimenSdp Java facade, compat/GamesCompat (deprecated 2.x)
  - `compose/`: AppDimensProvider, LocalDimenMetrics/LocalUiModeType, same stems as @get:Composable properties
  - `world/`: Vec2/Vec3, RectF, ViewportMode+ViewportTransform, WorldScale
  - `units/`, `resize/`
- Satellites (`appdimens-games-<strategy>`): percent·power·fluid·auto·diagonal·fill·fit·interpolated·logarithmic·perimeter·density·resize·units (+BOM)
- `library-native`: cpp/include/appdimens/games/{core.h,math.h,render.h} + c/appdimens_games_c.h + jni bridge (Kotlin: com.appdimens.games.jni.NativeBridge)
- `csharp/AppDimensGames/AppDimensGames.cs` (+asmdef) — Unity/Godot port
- `sample/` (Compose · GLSurfaceView · Vulkan surface) · `benchlab/` (vs games-2.0.1 & dynamic-3.1.9)

## Toolchain (family-aligned)
AGP 9.3.1 · Kotlin 2.x · compileSdk 37 · minSdk 24 · JDK 17 · Gradle 9.1

## Key invariants
- Hot path = base × precomputed factor; zero allocation; no locks on reads.
- Cache partitions keyed by GameMetrics identity; custom K never cached.
- Constants canonical: 300 / 533 / 611.6305 (literal!) / 833 / 1.78.
