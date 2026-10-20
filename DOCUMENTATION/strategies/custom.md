# Custom strategies — `cdp` / `csp` (`appdimens-games-custom`)

> **Unreleased** — on `main`, ships with the next release. Same engine and JSON format as every AppDimens platform (`appdimens-dynamic-custom`, KMP, Web, React Native, Flutter, Apple, .NET).

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
