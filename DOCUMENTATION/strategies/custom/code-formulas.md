# Code formulas

Two one-line entry points let you write the scaling math in plain code, in Kotlin or in C#. Inside the lambda you get a typed **scope** whose names match the [text language](text-formulas.md) (PascalCase in C#), so you only learn them once. The IDE autocompletes them, and a typo fails at compile time.

| | Factor | Final value |
|---|---|---|
| Kotlin | `CustomScaleStrategy { … }` | `scalingFormula { … }` |
| C# | `CustomScaleStrategy.Create(s => …)` · `CustomGames.CustomScaleStrategy(s => …)` | `ScalingStrategy.Formula(s => …)` · `CustomGames.Formula(s => …)` |
| You return | the **factor** (every value is multiplied by it) | the **final value** (dp) |
| Scope | `ScreenScope` | `FormulaScope` (= `ScreenScope` + `base` / `Base`) |
| Runs | once per window | once per value, per call |
| Cost per value | one multiply | the lambda |
| JSON | no (use a text `expression` curve) | no (use a text `formula`) |

## Programmable factor

```kotlin
val Product = CustomScaleStrategy { (width / 360f).coerceIn(0.85f, 1.80f) }
val Living  = CustomScaleStrategy { if (tv) height / 540f else ratio }
val Steps   = CustomScaleStrategy {
    when {
        sw < 360f -> 0.9f
        sw < 600f -> sw / 400f
        else -> 1.5f + (sw - 600f) / 2000f
    }
}
```

```csharp
var product = CustomScaleStrategy.Create(s => s.Clamp(s.Width / 360f, 0.85f, 1.8f));
var living  = CustomScaleStrategy.Create(s => s.Tv ? s.Height / 540f : s.Ratio);
var steps   = CustomScaleStrategy.Create(s =>
    s.Sw < 360f ? 0.9f : s.Sw < 600f ? s.Sw / 400f : 1.5f + (s.Sw - 600f) / 2000f);
```

