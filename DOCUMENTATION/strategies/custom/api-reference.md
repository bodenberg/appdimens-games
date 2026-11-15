# API reference

Kotlin and C# share the engine design and the numbers; names follow each language (camelCase / PascalCase).

## Building strategies

| Kotlin (`com.appdimens.games.custom`) | C# (`AppDimensGames.Custom`) | Notes |
|---|---|---|
| `CustomScaleStrategy(id?, register?) { factor }` | `CustomScaleStrategy.Create(s => …, id?, register?)` · `CustomGames.CustomScaleStrategy(…)` | programmable factor, `ScreenScope` ([code formulas](code-formulas.md)) |
| `scalingFormula(id?, register?) { value }` | `ScalingStrategy.Formula(s => …, id?, register?)` · `CustomGames.Formula(s => …)` | code formula, `FormulaScope` |
| `scalingFormula("text", id?, register?)` | `ScalingStrategy.Formula("text", id?, register?)` · `CustomGames.Formula("text")` | text formula, shared per text without an id ([text formulas](text-formulas.md)) |
| `scalingStrategy(id, register = true) { … }` | `ScalingStrategy.Define(id, b => { … }, register = true)` | full configuration ([configuration](configuration.md)) |
| `strategy.extend(id, register = true) { … }` | `strategy.Extend(id, b => { … }, register = true)` | child of an existing strategy |
| `strategy.withAxis(axis)` | `strategy.WithAxis(axis)` | same strategy on another axis, id `"<id>@<axis>"` |
| `ScalingConfig.toStrategy(id, register = true)` | `new ScalingStrategy(id, config)` + `ScalingStrategies.Register(…)` | wrap a config built by hand |
| `BuiltInStrategies.SCALED` … `FIXED` · `CustomPresets.GAME_HUD` … | `BuiltInStrategies.Scaled` … `Fixed` · `CustomPresets.GameHud` … | ready-made parents |

All strategies are immutable and thread-safe; each one memoizes the last resolved window.

## Registry: `ScalingStrategies`

| Kotlin | C# | Notes |
|---|---|---|
| `default` | `Default` | strategy used without an argument; initially `BuiltInStrategies.SCALED` / `Scaled` |
| `register(strategy)` · `unregister(id)` | `Register(strategy)` · `Unregister(id)` | add or replace · remove |
| `get(id)` / `ScalingStrategies["id"]` · `getOrThrow(id)` | `Get(id)` · `Require(id)` | lookup, including built-ins and presets; the throwing form raises `IllegalArgumentException` / `KeyNotFoundException` |
| `ids` | `Ids` | ids registered by the app |
| `formula("text")` | `Formula("text")` | shared text-formula strategy |
| `loadJson(json)` · `loadAsset(context, path)` | `LoadJson(json)` | register a [JSON document](json.md) and apply its `default`; atomic |
| — | `ExportJson()` | every registered strategy as one document |

## Kotlin engines / code (`com.appdimens.games.code.custom`)

Short names return **px**, like the core `code` package, and read the live `GameScreen` snapshot (`GameScreen.metrics()`). The optional `context: Context?` parameter exists for family parity and is not required.

| Call | Returns |
|---|---|
| `64.cdp(strategy)` · `64.cdp()` · `64.cdp("text")` | px |
| `64.cdpDp(strategy)` · `64.cdpDp("text")` · `64.cdpDp(metrics, strategy)` | dp (the `metrics` overload takes an explicit `GameMetrics`) |
| `64.cdpi(strategy)` · `64.cdpi("text")` | px, resize-invariant: resolved on `GameScreen.invariantMetrics()`, base value under true multi-window |
| `18.csp(strategy)` · `18.csp("text")` | px for text (font-scale policy applied) |
| `18.cspDp(strategy)` · `18.cspDp("text")` | dp for text (font-scale policy applied) |
| `strategy.px(64)` · `strategy.dp(64)` · `strategy.spPx(18)` | strategy-first forms |
| `strategy.factor()` · `strategy.explain()` | debug, live window |
| `ScalingStrategies.loadAsset(context, path)` | load a JSON document from Android `assets/` |

Without a strategy, the code API uses `ScalingStrategies.default`.

### Java / other JVM engines: `DimenCustom`

```java
float px  = DimenCustom.cdp(64f, DimenCustom.strategy("hud"));
float dp  = DimenCustom.cdpDp(64f, DimenCustom.formula("base * (sw / 360)"));
float pin = DimenCustom.cdpi(64f);                       // default strategy
float tx  = DimenCustom.csp(18f, "base * min(sw / 360, 1.3)");
DimenCustom.loadAsset(context, "appdimens-strategies.json");
```

`cdp`, `cdpDp`, `cdpi`, `csp` (each with a strategy, a formula string or the default), `formula(text)`, `strategy(id)`, `factor(strategy)`, `loadJson(json)` and `loadAsset(context, path)`.

## Compose (`com.appdimens.games.compose.custom`)

