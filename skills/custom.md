# Custom strategies: agent reference

Use when the user wants their own game-UI scaling (a formula, per-device rules, a shared JSON) instead of a built-in. Docs: `DOCUMENTATION/strategies/custom/` (start at `README.md`). Kotlin module `io.github.bodenberg:appdimens-games-custom` (unreleased); Unity / C#: `csharp/AppDimensGames/AppDimensGamesCustom.cs` + `AppDimensGamesCustom.Engine.cs` (namespace `AppDimensGames.Custom`).

## Decide the form

1. **A built-in passes visual QA?** Keep it. Custom strategies are for when none fits.
2. **The user describes math** ("scale by width over 360, between 0.85 and 1.8"):
   - multiplies every value → Kotlin `CustomScaleStrategy { (width / 360f).coerceIn(0.85f, 1.8f) }` · C# `CustomScaleStrategy.Create(s => s.Clamp(s.Width / 360f, 0.85f, 1.8f))`
   - computes the final value from `base` → Kotlin `scalingFormula { base * pow(sw / 360f, 0.8f) }` · C# `ScalingStrategy.Formula(s => s.Base * s.Pow(s.Sw / 360f, 0.8f))`
3. **The formula must live in JSON / remote config / design tokens** → text: `scalingFormula("base * pow(sw / 360, 0.8)")`, `64.cdp("…")` · C# `ScalingStrategy.Formula("…")`, `64f.Cdp("…")`, JSON `"formula"`.
4. **Different behavior per device / screen range / orientation, bounds, font-scale cap** → `scalingStrategy("id") { … on(DeviceClass.TV) { … } }` · C# `ScalingStrategy.Define("id", b => { … b.On(DeviceClass.Tv, r => …); })`, or the JSON document. Start HUDs from `CustomPresets.GAME_HUD` / `CustomPresets.GameHud`.
5. **Several apps, engines or platforms share it** → the JSON document, loaded once (`ScalingStrategies.loadAsset(context, "appdimens-strategies.json")` on Android, `ScalingStrategies.LoadJson(Resources.Load<TextAsset>("appdimens-strategies").text)` in Unity).

## Cheat sheet

```kotlin
import com.appdimens.games.custom.*           // strategies, GameMetrics bridge
import com.appdimens.games.code.custom.*      // 64.cdp(s): px · cdpDp: dp · cdpi: px invariant · csp: px · cspDp: dp · DimenCustom (Java)
import com.appdimens.games.compose.custom.*   // 64.cdp(s): Dp · cdpi · cdpPx · 18.csp(s): TextUnit · cspPx

val Product = CustomScaleStrategy { (width / 360f).coerceIn(0.85f, 1.8f) }
val Hud = scalingFormula { base * pow(sw / 360f, 0.8f) }
val Brand = scalingStrategy("brand") {
    axis = ScalingAxis.MIN; curve = power(0.85f); aspectRatio(); clamp(0.75f, 2.5f); ignoreFontScale()
    on(DeviceClass.TV) { curve = expression("height / 540") }
}
ScalingStrategies.default = Brand          // 64.cdp() / 18.csp() without argument
ProvideScalingStrategy(Hud) { … }          // Compose subtree
```

```csharp
using AppDimensGames.Custom;
public static readonly ScalingStrategy Hud = ScalingStrategy.Define("hud", b =>
{
    b.Axis = ScalingAxis.Min; b.Curve = ScalingCurve.Power(0.85f); b.EnableAspectRatio(); b.Clamp(0.75f, 2.5f); b.IgnoreFontScale();
    b.On(DeviceClass.Tv, r => r.Curve = ScalingCurve.Expression("height / 540"));
});
float button = 64f.Cdp(Hud);               // dp · CdpPx · Cdpi (invariant) · Csp (final text dp) · CspPx
float same = CustomKernels.Cdp(64f, Hud);  // kernel form; CustomKernels.Explain(Hud) for logs
```

Scope names (code and text; PascalCase in C#): `base` (formula only) `sw width height shortSide longSide diagonal perimeter aspectRatio ratio density fontScale`, flags `phone tablet desktop tv watch car vr landscape portrait multiWindow`, math `pow sqrt ln log10 exp abs round min max clamp lerp smoothstep`. `ratio = sw / 300`, so `base * ratio` = `sdp`.

## Rules an agent must follow

- **Units differ by runtime.** Kotlin `code.custom`: `cdp`, `cdpi`, `csp` return **px**; `cdpDp`, `cspDp` return dp. C#: `Cdp`, `Cdpi`, `Csp` return **dp**; `CdpPx`, `CspPx` return px. Compose returns `Dp` / `TextUnit`.
- **Declare strategies once** (top-level `val`, `object GameStrategies`, `static readonly` field, or JSON). Never build them in `Update()`, `onDrawFrame` or a composable body. Inline text (`64.cdp("…")`, `64f.Cdp("…")`) is shared per text and is fine.
- **Feed the window**: Kotlin code reads `GameScreen` (`GameScreen.updateFromContext(context)`), C# reads `AppDimensGames.Screen` (`Screen.Update(…)`), Compose needs `AppDimensProvider { }`.
- **Lambdas are pure**: no game state, settings or time inside. Different state → different strategy or a rule.
- **Prefer a factor** when the math is `base × screen-term`: it costs one multiply. Value-dependent strategies (per-value formula, `clampValue`) run the formula on every call (no per-value cache in Games). `base * sw / 360` misses the text fast path; write `base * (sw / 360)`.
- **HUD pinned during resize** → `cdpi` / `Cdpi`; **values unscaled in split screen** → `ignoreMultiWindow = true`.
- **Device class**: Kotlin from `GameMetrics.uiMode`; Unity from `SystemInfo.deviceType` (Desktop → desktop, Console → tv), override with `CustomGames.DeviceClassResolver`. Prefer size rules when only size matters.
- **Text vs code**: code lambdas cannot be exported to JSON; use text when the user mentions JSON, remote config, A/B tests or other platforms.
- Watch for **shadowing** in Kotlin: a local `val tv` hides the scope flag `tv` inside the braces.
- **Gameplay / world units are out of scope**: use world-space math (`WorldScale` / `ViewportMode`; C# `World.ViewportRect`).

## Verify

- `Brand.explain()` (Kotlin code), `Brand.explainCurrent()` (Compose), `CustomKernels.Explain(brand)` (C#) print the axis, curve, factor, applied rules and formula.
- Kotlin tests: `./gradlew :library-custom:testDebugUnitTest` (includes the 540-case cross-platform fixture).
- JSON files: `npx appdimens-skills kit custom check <file>` and `kit custom explain --formula "<text>" --devices medium-phone,medium-tablet`.
- Errors carry the JSON path or the formula position; see `DOCUMENTATION/strategies/custom/troubleshooting.md`.
