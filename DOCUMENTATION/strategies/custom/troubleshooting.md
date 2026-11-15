# Troubleshooting

## See what a strategy does

```kotlin
Log.d("dimens", Hud.explain())            // code.custom, live GameScreen
Text(Hud.explainCurrent())                // Compose
```

```csharp
Debug.Log(CustomKernels.Explain(hud));    // live Screen.Current
```

```
[brand] tablet smallestWidth=800dp / ref 360 = 2.2222 -> Logarithmic(k=0.4) = 1.3194 x 0.9 clamp[0.85, 1.6] => factor 1.1875 rules=[large, #1]
[hud] phone smallestWidth=411dp / ref 300 = 1.37 -> Linear = 1.37 => factor 1.37 then Formula(base * pow(sw / 360, 0.8))
```

The line reads left to right: device class, axis value and reference, the curve and its result, multiplier, aspect ratio, bounds, the final factor, the applied rules and, when there is one, the per-value formula or transform. The curve label follows each runtime's `toString` (C# prints `Logarithmic(0.4)`). `factor()` / `currentFactor()` / `CustomKernels.Factor(s)` return only the number.

## Common errors

| Message | Kotlin / C# type | Cause | Fix |
|---|---|---|---|
| `Invalid expression at 7: unknown variable 'sww' …` | `IllegalArgumentException` / `ArgumentException` | typo in a text formula | use a name from the list in the message, or switch to a [code formula](code-formulas.md), which the compiler checks |
| `'clamp' takes 3 argument(s), got 2` | same | wrong number of arguments | see the [function table](text-formulas.md#functions) |
| `custom strategy produced a non-finite factor (NaN)` | `IllegalArgumentException` / `InvalidOperationException` | division by zero, `ln` of 0, NaN from a lambda | guard the formula (`max(x, 1)`) or clamp the factor |
| `<path> must be an object` / `must be a boolean` | `IllegalArgumentException` / `ArgumentException` | wrong JSON type | see the [field tables](json.md#strategy-fields) |
| `maxValue must be >= 0` | same | negative value bound | value bounds limit the magnitude; use a positive number |
| `minFactor must be <= maxFactor` | same | crossed bounds in the declaration | swap them (crossed bounds produced by rules at runtime let the max win) |
| `No scaling strategy 'x'. Registered: […]` | `IllegalArgumentException` / `KeyNotFoundException` | id not registered | register it, load the JSON first, or check the id |
| `ValueTransform lambdas cannot be encoded to JSON` | `IllegalStateException` / `InvalidOperationException` | `toJson()` / `ToJson()` on a code lambda | use the text form for strategies you export |

## Frequent questions

**Values do not change when the window resizes.** Kotlin `code.custom` reads `GameScreen`: call `GameScreen.updateFromContext(context)` (or `GameScreen.update(metrics)`) on surface and configuration changes. C# reads `AppDimensGames.Screen`: call `Screen.Update(…)` when the resolution changes. Compose reads `LocalDimenMetrics`, so wrap the UI in `AppDimensProvider { … }`. The `i` variants (`cdpi`, `Cdpi`) stay on the fullscreen reference by design.

**C# values are smaller than the Kotlin ones.** The units differ: C# `Cdp` / `Csp` return dp (use `CdpPx` / `CspPx` for pixels); Kotlin `cdp` / `csp` return px (use `cdpDp` / `cspDp` for dp).

**My lambda result never changes.** The factor is memoized per window. A lambda that reads mutable state (a setting, the score, the time) is not re-run until the window changes. Use separate strategies and pick one, or make the setting part of the screen (a rule).

**`64.cdp(Product)` and `64.sdp` differ.** Expected: the default reference is 300 dp of smallest width; `ratio` is `sw / 300`. `base * ratio` (or `BuiltInStrategies.SCALED` / `Scaled`) gives exactly `sdp`.

**My text formula is slow / misses the fast path.** Only `base * E` qualifies. `base * sw / 360` reads as `(base * sw) / 360`; write `base * (sw / 360)`.

**A strategy created in `Update()` or a composable keeps re-resolving.** `CustomScaleStrategy` and the formula lambdas build a new strategy, with an empty memo, on each call. Declare them once (a `static readonly` field, a top-level `val`, `remember`). Inline text formulas are shared per text and are fine.

**The console build is not detected as TV.** Unity maps only `DeviceType.Console` to tv. Set `CustomGames.DeviceClassResolver` for other cases, or key the rule on size (`onScreen(minWidth = 960f)` / `b.OnScreen(…, minWidth: 960f)`).

**Fonts ignore my strategy.** `csp` follows the user font scale by default. Use `ignoreFontScale()` / `b.IgnoreFontScale()` for fixed HUD text, or `clampFontScale(max)` to cap it.

**Are the numbers the same in Unity and on Android?** Yes for everything that can be written as JSON (curves, rules, text formulas): a shared 540-case fixture checks every platform. Code lambdas run your own code, so they match as long as the code does.

[Back to the custom docs](README.md)
