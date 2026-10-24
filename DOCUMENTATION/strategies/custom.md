# Custom strategies — `cdp` / `csp` (`appdimens-games-custom`)

> **Unreleased** — on `main`, ships with the next release. Same engine and JSON format as every AppDimens platform (`appdimens-dynamic-custom`, KMP, Web, React Native, Flutter, Apple, .NET).

## Quick start: just write the formula

The easiest way is to write the math yourself, as ordinary Kotlin on one line. Inside the braces, the IDE autocompletes everything you can use (`base`, `sw`, `width`, `tablet`, `pow`, `clamp`…), and a typo is a compile error. There is nothing to configure.

```kotlin
import com.appdimens.games.custom.*
import com.appdimens.games.code.custom.*       // px, live GameScreen snapshot
// import com.appdimens.games.compose.custom.*  // Compose: Dp / TextUnit

// 1. Formula: the result is the final value (`base` = the value you pass, e.g. 64)
val Hud = scalingFormula { base * pow(sw / 360f, 0.8f) }
val Card = scalingFormula { if (tablet) base * 1.2f else base * ratio }

// 2. Factor: the result multiplies every value
val Product = CustomScaleStrategy { (width / 360f).coerceIn(0.85f, 1.80f) }
val Living = CustomScaleStrategy { if (tv) height / 540f else ratio }

val buttonPx = 64.cdp(Product)       // 64 × factor, px; also cdpDp, cdpi, csp, cspDp
val labelPx = 18.csp(Hud)            // text follows the strategy font-scale policy
```

Declare each strategy once (for example as a top-level `val`), not inside a composable or a frame loop, then use it anywhere. Compose: `Modifier.size(64.cdp(Product))` · `fontSize = 18.csp(Hud)`. Java / engines: `DimenCustom.cdp(64f, Product)`. The game window maps through `GameMetrics.toScreenSnapshot()`.

C# (Unity / Godot), same names in PascalCase:

```csharp
var hud = ScalingStrategy.Formula(s => s.Base * s.Pow(s.Sw / 360f, 0.8f));            // or CustomGames.Formula(…)
var product = CustomScaleStrategy.Create(s => s.Clamp(s.Width / 360f, 0.85f, 1.8f));   // or CustomGames.CustomScaleStrategy(…)
float button = 64f.Cdp(product);
```

### What you can use inside the braces

| Name | Meaning |
|---|---|
| `base` | the value you pass (`64` in `64.cdp(Hud)`), only in `scalingFormula { }` |
| `sw` · `width` · `height` | smallest width · window width · window height (dp) |
| `shortSide` · `longSide` · `diagonal` · `perimeter` · `aspectRatio` | other window measures |
| `ratio` | `sw / 300`, the ratio `sdp` uses (`base * ratio` is the same as `sdp`) |
| `phone` `tablet` `desktop` `tv` `watch` `car` `vr` · `landscape` `portrait` `multiWindow` | `Boolean` (non-fullscreen game surface = `multiWindow`) |
| `density` · `fontScale` · `device` · `screen` | screen density · user font scale · `DeviceClass` · the whole `ScreenSnapshot` |
| `pow` `sqrt` `ln` `log10` `exp` `abs` `round` `min` `max` `clamp` `lerp` `smoothstep` | math, no import needed |

Anything else from Kotlin or your own code also works (`coerceIn`, `when`, your helpers). Strategies must be pure: they must not read mutable state, because results are memoized per window. A non-finite factor throws when the window resolves.

| Form | You return | Cost per value | JSON |
|---|---|---|---|
| `CustomScaleStrategy { … }` | the **factor** | one multiply (runs once per window) | no |
| `scalingFormula { … }` | the final value | cached per value and window | no |
| `scalingFormula("…")` / `64.cdp("…")` | the final value, as text | one multiply when shaped `base * (…)`, otherwise cached | yes |

### Text formulas (for JSON and remote config)

When the formula has to live outside the code, in a JSON file, a remote config or an A/B test, write the same thing as text. It uses the same names (flags are 1/0):

```kotlin
val Hud = scalingFormula("base * pow(sw / 360, 0.8)")
val buttonPx = 64.cdp("base * (tablet ? 0.9 : 1) * clamp(sw / 360, 0.85, 1.6)")   // inline; also cdpDp, cdpi, csp, cspDp
```

