# AppDimens Games — Library Map (for agents)

## Modules
- `:library` → `io.github.bodenberg:appdimens-games` — CORE
  - `common/`: DpQualifier, Inverter, Orientation, UiModeType, GameScalingStrategy, GameElementType, GameDeviceType
  - `core/`: GameScreenConstants, GameMetrics (snapshot), GameScreen (live hub + frozen fullscreen for `i`), GameCache (lock-free), MissingModule-ready
  - `math/`: GameMath — 13 kernels, single source of truth (pure Float)
  - `code/`: family extensions (sdp/hdp/wdp/ssp/sem…), Facilitators, DimenScaled builder, DimenSdp Java facade, compat/GamesCompat (deprecated 2.x)
  - `compose/` (`AppDimensGamesCompose.kt`; Compose is compileOnly, no version pinned): AppDimensProvider, LocalDimenMetrics/LocalUiModeType, same stems as @get:Composable properties
  - `world/`: Vec2/Vec3, RectF, ViewportMode+ViewportTransform, WorldScale (fitPx/fillPx ≡ FIT_ALL/CROP)
  - `units/`, `resize/`
- Satellites (`appdimens-games-<strategy>`): percent·power·fluid·auto·diagonal·fill·fit·interpolated·logarithmic·perimeter·density·resize·units (+BOM)
- `appdimens-games-custom` (unreleased, `:library-custom`): `com.appdimens.games.custom` (engine identical to appdimens-dynamic-custom: `scalingFormula { base * pow(sw / 360f, 0.8f) }` / `CustomScaleStrategy { … }` (typed `FormulaScope` / `ScreenScope` receivers) / text `scalingFormula("…")`, `scalingStrategy { }`, `ScalingStrategies`, `ScalingStrategyJson`, `ScalingExpression`, presets, bridge `GameMetrics.toScreenSnapshot()` / `resolve(GameMetrics)`), `code.custom` (`cdp` px, `cdpDp`, `cdpi`, `csp`, each also taking a formula string, `DimenCustom`), `compose.custom` (`cdp`, `csp`, `ProvideScalingStrategy`). C#: `csharp/AppDimensGames/AppDimensGamesCustom.cs` (+ generated `.Engine.cs`).
- `library-native`: cpp/include/appdimens/games/{core.h,math.h,render.h} + c/appdimens_games_c.h + jni bridge (Kotlin: com.appdimens.games.jni.NativeBridge)
- `csharp/AppDimensGames/AppDimensGames.cs` (+asmdef) — Unity/Godot port
- `sample/` (Compose · GLSurfaceView · Vulkan surface) · `benchlab/` (vs games-2.0.1 & dynamic-3.1.9)

## Toolchain (family-aligned)
AGP 9.3.1 · Kotlin 2.4.10 · compileSdk 37 · minSdk 24 · JVM target 17 · Gradle 9.5.0 (daemon JVM 25)

## Key invariants
- Hot path = base × precomputed factor; zero allocation; no locks on reads.
- Cache partitions keyed by GameMetrics identity; custom K never cached.
- Constants canonical: 300 / 533 / 611.6305 (literal!) / 833 / 1.78.
