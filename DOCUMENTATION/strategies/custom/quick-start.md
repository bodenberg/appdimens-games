# Quick start

## 1. Install

**Kotlin (Android games, engines, Compose):**

```kotlin
implementation(platform("io.github.bodenberg:appdimens-games-bom:<version>"))
implementation("io.github.bodenberg:appdimens-games")
implementation("io.github.bodenberg:appdimens-games-custom")   // unreleased, on main
```

```kotlin
import com.appdimens.games.custom.*            // strategies
import com.appdimens.games.code.custom.*       // engines / Kotlin: 64.cdp(…) → px, live GameScreen
import com.appdimens.games.compose.custom.*    // Compose: 64.cdp(…) → Dp, 18.csp(…) → TextUnit
```

The code API reads the live `GameScreen` snapshot, so keep it updated as for the rest of the library (`GameScreen.updateFromContext(context)` in `onSurfaceChanged` / `onConfigurationChanged`, or `GameScreen.update(metrics)`). Compose reads `LocalDimenMetrics` inside `AppDimensProvider { … }`.

**Unity / Godot (C#):** copy `csharp/AppDimensGames/AppDimensGames.cs`, `AppDimensGamesCustom.cs` and `AppDimensGamesCustom.Engine.cs` into the project (Unity 2021.2+ / .NET Standard 2.1). Feed the window as usual with `AppDimensGames.Screen.Update(widthDp, heightDp, densityDpi, fontScale, fullscreen)` (see [CSHARP-UNITY.md](../../CSHARP-UNITY.md)).

```csharp
using AppDimensGames.Custom;
```

## 2. Write the math (recommended)

Declare the strategy **once** (a top-level `val` / `object`, or a `static readonly` field in C#), never inside a frame loop.

```kotlin
// Factor: the number that multiplies every value
val Product = CustomScaleStrategy { (width / 360f).coerceIn(0.85f, 1.80f) }
// Formula: the final value (`base` = the value you pass, the 64 in 64.cdp)
val Hud = scalingFormula { base * pow(sw / 360f, 0.8f) }
```

```csharp
public static class GameStrategies
{
    public static readonly ScalingStrategy Product = CustomScaleStrategy.Create(s => s.Clamp(s.Width / 360f, 0.85f, 1.8f));
    public static readonly ScalingStrategy Hud = ScalingStrategy.Formula(s => s.Base * s.Pow(s.Sw / 360f, 0.8f));
}
```

Inside the lambda the IDE autocompletes `width`, `sw`, `height`, `ratio`, `tablet`, `tv`, `landscape`, `pow`, `clamp`… (C#: `s.Width`, `s.Sw`, `s.Tablet`, `s.Pow`, `s.Clamp`…), and a typo is a compile error. The full list is in [Code formulas](code-formulas.md).

## 3. Use it

```kotlin
// Engines / Kotlin (px, like the core code package)
val buttonPx = 64.cdp(Product)            // px, live window
val buttonDp = 64.cdpDp(Product)          // dp
val pinnedPx = 64.cdpi(Product)           // px, resize-invariant (frozen fullscreen reference)
val labelPx  = 18.csp(Hud)                // px for text (font-scale policy applied)
val labelDp  = 18.cspDp(Hud)              // dp for text

// Compose
Modifier.size(64.cdp(Product))            // Dp
Text("Score", fontSize = 18.csp(Hud))     // TextUnit
```

```java
float px = DimenCustom.cdp(64f, product);   // Java / engines
```

```csharp
float button = 64f.Cdp(GameStrategies.Product);         // dp (C# short names return dp)
float buttonPx = 64f.CdpPx(GameStrategies.Product);     // px
float label = CustomKernels.Csp(18f, GameStrategies.Hud);   // final text size in dp
```

**Units differ:** in Kotlin `code.custom` the short names (`cdp`, `cdpi`, `csp`) return **px** and the `Dp` suffix returns dp; in C# the short names (`Cdp`, `Cdpi`, `Csp`) return **dp** and the `Px` suffix returns px, like the rest of `AppDimensGames`.

## 4. Make it the default (optional)

```kotlin
ScalingStrategies.default = Product                         // 64.cdp() without a strategy now uses Product
ProvideScalingStrategy(Hud) { Text("Lives", fontSize = 14.csp) }   // Compose subtree
```

```csharp
ScalingStrategies.Default = GameStrategies.Product;          // CustomKernels.Cdp(64f) / 64f.Cdp()
```

## 5. The same thing as text (for JSON and remote config)

```kotlin
val Remote = scalingFormula("base * pow(sw / 360, 0.8)")
val px = 64.cdp("base * (sw / 360)")                // inline, shared per text
```

```csharp
var remote = ScalingStrategy.Formula("base * pow(sw / 360, 0.8)");
float dp = 64f.Cdp("base * (sw / 360)");             // inline, shared per text
```

The text form uses the same names (flags are 1 or 0) and works in the strategies JSON (`{"id": "hud", "formula": "base * pow(sw / 360, 0.8)"}`). See [Text formulas](text-formulas.md).

## 6. Need more structure?

```kotlin
val Hud = scalingStrategy("hud") {
    axis = ScalingAxis.MIN
    curve = power(0.85f)
    aspectRatio()
    clamp(min = 0.75f, max = 2.5f)
    ignoreFontScale()
    on(DeviceClass.TV) { curve = expression("height / 540") }
}
```

```csharp
var hud = ScalingStrategy.Define("hud", b =>
{
    b.Axis = ScalingAxis.Min;
    b.Curve = ScalingCurve.Power(0.85f);
    b.EnableAspectRatio();
    b.Clamp(0.75f, 2.5f);
    b.IgnoreFontScale();
    b.On(DeviceClass.Tv, r => r.Curve = ScalingCurve.Expression("height / 540"));
});
```

See [Configuration](configuration.md) and [JSON format](json.md).

## 7. Check the numbers

```kotlin
Log.d("dimens", Product.explain())       // code.custom, live GameScreen
Text(Product.explainCurrent())           // Compose
```

```csharp
Debug.Log(CustomKernels.Explain(GameStrategies.Product));
```

```
[scale#1] phone smallestWidth=411dp / ref 300 = 1.37 -> Custom = 1.1417 => factor 1.1417
```

[Back to the custom docs](README.md)
