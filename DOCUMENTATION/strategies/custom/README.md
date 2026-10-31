# Custom strategies: documentation

**Kotlin module:** `io.github.bodenberg:appdimens-games-custom` (`:library-custom`, unreleased) · packages `com.appdimens.games.custom` (engine + `GameMetrics` bridge), `com.appdimens.games.code.custom` (engines / Kotlin / Java, px, live `GameScreen`), `com.appdimens.games.compose.custom` (Compose)
**Unity / C#:** `csharp/AppDimensGames/AppDimensGamesCustom.cs` + `AppDimensGamesCustom.Engine.cs`, namespace `AppDimensGames.Custom` (`CustomGames`, `CustomKernels`, `float` extensions)

The built-in strategies (`sdp`, `asdp`, `fit`, `fill`…) cover most games. When yours needs its **own** growth law for HUD, menus and text, this module lets you write it, in one line of code or as a full configuration that is shared through JSON with every AppDimens platform (Android, KMP, iOS, Flutter, React Native, Web, Games, .NET). The Kotlin and the C# engines give the same numbers.

## Pick a form

| You want to… | Kotlin | C# (Unity / Godot) | Read |
|---|---|---|---|
| Write the math yourself, with autocomplete | `CustomScaleStrategy { (width / 360f).coerceIn(0.85f, 1.8f) }` (factor) · `scalingFormula { base * pow(sw / 360f, 0.8f) }` (value) | `CustomScaleStrategy.Create(s => s.Clamp(s.Width / 360f, 0.85f, 1.8f))` · `ScalingStrategy.Formula(s => s.Base * s.Pow(s.Sw / 360f, 0.8f))` | [Quick start](quick-start.md) · [Code formulas](code-formulas.md) |
| Keep the formula in JSON or remote config | `scalingFormula("base * pow(sw / 360, 0.8)")` · `64.cdp("…")` | `ScalingStrategy.Formula("…")` · `64f.Cdp("…")` | [Text formulas](text-formulas.md) |
| Different behavior per device, screen range or orientation, bounds, font-scale policy | `scalingStrategy("hud") { … }` | `ScalingStrategy.Define("hud", b => { … })` | [Configuration](configuration.md) |
| Share one definition across apps and platforms | a strategies JSON file | the same file | [JSON format](json.md) |

## Guides

1. [Quick start](quick-start.md): install (Gradle and Unity), the four forms and your first strategy in five minutes.
2. [Code formulas](code-formulas.md): `CustomScaleStrategy` and `scalingFormula` in Kotlin and C#, with every name available inside the lambda.
3. [Text formulas](text-formulas.md): the formula language (operators, functions, variables, errors, fast path).
4. [Configuration](configuration.md): reference frame, axes, curves, bounds, aspect ratio, font scale, multi-window, rules, `extend`, built-ins, presets and how the game window maps to a device class.
5. [JSON format](json.md): the cross-platform document, loading (Android assets, Unity `TextAsset`), exporting and validation errors.
6. [API reference](api-reference.md): every Kotlin (code, Compose, Java) and C# entry point, the registry and the debug helpers.
7. [Performance](performance.md): what each form costs in a frame loop.
8. [Cookbook](cookbook.md): ready-to-use recipes (HUD, TV couch distance, split screen, tablets, readable text, A/B tests).
9. [Troubleshooting](troubleshooting.md): error messages, `explain()`, and frequent questions.

## How it works, in one picture

```
ratio  = axis(window) / axis(reference)                  // e.g. sw / 300
factor = clamp( curve(ratio) × multiplier × AR , min, max )   // once per window
scaled = clampValue( base × factor )   // the configured result
value  = formula ? formula(base, scaled, factor) : scaled   // a formula replaces the result (it gets the original base)
```

`CustomScaleStrategy` replaces the whole `factor` line. `scalingFormula` / `ScalingStrategy.Formula` replaces the whole `value` line. The full configuration lets you set each piece and change it per device with rules. The window is the game snapshot: `GameMetrics` (Kotlin, from `GameScreen`) or `Metrics` (C#, from `AppDimensGames.Screen`). The numbers are the same on every AppDimens platform: a shared 540-case fixture checks it.

[Back to the documentation index](../../README.md) · [Overview page](../custom.md) · [Unity / C# guide](../../CSHARP-UNITY.md)
