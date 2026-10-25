# Configuration

`scalingStrategy(id) { … }` (Kotlin) and `ScalingStrategy.Define(id, b => { … })` (C#) build a full strategy: the [code](code-formulas.md) and [text](text-formulas.md) one-liners are shortcuts for the simplest cases of it. Every field below also exists in the [JSON format](json.md), on every platform.

```kotlin
import com.appdimens.games.custom.*
import com.appdimens.games.common.Orientation        // for orientation rules

val Brand = scalingStrategy("brand") {            // creates AND registers "brand"
    reference(width = 360, height = 640)          // design frame (dp)
    axis = ScalingAxis.SMALLEST_WIDTH             // what to compare
    curve = power(0.8f)                            // how the ratio becomes a factor
    multiply(1.0f)
    clamp(min = 0.85f, max = 1.6f)                 // factor bounds
    clampValue(min = 10, max = 64)                 // dp bounds (value-dependent)
    aspectRatio(sensitivity = 0.004f)              // long/thin screen correction
    clampFontScale(1.3f)                           // csp policy
    ignoreMultiWindow = false
    on(DeviceClass.TABLET, DeviceClass.DESKTOP, name = "large") { curve = logarithmic(0.4f) }
}
```

```csharp
using AppDimensGames.Custom;

var brand = ScalingStrategy.Define("brand", b =>   // creates AND registers "brand"
{
    b.SetReference(360, 640);
    b.Axis = ScalingAxis.SmallestWidth;
    b.Curve = ScalingCurve.Power(0.8f);
    b.Multiply(1.0f);
    b.Clamp(0.85f, 1.6f);
    b.ClampValue(10, 64);
    b.EnableAspectRatio(sensitivity: 0.004f);
    b.ClampFontScale(1.3f);
    b.IgnoreMultiWindow = false;
    b.On(new[] { DeviceClass.Tablet, DeviceClass.Desktop }, r => r.Curve = ScalingCurve.Logarithmic(0.4f), name: "large");
});
```

Pass `register = false` (C#: `register: false`) to build a throwaway strategy.

## The formula

```
ratio  = axis(window) / axis(reference)
factor = clamp( curve(ratio) × multiplier × AR , minFactor, maxFactor )
value  = formula( clampValue( base × factor , minValue, maxValue ) )
```

## Fields

| Field | Kotlin DSL | C# builder | JSON | Default |
|---|---|---|---|---|
| Reference frame (dp) | `reference(width = 360, height = 640)` | `b.SetReference(360, 640)` | `"reference": {"width": 360, "height": 640}` | 300 × 533 |
| Axis | `axis = ScalingAxis.HEIGHT` | `b.Axis = ScalingAxis.Height` | `"axis": "height"` | `smallestWidth` |
| Curve | `curve = power(0.8f)` | `b.Curve = ScalingCurve.Power(0.8f)` | `"curve": {"type": "power", "exponent": 0.8}` | `linear` |
| Multiplier | `multiply(0.9)` | `b.Multiply(0.9f)` | `"multiplier": 0.9` | 1 |
| Factor bounds | `clamp(min = 0.85, max = 1.6)` | `b.Clamp(0.85f, 1.6f)` | `"clamp": {"min": 0.85, "max": 1.6}` | none |
| Value bounds (dp, ≥ 0) | `clampValue(min = 10, max = 32)` | `b.ClampValue(10, 32)` | `"valueClamp": {"min": 10, "max": 32}` | none |
| Aspect ratio | `aspectRatio(sensitivity = 0.004f)` | `b.EnableAspectRatio(sensitivity: 0.004f)` | `"aspectRatio": {"enabled": true, "sensitivity": 0.004}` | off, k = 0.08 / 30 |
| Font scale (`csp`) | `followFontScale()` · `ignoreFontScale()` · `clampFontScale(1.3f)` | `b.FollowFontScale()` · `b.IgnoreFontScale()` · `b.ClampFontScale(1.3f)` | `"fontScale": "follow"` · `"ignore"` · `{"max": 1.3}` | `follow` |
| Multi-window | `ignoreMultiWindow = true` | `b.IgnoreMultiWindow = true` | `"ignoreMultiWindow": true` | false |
| Per-value formula | `formula("max(scaled, base)")` | `b.SetFormula("max(scaled, base)")` | `"formula": "max(scaled, base)"` | none |
| Per-value code hook | `transform { base, scaled, screen -> … }` | `b.Transform((baseValue, scaled, screen) => …)` | (code only) | none |
| Rules | `on(…) { … }`, `onScreen(…) { … }`, `landscape { … }`… | `b.On(…)`, `b.OnScreen(…)`, `b.Landscape(…)`… | `"rules": [{"when": {…}, "set": {…}}]` | none |
| Parent | `BuiltInStrategies.POWER.extend("x") { … }` | `BuiltInStrategies.Power.Extend("x", b => { … })` | `"extends": "power"` | none |

## Axes

| Axis | Kotlin | C# | JSON | Screen value | Reference value |
|---|---|---|---|---|---|
| Smallest width | `SMALLEST_WIDTH` | `SmallestWidth` | `smallestWidth` | `sw` | `min(refW, refH)` |
| Width · height | `WIDTH` · `HEIGHT` | `Width` · `Height` | `width` · `height` | window width · height | `refW` · `refH` |
| Shorter · longer side | `MIN` · `MAX` | `Min` · `Max` | `min` · `max` | shorter · longer side | `min` · `max` of the reference |
| Diagonal | `DIAGONAL` | `Diagonal` | `diagonal` | `√(w² + h²)` | `√(refW² + refH²)` |
| Perimeter | `PERIMETER` | `Perimeter` | `perimeter` | `w + h` | `refW + refH` |

The built-in `hdp` divides the height by **300**. A custom `height` axis divides by `reference.height` (533 by default), which is what a designer drawing on a full frame expects.

## Curves

| Curve | Kotlin | C# | JSON | Factor |
|---|---|---|---|---|
| Linear | `linear()` | `ScalingCurve.Linear` | `"linear"` | `r` (= `sdp` with the default reference) |
| Power | `power(0.8f)` | `ScalingCurve.Power(0.8f)` | `{"type": "power", "exponent": 0.8}` | `r^e` (built-in power uses 0.75) |
| Logarithmic | `logarithmic(0.4f)` | `ScalingCurve.Logarithmic(0.4f)` | `{"type": "logarithmic", "k": 0.4}` | `1 + k·ln(r)` |
| Interpolated | `interpolated(0.5f)` | `ScalingCurve.Interpolated(0.5f)` | `{"type": "interpolated", "t": 0.5}` | `1 + (r − 1)·t` |
| Constant | `constant(1f)` | `ScalingCurve.Constant(1f)` | `{"type": "constant", "factor": 1}` | `f` (`constant(1f)` = plain dp) |
| Breakpoints | `breakpoints(360 to 1, 600 to 1.25)` | `ScalingCurve.Breakpoints((360, 1f), (600, 1.25f))` | `{"type": "breakpoints", "points": [{"at": 360, "factor": 1}, …]}` | piecewise linear over the **absolute** axis value (dp), clamped to the first and last points |
| Text expression | `expression("pow(ratio, 0.8)")` | `ScalingCurve.Expression("pow(ratio, 0.8)")` | `{"type": "expression", "expression": "…"}` | your formula ([text formulas](text-formulas.md)) |
| Code | `custom { ratio, screen -> … }` | `ScalingCurve.Custom((ratio, screen) => …)` | (code only) | your formula, pure |

## Bounds

- `clamp(min, max)` limits the **factor**. If rules leave `min > max`, the max wins. Declared configs are validated up front (`minFactor must be <= maxFactor`).
- `clampValue(min, max)` limits the **resulting dp**. The bounds apply to the magnitude, so a negative offset mirrors a positive one and 0 stays 0; they must be ≥ 0. Value bounds make the strategy value-dependent.

## Aspect ratio

`aspectRatio()` / `b.EnableAspectRatio()` multiplies the factor by `1 + k·ln(AR / 1.78)`, where `AR = longSide / shortSide` and the default `k` is the built-in 0.08 / 30. Raise `k` for a stronger correction on very long or very square screens (ultra-wide phones, 4:3 tablets).

## Font scale (`csp`)

| Policy | Effect on `18.csp(Brand)` / `CustomKernels.Csp(18f, brand)` |
|---|---|
| `followFontScale()` (default) | follows the user font scale, like `sp` |
| `ignoreFontScale()` | fixed size; the system font scale is cancelled (typical for a HUD) |
| `clampFontScale(1.3f)` | follows it up to 1.3×, then stops |

Engines draw text themselves, so Kotlin `csp` (px) / `cspDp` (dp) and C# `Csp` (dp) / `CspPx` (px) return the **final** size with the policy applied. Compose `csp` returns a `TextUnit` in sp units, pre-compensated so Compose's own font scaling lands on the policy.

## Multi-window

In Games, a window that is not fullscreen (split screen, freeform, `GameMetrics.isFullscreen == false`, C# `Metrics.Fullscreen == false`) counts as multi-window. `ignoreMultiWindow = true` keeps values unscaled there: the factor is 1, and formulas and transforms are skipped. Value bounds still apply. To react to split screen yourself instead, use a rule (`multiWindow { … }`, C# `b.MultiWindow(r => …)`) or read `multiWindow` in a formula.

The `i` variants (`cdpi`, C# `Cdpi` / `CdpInvariant`) are a different tool: they resolve against the frozen fullscreen reference (`GameScreen.invariantMetrics()`, C# `Screen.Invariant`), so HUD elements stay pinned while the window is resized, and return the base value under true multi-window.

## Rules

A rule changes some fields when its condition matches the window.

```kotlin
scalingStrategy("brand") {
    curve = power(0.8f)
    on(DeviceClass.TABLET, DeviceClass.DESKTOP, name = "large") { curve = logarithmic(0.4f) }
    onScreen(minSmallestWidth = 600f, orientation = Orientation.LANDSCAPE) { multiply(0.9f) }
    on(DeviceClass.WATCH) { axis = ScalingAxis.MIN; reference(192, 192); clamp(0.85f, 1.15f) }
    landscape { multiply(0.95f) }
    multiWindow { clamp(max = 1.2f) }
    largeScreens { clampFontScale(1.2f) }        // sw >= 600, any device class
}
```

```csharp
ScalingStrategy.Define("brand", b =>
{
    b.Curve = ScalingCurve.Power(0.8f);
    b.On(new[] { DeviceClass.Tablet, DeviceClass.Desktop }, r => r.Curve = ScalingCurve.Logarithmic(0.4f), name: "large");
    b.OnScreen(r => r.Multiply(0.9f), minSmallestWidth: 600f, landscape: true);
    b.On(DeviceClass.Watch, r => { r.Axis = ScalingAxis.Min; r.SetReference(192, 192); r.Clamp(0.85f, 1.15f); });
    b.Landscape(r => r.Multiply(0.95f));
    b.MultiWindow(r => r.Clamp(max: 1.2f));
    b.LargeScreens(r => r.ClampFontScale(1.2f));
});
```

| Condition | Kotlin | C# | JSON `when` |
|---|---|---|---|
| Device class | `on(DeviceClass.TV)` | `b.On(DeviceClass.Tv, …)` | `"device": "tv"` or `["tablet", "desktop"]` |
| Orientation | `landscape { }` · `portrait { }` · `onScreen(orientation = …)` | `b.Landscape(…)` · `b.Portrait(…)` · `OnScreen(…, landscape: true)` | `"orientation": "landscape"` |
| Smallest width / width / height (dp) | `onScreen(minSmallestWidth = 600f, maxWidth = 1200f)` | `b.OnScreen(…, minSmallestWidth: 600f, maxWidth: 1200f)` | `"smallestWidth": {"min": 600}`, `"width": {"max": 1200}`, `"height": {…}` |
| Aspect ratio | `onScreen(minAspectRatio = 2f)` | `b.OnScreen(…, minAspectRatio: 2f)` | `"aspectRatio": {"min": 2}` |
| Split screen | `multiWindow { }` | `b.MultiWindow(…)` | `"multiWindow": true` |

- A condition matches when **every** field set in it matches. Ranges are `min <= value < max`.
- All matching rules apply **in declaration order**. A later rule overrides the fields an earlier one set; fields a rule leaves out keep their current value.
- Applied rule names show in `explain()`. Rules without a name show as `#index`.

### Device classes in Games

Device classes: `PHONE`, `TABLET`, `DESKTOP`, `TV`, `WATCH`, `CAR`, `VR` (C#: `Phone`, `Tablet`, `Desktop`, `Tv`, `Watch`, `Car`, `Vr`).

| Runtime | How the device class is chosen |
|---|---|
| Kotlin (`GameMetrics.toScreenSnapshot()`) | `GameMetrics.uiMode`: `TELEVISION` → tv, `CAR` → car, `WATCH` → watch, `DESK` → desktop, `VR_HEADSET` → vr; otherwise `sw ≥ 600` → tablet, else phone (`DeviceClass.fromGameUiMode`) |
| Unity (`Metrics.ToScreenSnapshot()`) | `SystemInfo.deviceType`: Desktop → desktop, Console → tv; otherwise `sw ≥ 600` → tablet, else phone (`CustomGames.DefaultDeviceClass`) |
| Godot / other C# hosts | `sw ≥ 600` → tablet, else phone |

In C#, override the choice with `CustomGames.DeviceClassResolver = m => …` (return `null` to keep the default), for example to force `DeviceClass.Tv` on a console build. Rules keyed on size ranges behave the same everywhere; prefer them when the layout only depends on size.

## Extending

```kotlin
val Soft    = BuiltInStrategies.POWER.extend("soft") { curve = power(0.6f) }
val MyTv    = CustomPresets.TV_SAFE.extend("myTv") { on(DeviceClass.TV) { clamp(max = 1.6f) } }
val Heights = Brand.withAxis(ScalingAxis.HEIGHT)   // "brand@height", cached per strategy
```

```csharp
var soft    = BuiltInStrategies.Power.Extend("soft", b => b.Curve = ScalingCurve.Power(0.6f));
var myTv    = CustomPresets.TvSafe.Extend("myTv", b => b.On(DeviceClass.Tv, r => r.Clamp(max: 1.6f)));
var heights = brand.WithAxis(ScalingAxis.Height);   // "brand@height"
```

`extend` / `Extend` keeps the parent's fields and rules (the parent's rules first) and lets you change any of them.

### Built-ins as configs

Kotlin `BuiltInStrategies.SCALED`, `POWER`, `LOGARITHMIC`, `INTERPOLATED`, `DIAGONAL`, `PERIMETER`, `FIXED` (C#: `Scaled`, `Power`, `Logarithmic`, `Interpolated`, `Diagonal`, `Perimeter`, `Fixed`). Their factors match the built-in satellites on the default smallest-width path, so they are a safe starting point.

### Presets (`CustomPresets`)

| Kotlin | C# | Id | What it does |
|---|---|---|---|
| `TABLET_FRIENDLY` | `TabletFriendly` | `tabletFriendly` | Linear on phones; `power(0.5)` on tablets and desktop; factor ≤ 1.8 |
| `COMPACT_FIRST` | `CompactFirst` | `compactFirst` | Reference 360×640, `interpolated(0.5)`, factor 0.9–1.4 |
| `TV_SAFE` | `TvSafe` | `tvSafe` | On TV: height axis vs 540 dp, factor 1–2 |
| `WEARABLE` | `Wearable` | `wearable` | On watch: shorter side vs 192 dp, factor 0.85–1.15 |
| `GAME_HUD` | `GameHud` | `gameHud` | Shorter side, `power(0.85)`, aspect ratio on, 0.75–2.5, fixed-size text |
| `READABLE_TEXT` | `ReadableText` | `readableText` | `logarithmic(0.4)`, font scale capped at 1.3 |

Presets are not registered by default, but `ScalingStrategies["tvSafe"]` (C#: `ScalingStrategies.Get("tvSafe")`) and JSON `extends` find them by id.

[Back to the custom docs](README.md)