- Kotlin: `CustomScaleStrategy(id: String? = null, register: Boolean = id != null, factor: ScreenScope.() -> Float): ScalingStrategy`.
- C#: `CustomScaleStrategy.Create(Func<ScreenScope, float> factor, string? id = null, bool? register = null)`; `CustomGames.CustomScaleStrategy(…)` is the same call.
- Without an `id` a unique `scale#N` id is generated and the strategy is not registered. With an id it is registered, so `ScalingStrategies["product"]` (C#: `ScalingStrategies.Get("product")`) finds it.
- A non-finite factor (NaN, infinity) throws when the window resolves: `IllegalArgumentException` in Kotlin, `InvalidOperationException` in C#.

## The final value

```kotlin
val Hud  = scalingFormula { base * pow(sw / 360f, 0.8f) }
val Card = scalingFormula { if (tablet) base * 1.2f else base * ratio }
val Snap = scalingFormula { round(base * sw / 360f) }          // whole dp only
val Min  = scalingFormula { max(base * ratio, base) }          // never smaller than the design value
```

```csharp
var hud  = ScalingStrategy.Formula(s => s.Base * s.Pow(s.Sw / 360f, 0.8f));
var card = ScalingStrategy.Formula(s => s.Tablet ? s.Base * 1.2f : s.Base * s.Ratio);
var snap = ScalingStrategy.Formula(s => s.Round(s.Base * s.Sw / 360f));
var min  = ScalingStrategy.Formula(s => s.Max(s.Base * s.Ratio, s.Base));
```

- Kotlin: `scalingFormula(id: String? = null, register: Boolean = id != null, formula: FormulaScope.() -> Float): ScalingStrategy`.
- C#: `ScalingStrategy.Formula(Func<FormulaScope, float> formula, string? id = null, bool? register = null)`; `CustomGames.Formula(…)` is the same call.
- `base` is the value you pass: in `64.cdp(Hud)` it is `64f`. For `csp` the result then follows the strategy font-scale policy (see [font scale](configuration.md#font-scale-csp)).
- Without an `id`, a unique `formula#N` id is generated.

## Names inside the lambda

| Kotlin | C# | Type | Meaning |
|---|---|---|---|
| `base` | `Base` | `Float` | the value being scaled (formula only) |
| `sw` | `Sw` | `Float` | smallest width of the window (dp) |
| `width` · `height` | `Width` · `Height` | `Float` | window width · height (dp) |
| `shortSide` · `longSide` | `ShortSide` · `LongSide` | `Float` | shorter · longer side (dp) |
| `diagonal` · `perimeter` | `Diagonal` · `Perimeter` | `Float` | `√(w² + h²)` · `w + h` (dp) |
| `aspectRatio` | `AspectRatio` | `Float` | `longSide / shortSide` |
| `ratio` | `Ratio` | `Float` | `sw / 300`, the ratio `sdp` uses (`base * ratio` is exactly `sdp`) |
| `density` · `fontScale` | `Density` · `FontScale` | `Float` | screen density · user font scale |
| `phone` `tablet` `desktop` `tv` `watch` `car` `vr` | `Phone` `Tablet` `Desktop` `Tv` `Watch` `Car` `Vr` | `Boolean` | device class of the window |
| `landscape` · `portrait` · `multiWindow` | `Landscape` · `Portrait` · `MultiWindow` | `Boolean` | orientation · not fullscreen (split screen, freeform) |
| `device` | `Device` | `DeviceClass` | the device class itself (for `when` / `switch`) |
| `screen` | `Screen` | `ScreenSnapshot` | the whole snapshot |
| `pow(x, e)` `sqrt` `ln` `log10` `exp` `abs` | `Pow` `Sqrt` `Ln` `Log10` `Exp` `Abs` | `Float` | math, no import needed |
| `round(x)` | `Round(x)` | `Float` | half up: `floor(x + 0.5)` |
| `min(a, b)` `max(a, b)` `clamp(x, lo, hi)` | `Min` `Max` `Clamp` | `Float` | bounds |
| `lerp(a, b, t)` · `smoothstep(e0, e1, x)` | `Lerp` · `Smoothstep` | `Float` | blending |

The device class comes from the game snapshot: in Kotlin from `GameMetrics.uiMode` (TV, car, watch, desk → desktop, VR headset) and the smallest width (`sw ≥ 600` is a tablet); in Unity from `SystemInfo.deviceType` (Desktop → desktop, Console → tv) and the smallest width. `multiWindow` is true when the game surface is not fullscreen. See [device classes](configuration.md#device-classes-in-games).

## Rules for the lambda

- **Keep it pure.** It must not read mutable state, preferences, game state or time: the factor is memoized per window. Use a different strategy (or a rule) for a different state.
- **Declare it once.** Every call to `CustomScaleStrategy` / `scalingFormula` / `ScalingStrategy.Formula(lambda)` creates a new strategy with its own memo. Use a top-level `val`, an `object`, a `static readonly` field or `remember` in Compose, never a fresh lambda in `Update()` or on every recomposition.
- **Watch out for shadowing (Kotlin).** A local variable named like a scope property wins over it. In `val tv = …; CustomScaleStrategy { if (tv) … }` the `tv` is your local, not the device flag. C# lambdas always go through `s.`, so they do not shadow.
- **Prefer the factor form when you can.** If your formula is `base × (something about the screen)`, write the something as a `CustomScaleStrategy`: it costs one multiply instead of running your lambda for each value.

## When to use something else

- The formula must live in JSON, a design-token file or remote config: [Text formulas](text-formulas.md).
- You need rules per device, bounds or a font-scale policy on top of a curve: [Configuration](configuration.md). The builders also accept code inside them: Kotlin `curve = custom { ratio, screen -> … }` and `transform { base, scaled, screen -> … }`; C# `b.Curve = ScalingCurve.Custom((ratio, screen) => …)` and `b.Transform((baseValue, scaled, screen) => …)`.

[Back to the custom docs](README.md)