Text is checked when the strategy is created: an unknown name fails right away with its position and the list of valid names (e.g. `unknown variable 'sww' (allowed: aspectRatio, base, …)`). The same text works in the strategies JSON (`{"id": "hud", "formula": "base * pow(sw / 360, 0.8)"}`) and gives the same numbers on every AppDimens platform. Operators and functions are listed in [Free-form formulas](#free-form-formulas-no-limits-from-the-built-in-curves). Java / engines: `DimenCustom.cdp(64f, "…")` · `DimenCustom.formula("…")`. C#: `64f.Cdp("base * pow(sw / 360, 0.8)")` · `CustomKernels.Csp(18f, "…")` · `CustomGames.Formula("…")`.

**Fast path.** A text formula shaped `base * (…)`, where the `(…)` part does not read `base`, costs one multiply per value, because the `(…)` part is computed once per window. `base * sw / 360` is evaluated as written, `(base * sw) / 360`, so write `base * (sw / 360)` to get the fast path. In code, prefer `CustomScaleStrategy { … }` for the same effect.

For rules per device, factor bounds or a font-scale policy, use the full configuration below.

## What it is

Your own scaling strategy, defined as a configuration instead of a fixed growth law:

```
ratio  = axis(window) / axis(reference)
factor = clamp(curve(ratio) × multiplier × AR, minFactor, maxFactor)
dp     = formula(clampValue(base × factor))
```

You choose the reference frame, the axis (`smallestWidth`, `width`, `height`, `min`, `max`, `diagonal`, `perimeter`), the curve (linear, power, logarithmic, interpolated, constant, breakpoints, a **free-form text expression** or a code lambda), the bounds, aspect-ratio correction, font-scale policy, and **rules per device class** (phone, tablet, desktop, tv, watch, car, vr), screen range, orientation or split-screen. The game window maps as follows: `GameMetrics.isFullscreen == false` counts as multi-window, and `UiModeType` gives the device class.

## Calculation used

The formula above. Curves: `r`, `r^e`, `1 + k·ln r`, `1 + (r − 1)·t`, constant `f`, breakpoints (piecewise linear over the absolute axis dp), or your expression. AR correction: `× (1 + k·ln(AR / 1.78))`, default `k = 0.08 / 30`. Font scale (`csp`): `follow`, `ignore` or `clamp(max)`.

## How to use

### Kotlin (code API — px, live `GameScreen` snapshot)

```kotlin
val Hud = scalingStrategy("hud") {
    axis = ScalingAxis.MIN
    curve = power(0.85f)
    aspectRatio()
    clamp(min = 0.75f, max = 2.5f)
    ignoreFontScale()
    on(DeviceClass.TV) { curve = expression("height / 540") }
}

val buttonPx = 64f.cdp(Hud)          // px; also cdpDp(Hud), cdpi(Hud) (resize-invariant), csp(Hud), cspDp(Hud)
val factor = Hud.factor()            // Hud.explain() for logs
```

### Compose

```kotlin
AppDimensProvider {
    ProvideScalingStrategy(Hud) {
        Box(Modifier.size(64.cdp)) { Text("Score", fontSize = 18.csp) }   // also 64.cdp(Hud), cdpi, cdpPx, cspPx
    }
}
```

### C# (Unity / Godot) — `csharp/AppDimensGames/AppDimensGamesCustom*.cs`

```csharp
using AppDimensGames.Custom;
var hud = ScalingStrategy.Define("hud", b =>
{
    b.Axis = ScalingAxis.Min;
    b.Curve = ScalingCurve.Expression("clamp(pow(ratio, 0.85), 0.75, 2.5)");
    b.IgnoreFontScale();
});
float button = CustomKernels.Cdp(64f, hud);       // dp; 64f.Cdp(hud), CdpPx, CdpInvariant, Csp (final dp), CspPx
```

Unity maps `SystemInfo.deviceType` Desktop → `desktop` and Console → `tv`. Override this with `CustomGames.DeviceClassResolver`.

### JSON (design tokens, remote config, A/B tests)

```kotlin
ScalingStrategies.loadAsset(context, "appdimens-strategies.json")   // C#: ScalingStrategies.LoadJson(text)
```

```json
{ "version": 1, "default": "hud",
  "strategies": [ { "id": "hud", "extends": "gameHud",
                    "rules": [ { "when": { "device": "tv" }, "set": { "curve": { "type": "expression", "expression": "height / 540" } } } ] } ] }
```

## Free-form formulas (no limits from the built-in curves)

You are not limited to the built-in curves. Write the formula yourself, as **text** that also works in JSON and gives the same result on every AppDimens platform:

- **Curve expression** returns the **factor** from the screen. It is computed once per window, like any curve, and stays on the one-multiply fast path.
  `"curve": {"type": "expression", "expression": "pow(ratio, 0.8) * (tablet ? 0.9 : 1)"}`
- **Formula** returns the **final value** for each input. It can use `base`, `scaled` and `factor`, and replaces or post-processes the result. The strategy then becomes value-dependent, so results are cached per window.
  `"formula": "base * pow(sw / 360, 0.7) + 2"` · `"formula": "max(scaled, base)"`

| | |
|---|---|
| Operators | `+ - * / % ^` (`^` = power, right-associative; `%` keeps the dividend's sign), `== != < <= > >=`, `&& \|\| !`, `cond ? a : b`, `( )` |
| Functions | `min(a, b, …)` `max(a, b, …)` `clamp(x, lo, hi)` `pow` `sqrt` `ln`/`log` `log10` `log2` `exp` `abs` `sign` `floor` `ceil` `round` (half up) `lerp(a, b, t)` `step(edge, x)` `smoothstep(e0, e1, x)` |
| Screen variables | `sw` `width` `height` `shortSide` `longSide` `diagonal` `perimeter` `aspectRatio` `density` `fontScale`; flags (1/0): `landscape` `portrait` `multiWindow` `phone` `tablet` `desktop` `tv` `watch` `car` `vr`; constants `pi` `e`, `true` / `false` |
| Curve variables | screen variables + `ratio` (axis / reference), `axis` (dp), `ref` (reference dp), `refWidth`, `refHeight` |
| Formula variables | curve variables + `base` (input), `scaled` (base × factor after `valueClamp`), `factor` |

Expressions are parsed when the strategy is created. Unknown variables or functions, wrong arity and syntax errors fail early with the position and the JSON path (e.g. `brand.rules[0].set.curve.expression`). A non-finite factor (for example, a division by zero) throws when the strategy resolves. Rules can swap expressions per device like any other field, and `"formula": null` clears an inherited formula.

For anything the language cannot express, use a code-only lambda: `custom { ratio, screen -> … }` / `ScalingCurve.Custom((r, s) => …)` (factor) or `transform { base, scaled, screen -> … }` / `b.Transform(…)` (value). Both have full access to your code, but they cannot be serialized to JSON.

## Presets

`CustomPresets.GAME_HUD` (`GameHud` in C#): shorter side, `power(0.85)`, aspect-ratio aware, factor 0.75–2.5, fixed text. Also `TABLET_FRIENDLY`, `COMPACT_FIRST`, `TV_SAFE`, `WEARABLE`, `READABLE_TEXT`. Built-ins as parents: `BuiltInStrategies.{SCALED, POWER, LOGARITHMIC, INTERPOLATED, DIAGONAL, PERIMETER, FIXED}` (same factors as the satellites).

## Why use it

- One growth law for HUD, menus and text that you tune once, then share with your launcher app, website and backend through the same JSON.
- Per-device behavior (TV couch distance, watch faces, desktop windows, split-screen) without `if`s at call sites.
- The free-form expressions remove any limit from the built-in curves.

## When to use it

- No built-in strategy passes your visual QA, or you need different laws per device class.
- A design team or remote config owns the numbers (JSON).
- **Not** for gameplay physics or world units: use `GameWorld` / viewport math for those.

## Advantages & trade-offs

| ✅ | ⚠️ |
|---|---|
| Factor memoized per `GameMetrics` snapshot: one multiply per call, zero allocation on hit | One more module; keep strategies few and named |
| Same results on every platform (shared fixture, 540 cases) | Value-dependent strategies (`clampValue`, `formula`) run the formula for each value |
| JSON round-trip (except code lambdas) | Lambdas (`custom { }`, `transform { }`) are not serializable |

## Recommended usage strategy

1. Start from `CustomPresets.GAME_HUD` or a built-in (`extend`).
2. Define strategies in one place (`object GameStrategies` or a JSON asset).
3. Keep the HUD on `cdp` and let world content use `GameWorld` / fit/fill.
4. Check numbers with `explain()` on phone, tablet, TV and split-screen before shipping.