Reads `LocalDimenMetrics` (provided by `AppDimensProvider { … }`).

| Call | Returns |
|---|---|
| `64.cdp(strategy)` · `64.cdp` · `64.cdp("text")` | `Dp` |
| `64.cdpi(strategy)` · `64.cdpi` · `64.cdpi("text")` | `Dp`, base value when the surface is not fullscreen |
| `64.cdpPx(strategy)` · `64.cdpPx` · `64.cdpPx("text")` | `Float` px |
| `18.csp(strategy)` · `18.csp` · `18.csp("text")` | `TextUnit` (sp, the font-scale policy is pre-compensated) |
| `18.cspPx(strategy)` · `18.cspPx` · `18.cspPx("text")` | `Float` px (policy applied) |
| `strategy.dp(64)` · `strategy.sp(18)` · `strategy.px(64)` | strategy-first forms |
| `64.dp.scaledBy(strategy)` | scale an existing `Dp` |
| `ProvideScalingStrategy(strategy) { … }` · `LocalScalingStrategy` · `currentScalingStrategy` | scoped default |
| `strategy.currentFactor()` · `strategy.explainCurrent()` | debug |

Without an explicit strategy, `64.cdp` uses the nearest `ProvideScalingStrategy`, then `ScalingStrategies.default`.

## `GameMetrics` bridge (Kotlin)

| Call | Notes |
|---|---|
| `GameMetrics.toScreenSnapshot()` | platform-free `ScreenSnapshot`; not fullscreen → `isMultiWindow = true` |
| `strategy.resolve(metrics: GameMetrics)` | memoized per snapshot: one memo check + one multiply, allocation-free on hit |
| `strategy.resolveLive()` · `strategy.resolveInvariant()` | live `GameScreen` · frozen fullscreen reference (`null` under true multi-window) |
| `DeviceClass.fromGameUiMode(uiMode, smallestWidthDp)` | device class from the Games `UiModeType` |

## Unity / C# (`AppDimensGamesCustom.cs`)

`CustomKernels` reads the live `AppDimensGames.Screen.Current` (fed by `Screen.Update(…)`). Short names return **dp**, like `MathKernels`; the `Px` suffix multiplies by `Screen.Current.Density`.

| `CustomKernels` | `float` extension (`CustomFloatExtensions`) | Returns |
|---|---|---|
| `Cdp(64f, strategy?)` · `Cdp(64f, "text")` | `64f.Cdp(strategy?)` · `64f.Cdp("text")` | dp |
| `CdpInvariant(64f, strategy?)` · `CdpInvariant(64f, "text")` | `64f.Cdpi(…)` | dp on the frozen fullscreen reference (`Screen.Invariant`); base value under multi-window |
| `CdpPx(64f, …)` | `64f.CdpPx(…)` | px |
| `Csp(18f, …)` | `18f.Csp(…)` | final text size in dp (font-scale policy applied) |
| `CspPx(18f, …)` | `18f.CspPx(…)` | final text size in px |
| `Factor(strategy?)` · `Explain(strategy?)` | — | debug, live window |

Without a strategy (`null`), `ScalingStrategies.Default` is used.

| `CustomGames` | Notes |
|---|---|
| `Formula("text", id?, register?)` · `Formula(s => …, id?, register?)` · `CustomScaleStrategy(s => …, id?, register?)` | the one-liners, same as `ScalingStrategy.Formula` / `CustomScaleStrategy.Create` |
| `metrics.ToScreenSnapshot()` | `AppDimensGames.Metrics` → `ScreenSnapshot`; not fullscreen → multi-window |
| `strategy.Resolve(metrics)` | memoized per snapshot |
| `DeviceClassResolver` | `Func<Metrics, DeviceClass?>?` override of the device class |
| `DefaultDeviceClass(metrics)` | Unity `SystemInfo.deviceType` (Desktop → desktop, Console → tv), else by size |

## Strategy members

| Kotlin | C# | Notes |
|---|---|---|
| `id` · `config` · `isValueDependent` | `Id` · `Config` · `IsValueDependent` | identity · full config · whether results depend on the value |
| `resolve(screen)` → `ResolvedScaling` | `Resolve(screen)` → `ResolvedScaling` | memoized per window: `factor`, `appliedRules`, `scale(base)`, `scaleSpDp(base)`, `scaleSpUnits(base)`, `explain(id)` (C#: PascalCase) |
| `factor(screen)` · `scale(base, screen)` · `explain(screen)` | `Factor(screen)` · `Scale(base, screen)` · `Explain(screen)` | platform-free helpers (tests, previews) |
| `toJson()` | `ToJson()` | export (throws for code lambdas) |

`ScreenSnapshot(widthDp, heightDp, smallestWidthDp, density, fontScale, isLandscape, isMultiWindow, device)` builds a window by hand, for tests and previews; every argument after `heightDp` has a default in both languages.

[Back to the custom docs](README.md)
