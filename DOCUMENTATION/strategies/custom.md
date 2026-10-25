# Custom strategies — `cdp` / `csp` (`appdimens-games-custom`)

> **Unreleased** — on `main`, ships with the next release. Same engine and JSON format as every AppDimens platform (`appdimens-dynamic-custom`, KMP, Web, React Native, Flutter, Apple, .NET).

**Kotlin:** `io.github.bodenberg:appdimens-games-custom` (`:library-custom`) · **Unity / C#:** `csharp/AppDimensGames/AppDimensGamesCustom.cs` + `AppDimensGamesCustom.Engine.cs` · See [MODULES.md](../MODULES.md) · [Mathematics §Custom](../MATHEMATICS-AND-CALCULUS.md#custom-strategies-cdp) · [Unity / C# guide §7](../CSHARP-UNITY.md) · **[Full custom-strategy documentation](custom/README.md)**

The built-in strategies cover most games. When a game (or a studio sharing one look across titles) needs its **own** growth law for HUD, menus and text, this module lets you write it. The simplest form is one line of typed code; the full form is a configuration that loads from JSON on every AppDimens platform with the same numbers, in Kotlin and in C#.

```kotlin
import com.appdimens.games.custom.*
import com.appdimens.games.code.custom.*       // px, live GameScreen snapshot

// Factor: the number that multiplies every value
val Product = CustomScaleStrategy { (width / 360f).coerceIn(0.85f, 1.80f) }
// Formula: the final value (base = the 64 in 64.cdp)
val Hud = scalingFormula { base * pow(sw / 360f, 0.8f) }
// Text, for JSON and remote config
val Remote = scalingFormula("base * pow(sw / 360, 0.8)")
// Full configuration with rules
val Brand = scalingStrategy("brand") {
    axis = ScalingAxis.MIN
    curve = power(0.85f)
    clamp(min = 0.75f, max = 2.5f)
    on(DeviceClass.TV) { curve = expression("height / 540") }
}

val buttonPx = 64.cdp(Product)           // px; also cdpDp, cdpi, csp, cspDp
// Compose: Modifier.size(64.cdp(Product)) · fontSize = 18.csp(Hud)  ·  Java: DimenCustom.cdp(64f, product)
```

```csharp
using AppDimensGames.Custom;

var product = CustomScaleStrategy.Create(s => s.Clamp(s.Width / 360f, 0.85f, 1.8f));
var hud = ScalingStrategy.Formula(s => s.Base * s.Pow(s.Sw / 360f, 0.8f));
var brand = ScalingStrategy.Define("brand", b => { b.Axis = ScalingAxis.Min; b.Curve = ScalingCurve.Power(0.85f); b.Clamp(0.75f, 2.5f); });

float button = 64f.Cdp(product);                     // dp (C# short names return dp); CdpPx, Cdpi, Csp, CspPx
float label = CustomKernels.Csp(18f, "base * min(sw / 360, 1.3)");
```

## Documentation

| Guide | What it covers |
|---|---|
| [Overview](custom/README.md) | which form to pick, Kotlin and C# side by side |
| [Quick start](custom/quick-start.md) | install (Gradle and Unity) and first strategy in five minutes |
| [Code formulas](custom/code-formulas.md) | `CustomScaleStrategy`, `scalingFormula` / `ScalingStrategy.Formula` and every name inside the lambda |
| [Text formulas](custom/text-formulas.md) | the formula language: operators, functions, variables, errors, fast path |
| [Configuration](custom/configuration.md) | reference, axes, curves, bounds, aspect ratio, font scale, multi-window, rules, `extend`, built-ins, presets, device classes in Games |
| [JSON format](custom/json.md) | the cross-platform document, loading (assets, `TextAsset`), exporting, validation |
| [API reference](custom/api-reference.md) | Kotlin code / Compose / Java `DimenCustom`, `GameMetrics` bridge, C# `CustomGames` / `CustomKernels` / `float` extensions |
| [Performance](custom/performance.md) | what each form costs in a frame loop |
| [Cookbook](custom/cookbook.md) | recipes: HUD, TV and consoles, split screen, tablets, readable text, A/B tests |
| [Troubleshooting](custom/troubleshooting.md) | error messages, `explain()`, FAQ |

## When to use it

- No built-in strategy passes your visual QA, or you need different laws per device class (TV couch distance, watch, desktop, split screen).
- A design team or remote config owns the numbers (JSON shared with your launcher app, website and backend).
- **Not** for gameplay physics or world units: use world-space math (`WorldScale` / `ViewportMode`; C# `World.ViewportRect`) for those.

## Trade-offs

- Value-dependent strategies (`clampValue`, per-value formulas) run the formula on every call; screen-only strategies cost one multiply.
- Code lambdas cannot be exported to JSON; their text equivalents can.
- Units differ by runtime: Kotlin `code.custom` short names return px, C# short names return dp.
- The aspect-ratio correction always uses the satellite law (`× (1 + k·ln(AR/1.78))`).

[Back to index](../README.md)
